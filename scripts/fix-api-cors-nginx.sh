#!/usr/bin/env bash
# Run ON the EC2 box (or via ssh). Fixes CORS preflight for api.pansariwala.shop.
set -euo pipefail

DOMAIN="${1:-pansariwala.shop}"
CONF="/etc/nginx/conf.d/pansariwala.conf"

if [[ ! -f "$CONF" ]]; then
  echo "Missing $CONF — install site config first."
  exit 1
fi

sudo cp "$CONF" "${CONF}.bak.$(date +%s)"

# Ensure api.DOMAIN server block answers OPTIONS with ACAO (idempotent-ish patch via rewrite of api server).
# Safer path: re-apply from repo template if present next to this script after deploy.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMPLATE="$ROOT/scripts/nginx-pansariwala.conf"
if [[ -f "$TEMPLATE" ]]; then
  WEB_ROOT="${EC2_WEB_ROOT:-/var/www/pansariwala}"
  sudo sed -e "s/DOMAIN/${DOMAIN}/g" -e "s|WEB_ROOT|${WEB_ROOT}|g" "$TEMPLATE" | sudo tee "$CONF" >/dev/null
else
  echo "Template $TEMPLATE not found on this host."
  echo "Copy scripts/nginx-pansariwala.conf to the server, then re-run."
  exit 1
fi

sudo nginx -t
sudo systemctl reload nginx

echo "=== live OPTIONS check (expect 204 + Access-Control-Allow-Origin) ==="
curl -sS -D - -o /dev/null -X OPTIONS "https://api.${DOMAIN}/auth/admin/login" \
  -H "Origin: https://${DOMAIN}" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type" \
  --max-time 10 || true

echo "Done."
