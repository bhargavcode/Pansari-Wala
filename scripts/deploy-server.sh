#!/usr/bin/env bash
# Upload pansari-server fat JAR to EC2 and restart the systemd service.
# Required env: EC2_HOST, EC2_USER, EC2_SSH_KEY_PATH, EC2_DEPLOY_PATH
# Optional: EC2_SERVICE_NAME (default: pansari-server), JAR_PATH
set -euo pipefail

: "${EC2_HOST:?EC2_HOST is required}"
: "${EC2_USER:?EC2_USER is required}"
: "${EC2_SSH_KEY_PATH:?EC2_SSH_KEY_PATH is required}"
: "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH is required}"

EC2_SERVICE_NAME="${EC2_SERVICE_NAME:-pansari-server}"
JAR_PATH="${JAR_PATH:-server/build/libs/pansari-server-all.jar}"
REMOTE_JAR="${EC2_DEPLOY_PATH%/}/pansari-server-all.jar"
KNOWN_HOSTS="${RUNNER_TEMP:-/tmp}/ec2_known_hosts"
SSH_OPTS=(
  -i "$EC2_SSH_KEY_PATH"
  -o StrictHostKeyChecking=accept-new
  -o UserKnownHostsFile="$KNOWN_HOSTS"
  -o IdentitiesOnly=yes
  -o ConnectTimeout=20
)

if [[ ! -f "$JAR_PATH" ]]; then
  echo "JAR not found: $JAR_PATH" >&2
  exit 1
fi

echo "Trusting host key for ${EC2_HOST}"
: > "$KNOWN_HOSTS"
ssh-keyscan -T 20 -H "$EC2_HOST" >> "$KNOWN_HOSTS" 2>/dev/null || true
if ! ssh "${SSH_OPTS[@]}" -o BatchMode=yes "${EC2_USER}@${EC2_HOST}" "true"; then
  echo "Cannot SSH to ${EC2_HOST}:22 from GitHub Actions." >&2
  echo "Open EC2 security group inbound TCP 22 from 0.0.0.0/0 (GitHub runner IPs are not your home IP)." >&2
  exit 255
fi

echo "Uploading $JAR_PATH -> ${EC2_USER}@${EC2_HOST}:${REMOTE_JAR}"
scp "${SSH_OPTS[@]}" "$JAR_PATH" "${EC2_USER}@${EC2_HOST}:${REMOTE_JAR}.tmp"
ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" \
  "mv '${REMOTE_JAR}.tmp' '${REMOTE_JAR}' && sudo systemctl restart '${EC2_SERVICE_NAME}' && sudo systemctl is-active '${EC2_SERVICE_NAME}'"
echo "Deploy complete."
