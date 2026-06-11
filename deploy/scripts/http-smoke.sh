#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-}"
SMOKE_RETRY_COUNT="${SMOKE_RETRY_COUNT:-20}"
SMOKE_RETRY_DELAY_SECONDS="${SMOKE_RETRY_DELAY_SECONDS:-3}"

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
  local attempt=1
  while (( attempt <= SMOKE_RETRY_COUNT )); do
    if curl -fsS "$url" >/dev/null; then
      return 0
    fi
    if (( attempt == SMOKE_RETRY_COUNT )); then
      break
    fi
    sleep "$SMOKE_RETRY_DELAY_SECONDS"
    attempt=$((attempt + 1))
  done
  echo "Smoke check failed for ${url} after ${SMOKE_RETRY_COUNT} attempts."
  return 1
}

check_url "frontend home" "${BASE_URL}/"
check_url "frontend health" "${BASE_URL}/healthz"
check_url "backend health" "${BASE_URL}/api/actuator/health"

echo "Smoke checks passed for ${BASE_URL}"
