#!/usr/bin/env bash
# One-time EC2 bootstrap (run on the instance as a sudo-capable user).
# Usage: sudo bash scripts/bootstrap-ec2.sh [deploy_user]
set -euo pipefail

DEPLOY_USER="${1:-${SUDO_USER:-ubuntu}}"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/pansari}"
SERVICE_NAME="${SERVICE_NAME:-pansari-server}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Deploy user: $DEPLOY_USER"
echo "Deploy path: $DEPLOY_PATH"

if ! command -v java >/dev/null 2>&1; then
  echo "Java not found. Install Temurin/Corretto 17 first, then re-run." >&2
  exit 1
fi

java -version

mkdir -p "$DEPLOY_PATH"
if [[ ! -f "$DEPLOY_PATH/env" ]]; then
  cat > "$DEPLOY_PATH/env" <<'EOF'
PORT=8080
JWT_SECRET=change-me
JWT_ISSUER=pansariwala
MONGODB_PASSWORD=
MONGODB_DB=pansariwala
AUTH_DEV_MODE=true
SMS_API_URL=
SMS_API_TOKEN=
PASSWORD_SALT=
ADMIN_USERNAME=bhargav
ADMIN_PASSWORD=
S3_BUCKET=pansariwala-assets
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
FIREBASE_PROJECT_ID=
EOF
  chmod 600 "$DEPLOY_PATH/env"
  echo "Created $DEPLOY_PATH/env — edit secrets before starting."
fi

chown -R "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH"

UNIT_SRC="$SCRIPT_DIR/pansari-server.service"
UNIT_DST="/etc/systemd/system/${SERVICE_NAME}.service"
sed "s/^User=.*/User=${DEPLOY_USER}/" "$UNIT_SRC" | sed "s|/opt/pansari|${DEPLOY_PATH}|g" > "$UNIT_DST"

systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
echo "Enabled ${SERVICE_NAME}. Place pansari-server-all.jar in ${DEPLOY_PATH} then: systemctl start ${SERVICE_NAME}"

# Allow CI user to restart without password
SUDOERS_FILE="/etc/sudoers.d/${SERVICE_NAME}"
echo "${DEPLOY_USER} ALL=(root) NOPASSWD: /bin/systemctl restart ${SERVICE_NAME}, /bin/systemctl is-active ${SERVICE_NAME}, /bin/systemctl status ${SERVICE_NAME}" > "$SUDOERS_FILE"
chmod 440 "$SUDOERS_FILE"
echo "Wrote $SUDOERS_FILE"
echo "Bootstrap complete."
