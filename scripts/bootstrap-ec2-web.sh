#!/usr/bin/env bash
# One-time EC2 setup: nginx + TLS + static web root for pansariwala.shop
# Usage: sudo bash scripts/bootstrap-ec2-web.sh [domain] [deploy_user] [web_root]
# Example: sudo bash scripts/bootstrap-ec2-web.sh pansariwala.shop ec2-user /var/www/pansariwala
set -euo pipefail

DOMAIN="${1:-pansariwala.shop}"
DEPLOY_USER="${2:-${SUDO_USER:-ec2-user}}"
WEB_ROOT="${3:-/var/www/pansariwala}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_TEMPLATE="$SCRIPT_DIR/nginx-pansariwala.conf"

echo "Domain: $DOMAIN"
echo "Deploy user: $DEPLOY_USER"
echo "Web root: $WEB_ROOT"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Run as root (sudo)." >&2
  exit 1
fi

# Amazon Linux 2023 / AL2
if command -v dnf >/dev/null 2>&1; then
  dnf install -y nginx certbot python3-certbot-nginx rsync
elif command -v yum >/dev/null 2>&1; then
  yum install -y nginx certbot python3-certbot-nginx rsync
else
  apt-get update && apt-get install -y nginx certbot python3-certbot-nginx rsync
fi

mkdir -p "$WEB_ROOT" /var/www/certbot
chown -R "$DEPLOY_USER:$DEPLOY_USER" "$WEB_ROOT"

sed -e "s/DOMAIN/${DOMAIN}/g" -e "s|WEB_ROOT|${WEB_ROOT}|g" "$NGINX_TEMPLATE" \
  > "/etc/nginx/conf.d/pansariwala.conf"

# Placeholder page until first CI web deploy
cat > "${WEB_ROOT}/index.html" <<EOF
<!DOCTYPE html><html><head><meta charset="utf-8"><title>Pansari Wala</title></head>
<body><h1>Pansari Wala</h1><p>Web deploy pending. Run CI or scripts/deploy-web.sh.</p></body></html>
EOF
chown "$DEPLOY_USER:$DEPLOY_USER" "${WEB_ROOT}/index.html"

install -m 0755 "$SCRIPT_DIR/pansari-publish-web.sh" /usr/local/bin/pansari-publish-web
SUDOERS_FILE="/etc/sudoers.d/pansari-web"
echo "${DEPLOY_USER} ALL=(root) NOPASSWD: /usr/local/bin/pansari-publish-web" > "$SUDOERS_FILE"
chmod 440 "$SUDOERS_FILE"
echo "Wrote $SUDOERS_FILE"

nginx -t
systemctl enable nginx
systemctl start nginx

echo ""
echo "=== Route 53 (do this in AWS console) ==="
echo "  A  ${DOMAIN}      -> EC2 Elastic IP"
echo "  A  www.${DOMAIN}  -> same IP"
echo "  A  api.${DOMAIN}  -> same IP"
echo ""
echo "Security group: allow 80, 443 from 0.0.0.0/0"
echo ""
echo "After DNS propagates, obtain TLS cert:"
echo "  sudo certbot --nginx -d ${DOMAIN} -d www.${DOMAIN} -d api.${DOMAIN}"
echo ""
echo "Set server env API URL to https://api.${DOMAIN} and redeploy mobile apps."
echo "Bootstrap web complete."
