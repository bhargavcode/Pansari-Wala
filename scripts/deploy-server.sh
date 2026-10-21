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

# Upsert app secrets passed from CI into the systemd EnvironmentFile (values never hit the command line or logs).
SYNC_ENV_KEYS=(CASHFREE_APP_ID CASHFREE_SECRET_KEY CASHFREE_ENV PUBLIC_API_BASE_URL)
REMOTE_ENV="${EC2_DEPLOY_PATH%/}/env"
ENV_UPDATES="$(mktemp)"
trap 'rm -f "$ENV_UPDATES"' EXIT
for key in "${SYNC_ENV_KEYS[@]}"; do
  value="${!key:-}"
  [[ -n "$value" ]] && printf '%s=%s\n' "$key" "$value" >> "$ENV_UPDATES"
done
if [[ -s "$ENV_UPDATES" ]]; then
  echo "Updating $(wc -l < "$ENV_UPDATES" | xargs) env keys in ${REMOTE_ENV}"
  ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" "
    set -e
    touch '${REMOTE_ENV}' && chmod 600 '${REMOTE_ENV}'
    tmp=\$(mktemp)
    cat > \"\$tmp.new\"
    cut -d= -f1 \"\$tmp.new\" | sed 's/^/^/; s/\$/=/' > \"\$tmp.keys\"
    { grep -v -f \"\$tmp.keys\" '${REMOTE_ENV}' || true; cat \"\$tmp.new\"; } > \"\$tmp\"
    cat \"\$tmp\" > '${REMOTE_ENV}'
    rm -f \"\$tmp\" \"\$tmp.new\" \"\$tmp.keys\"
  " < "$ENV_UPDATES"
fi

echo "Uploading $JAR_PATH -> ${EC2_USER}@${EC2_HOST}:${REMOTE_JAR}"
scp "${SSH_OPTS[@]}" "$JAR_PATH" "${EC2_USER}@${EC2_HOST}:${REMOTE_JAR}.tmp"
ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" \
  "mv '${REMOTE_JAR}.tmp' '${REMOTE_JAR}' && sudo systemctl restart '${EC2_SERVICE_NAME}'"

# systemd reports "active" as soon as the JVM starts; wait for the API to answer before declaring success.
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${SERVER_PORT:-8080}/health}"
HEALTH_WAIT_SEC="${HEALTH_WAIT_SEC:-90}"
if ! ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" "
  for i in \$(seq 1 ${HEALTH_WAIT_SEC}); do
    curl -fsS -o /dev/null --max-time 3 '${HEALTH_URL}' && exit 0
    sleep 1
  done
  exit 1"; then
  echo "::error::${EC2_SERVICE_NAME} did not become healthy at ${HEALTH_URL} within ${HEALTH_WAIT_SEC}s" >&2
  ssh "${SSH_OPTS[@]}" "${EC2_USER}@${EC2_HOST}" \
    "sudo systemctl status '${EC2_SERVICE_NAME}' --no-pager -l | head -20; sudo journalctl -u '${EC2_SERVICE_NAME}' -n 120 --no-pager; free -m; df -h /" >&2 || true
  exit 1
fi
echo "Deploy complete (healthy)."
