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
check_backend_health() {
  local url="${BASE_URL}/api/actuator/health"
  echo "Checking backend health: ${url}"
  local attempt=1
  local response
  while (( attempt <= SMOKE_RETRY_COUNT )); do
    if response="$(curl -fsS "$url")" && grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<<"$response"; then
      return 0
    fi
    if (( attempt == SMOKE_RETRY_COUNT )); then
      break
    fi
    sleep "$SMOKE_RETRY_DELAY_SECONDS"
    attempt=$((attempt + 1))
  done
  echo "Backend health check failed for ${url}; expected HTTP 200 with status UP."
  return 1
}

check_backend_health

check_catalog_api() {
  local url="${BASE_URL}/api/products"
  echo "Checking catalog API: ${url}"
  local attempt=1
  local response
  while (( attempt <= SMOKE_RETRY_COUNT )); do
    if response="$(curl -fsS "$url")" && grep -Eq '^[[:space:]]*\[' <<<"$response" && grep -Eq '"id"[[:space:]]*:' <<<"$response"; then
      return 0
    fi
    if (( attempt == SMOKE_RETRY_COUNT )); then
      break
    fi
    sleep "$SMOKE_RETRY_DELAY_SECONDS"
    attempt=$((attempt + 1))
  done
  echo "Catalog API check failed for ${url}; expected a JSON product list."
  return 1
}

check_catalog_api

echo "Smoke checks passed for ${BASE_URL}"
