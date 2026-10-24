#!/usr/bin/env bash
# Upload webApp JS dist to EC2 and reload nginx.
# Required env: EC2_HOST, EC2_USER, EC2_SSH_KEY_PATH
# Optional: EC2_WEB_ROOT (default /var/www/pansariwala)
set -euo pipefail

: "${EC2_HOST:?EC2_HOST is required}"
: "${EC2_USER:?EC2_USER is required}"
: "${EC2_SSH_KEY_PATH:?EC2_SSH_KEY_PATH is required}"
EC2_WEB_ROOT="${EC2_WEB_ROOT:-/var/www/pansariwala}"

DIST_DIR="${DIST_DIR:-webApp/build/dist/js/productionExecutable}"
KNOWN_HOSTS="${RUNNER_TEMP:-/tmp}/ec2_known_hosts"
SSH_OPTS=(
  -i "$EC2_SSH_KEY_PATH"
  -o StrictHostKeyChecking=accept-new
  -o UserKnownHostsFile="$KNOWN_HOSTS"
  -o IdentitiesOnly=yes
  -o ConnectTimeout=20
)

if [[ ! -d "$DIST_DIR" ]]; then
  echo "Web dist not found: $DIST_DIR" >&2
  exit 1
fi

if [[ ! -f "$DIST_DIR/index.html" ]]; then
  found="$(find "$DIST_DIR" -name index.html -print -quit 2>/dev/null || true)"
  if [[ -n "${found}" ]]; then
    DIST_DIR="$(dirname "$found")"
    echo "Using nested dist: $DIST_DIR"
  else
    echo "index.html not found under $DIST_DIR" >&2
    exit 1
  fi
fi

# Bust browser caches per deploy: webApp.js has no content hash in its filename.
BUILD_STAMP="${GITHUB_SHA:-$(date +%s)}"
BUILD_STAMP="${BUILD_STAMP:0:12}"
sed -i.bak -E "s#src=\"webApp\.js(\?v=[^\"]*)?\"#src=\"webApp.js?v=${BUILD_STAMP}\"#" "$DIST_DIR/index.html"
sed -i.bak -E "s#href=\"styles\.css(\?v=[^\"]*)?\"#href=\"styles.css?v=${BUILD_STAMP}\"#" "$DIST_DIR/index.html"
rm -f "$DIST_DIR/index.html.bak"

echo "Trusting host key for ${EC2_HOST}"
: > "$KNOWN_HOSTS"
ssh-keyscan -T 20 -H "$EC2_HOST" >> "$KNOWN_HOSTS" 2>/dev/null || true
if ! ssh "${SSH_OPTS[@]}" -o BatchMode=yes "${EC2_USER}@${EC2_HOST}" "true"; then
  echo "Cannot SSH to ${EC2_HOST}:22 from GitHub Actions." >&2
  echo "Open EC2 security group inbound TCP 22 from 0.0.0.0/0 (GitHub runner IPs are not your home IP)." >&2
  exit 255
fi

REMOTE_TMP="/tmp/pansariwala-web-$$"
echo "Uploading $DIST_DIR -> ${EC2_USER}@${EC2_HOST}:${REMOTE_TMP} (root ${EC2_WEB_ROOT})"
ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" "rm -rf '${REMOTE_TMP}' && mkdir -p '${REMOTE_TMP}'"
scp -r "${SSH_OPTS[@]}" "${DIST_DIR}/." "${EC2_USER}@${EC2_HOST}:${REMOTE_TMP}/"

ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" bash -s <<EOF
set -euo pipefail
mkdir -p '${EC2_WEB_ROOT}'
if command -v rsync >/dev/null 2>&1; then
  rsync -a --delete '${REMOTE_TMP}/' '${EC2_WEB_ROOT}/'
else
  rm -rf '${EC2_WEB_ROOT:?}'/*
  cp -a '${REMOTE_TMP}/.' '${EC2_WEB_ROOT}/'
fi
rm -rf '${REMOTE_TMP}'
NGINX_BIN="\$(command -v nginx || true)"
if [[ -z "\$NGINX_BIN" && -x /usr/sbin/nginx ]]; then
  NGINX_BIN=/usr/sbin/nginx
fi
if [[ -n "\$NGINX_BIN" ]]; then
  # The app sends X-App-Language on every API call; nginx owns CORS, so its preflight must allow it.
  NGINX_CONF=/etc/nginx/conf.d/pansariwala.conf
  if [[ -f "\$NGINX_CONF" ]] && ! grep -q 'X-App-Language' "\$NGINX_CONF"; then
    sudo cp "\$NGINX_CONF" /tmp/pansariwala.conf.bak
    sudo sed -i 's/Access-Control-Allow-Headers "Authorization, Content-Type, Accept"/Access-Control-Allow-Headers "Authorization, Content-Type, Accept, X-App-Language"/g' "\$NGINX_CONF"
    if ! sudo "\$NGINX_BIN" -t; then
      echo "nginx CORS patch failed validation; restoring previous config" >&2
      sudo cp /tmp/pansariwala.conf.bak "\$NGINX_CONF"
    fi
  fi
  sudo "\$NGINX_BIN" -t
  sudo systemctl reload nginx
else
  echo "nginx not found — files copied to ${EC2_WEB_ROOT}"
fi
EOF

echo "Web deploy complete."
