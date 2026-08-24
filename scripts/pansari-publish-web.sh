#!/usr/bin/env bash
# Root-only: copy staged web files into nginx root and reload.
# Usage: pansari-publish-web <src_dir> [web_root]
set -euo pipefail
SRC="${1:?src dir required}"
ROOT="${2:-/var/www/pansariwala}"
if [[ ! -d "$SRC" ]]; then
  echo "Source missing: $SRC" >&2
  exit 1
fi
mkdir -p "$ROOT"
rsync -a --delete "${SRC}/" "${ROOT}/"
nginx -t
systemctl reload nginx
echo "Published $SRC -> $ROOT"
