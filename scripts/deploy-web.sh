#!/usr/bin/env bash
# Upload webApp JS dist to EC2 and reload nginx.
# Required env: EC2_HOST, EC2_USER, EC2_SSH_KEY_PATH, EC2_WEB_ROOT
set -euo pipefail

: "${EC2_HOST:?EC2_HOST is required}"
: "${EC2_USER:?EC2_USER is required}"
: "${EC2_SSH_KEY_PATH:?EC2_SSH_KEY_PATH is required}"
: "${EC2_WEB_ROOT:?EC2_WEB_ROOT is required (e.g. /var/www/pansariwala)}"

DIST_DIR="${DIST_DIR:-webApp/build/dist/js/productionExecutable}"
KNOWN_HOSTS="${RUNNER_TEMP:-/tmp}/ec2_known_hosts"
SSH_OPTS=(-i "$EC2_SSH_KEY_PATH" -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$KNOWN_HOSTS" -o IdentitiesOnly=yes)

if [[ ! -d "$DIST_DIR" ]]; then
  echo "Web dist not found: $DIST_DIR" >&2
  exit 1
fi

echo "Trusting host key for ${EC2_HOST}"
ssh-keyscan -H "$EC2_HOST" >> "$KNOWN_HOSTS" 2>/dev/null

REMOTE_TMP="${EC2_WEB_ROOT}.deploy.$$"
echo "Uploading $DIST_DIR -> ${EC2_USER}@${EC2_HOST}:${REMOTE_TMP}"
ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" "rm -rf '${REMOTE_TMP}' && mkdir -p '${REMOTE_TMP}'"
scp -r "${SSH_OPTS[@]}" "${DIST_DIR}/." "${EC2_USER}@${EC2_HOST}:${REMOTE_TMP}/"

ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" bash -s <<EOF
set -euo pipefail
sudo mkdir -p '${EC2_WEB_ROOT}'
sudo rsync -a --delete '${REMOTE_TMP}/' '${EC2_WEB_ROOT}/'
rm -rf '${REMOTE_TMP}'
sudo nginx -t
sudo systemctl reload nginx
EOF

echo "Web deploy complete."
