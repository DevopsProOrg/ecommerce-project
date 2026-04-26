#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${1:-ogfashions}"
docker rm -f "${APP_NAME}-frontend" "${APP_NAME}-backend" "${APP_NAME}-mysql" >/dev/null 2>&1 || true
docker network rm "${APP_NAME}-net" >/dev/null 2>&1 || true
echo "Removed containers and network for ${APP_NAME}. The MySQL volume was kept."
