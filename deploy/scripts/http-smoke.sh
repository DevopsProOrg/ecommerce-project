#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-}"

if [[ -z "$BASE_URL" ]]; then
  echo "Usage: bash deploy/scripts/http-smoke.sh <base-url>"
  echo "Example: bash deploy/scripts/http-smoke.sh https://ogstyles.store"
  exit 1
fi

BASE_URL="${BASE_URL%/}"

check_url() {
  local name="$1"
  local url="$2"
  echo "Checking ${name}: ${url}"
  curl -fsS "$url" >/dev/null
}

check_url "frontend home" "${BASE_URL}/"
check_url "frontend health" "${BASE_URL}/healthz"
check_url "backend health" "${BASE_URL}/api/actuator/health"

echo "Smoke checks passed for ${BASE_URL}"
