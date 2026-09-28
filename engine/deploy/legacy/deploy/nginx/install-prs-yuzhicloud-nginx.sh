#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_SRC="$SCRIPT_DIR/prs.yuzhicloud.com.conf"
CONF_DST="/etc/nginx/conf.d/prs.yuzhicloud.com.conf"

if [[ ! -f "$CONF_SRC" ]]; then
  echo "Missing nginx config: $CONF_SRC" >&2
  exit 1
fi

sudo install -m 0644 "$CONF_SRC" "$CONF_DST"
sudo nginx -t
sudo systemctl reload nginx
echo "Installed $CONF_DST"
