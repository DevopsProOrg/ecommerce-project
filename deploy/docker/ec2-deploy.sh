#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/deploy/docker/docker.env}"
DEFAULTS_FILE="${DOCKER_DEFAULTS_FILE:-$ROOT_DIR/deploy/docker/docker.env.defaults}"
SECRETS_FILE="${DOCKER_SECRETS_FILE:-$ROOT_DIR/deploy/docker/docker.secrets.env}"
SMOKE_SCRIPT="$ROOT_DIR/deploy/scripts/http-smoke.sh"

if [[ ! -f "$DEFAULTS_FILE" ]]; then
  echo "Missing defaults file: $DEFAULTS_FILE"
  exit 1
fi

set -a
source "$DEFAULTS_FILE"
if [[ -f "$ENV_FILE" ]]; then
  source "$ENV_FILE"
fi
if [[ -f "$SECRETS_FILE" ]]; then
  source "$SECRETS_FILE"
fi
set +a

placeholder_values=(
  ""
  "change-this-mysql-root-password"
  "replace-with-real-mysql-root-password"
  "replace-with-real-datasource-password"
  "replace-with-real-admin-password"
  "replace-with-gmail-app-password"
)

is_placeholder() {
  local value="${1:-}"
  for placeholder in "${placeholder_values[@]}"; do
    if [[ "$value" == "$placeholder" ]]; then
      return 0
    fi
  done
  return 1
}

require_secret() {
  local var_name="$1"
  local value="${!var_name:-}"
  if is_placeholder "$value"; then
    echo "Missing required secret: $var_name"
    echo "Create $SECRETS_FILE from deploy/docker/docker.secrets.env.example and set a real value."
    exit 1
  fi
}

APP_NAME="${APP_NAME:-ogfashions}"
APP_ENV="${APP_ENV:-production}"
NETWORK_NAME="${APP_NAME}-net"
MYSQL_CONTAINER="${APP_NAME}-mysql"
BACKEND_CONTAINER="${APP_NAME}-backend"
FRONTEND_CONTAINER="${APP_NAME}-frontend"
MYSQL_VOLUME="${APP_NAME}-mysql-data"
IMAGE_TAG="${IMAGE_TAG:-v1.0}"
BACKEND_IMAGE="${BACKEND_IMAGE:-${APP_NAME}-backend:${IMAGE_TAG}}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-${APP_NAME}-frontend:${IMAGE_TAG}}"
BUILD_IMAGES_LOCALLY="${BUILD_IMAGES_LOCALLY:-true}"
USE_EXTERNAL_DB="${USE_EXTERNAL_DB:-false}"
SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-}"
APP_PUBLIC_BASE_URL="${APP_PUBLIC_BASE_URL:-}"
APP_CORS_ALLOWED_ORIGINS="${APP_CORS_ALLOWED_ORIGINS:-${APP_PUBLIC_BASE_URL}}"
APP_CORS_ALLOWED_ORIGIN_PATTERNS="${APP_CORS_ALLOWED_ORIGIN_PATTERNS:-}"
BACKEND_HOST_PORT="${BACKEND_HOST_PORT:-8888}"
FRONTEND_HOST_PORT="${FRONTEND_HOST_PORT:-80}"
PORT_CONFLICT_STRATEGY="${PORT_CONFLICT_STRATEGY:-fail}"
PORT_SEARCH_LIMIT="${PORT_SEARCH_LIMIT:-25}"
DO_POST_DEPLOY_SMOKE="${DO_POST_DEPLOY_SMOKE:-true}"

port_in_use() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltn "( sport = :${port} )" 2>/dev/null | tail -n +2 | grep -q .
  elif command -v lsof >/dev/null 2>&1; then
    lsof -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
  elif command -v netstat >/dev/null 2>&1; then
    netstat -an 2>/dev/null | grep -E "[:.]${port}[[:space:]].*(LISTEN|LISTENING)" >/dev/null 2>&1
  else
    return 1
  fi
}

resolve_host_port() {
  local name="$1"
  local preferred_port="$2"
  local strategy="$3"
  local max_tries="$4"
  local candidate="$preferred_port"
  local tries=0

  while port_in_use "$candidate"; do
    if [[ "$strategy" != "auto" ]]; then
      echo "Port conflict detected for ${name}: ${candidate} is already in use."
      echo "Set a different port in your env file or set PORT_CONFLICT_STRATEGY=auto for local development."
      exit 1
    fi

    tries=$((tries + 1))
    if (( tries > max_tries )); then
      echo "Unable to find a free port for ${name} after ${max_tries} attempts starting from ${preferred_port}."
      exit 1
    fi
    candidate=$((candidate + 1))
  done

  echo "$candidate"
}

require_secret SPRING_DATASOURCE_PASSWORD
require_secret APP_BOOTSTRAP_ADMIN_PASSWORD
require_secret SPRING_MAIL_PASSWORD
if [[ "$USE_EXTERNAL_DB" != "true" ]]; then
  require_secret MYSQL_ROOT_PASSWORD
fi

if [[ -n "$APP_PUBLIC_BASE_URL" ]]; then
  APP_CORS_ALLOWED_ORIGINS="${APP_CORS_ALLOWED_ORIGINS#,}"
fi

docker network inspect "$NETWORK_NAME" >/dev/null 2>&1 || docker network create "$NETWORK_NAME"
if [[ "$USE_EXTERNAL_DB" != "true" ]]; then
  docker volume inspect "$MYSQL_VOLUME" >/dev/null 2>&1 || docker volume create "$MYSQL_VOLUME"
fi

if [[ "$BUILD_IMAGES_LOCALLY" == "true" ]]; then
  docker build -t "$BACKEND_IMAGE" "$ROOT_DIR/backend"
  docker build -t "$FRONTEND_IMAGE" "$ROOT_DIR/frontend"
else
  docker pull "$BACKEND_IMAGE"
  docker pull "$FRONTEND_IMAGE"
fi

docker rm -f "$MYSQL_CONTAINER" "$BACKEND_CONTAINER" "$FRONTEND_CONTAINER" >/dev/null 2>&1 || true

BACKEND_HOST_PORT="$(resolve_host_port backend "$BACKEND_HOST_PORT" "$PORT_CONFLICT_STRATEGY" "$PORT_SEARCH_LIMIT")"
FRONTEND_HOST_PORT="$(resolve_host_port frontend "$FRONTEND_HOST_PORT" "$PORT_CONFLICT_STRATEGY" "$PORT_SEARCH_LIMIT")"

if [[ "$APP_ENV" == "local" && ( -z "$APP_PUBLIC_BASE_URL" || "$APP_PUBLIC_BASE_URL" == http://localhost:* || "$APP_PUBLIC_BASE_URL" == http://127.0.0.1:* ) ]]; then
  APP_PUBLIC_BASE_URL="http://localhost:${FRONTEND_HOST_PORT}"
fi

if [[ "$USE_EXTERNAL_DB" == "true" ]]; then
  if is_placeholder "$SPRING_DATASOURCE_URL"; then
    echo "USE_EXTERNAL_DB=true requires a real SPRING_DATASOURCE_URL value."
    exit 1
  fi
  EFFECTIVE_DATASOURCE_URL="$SPRING_DATASOURCE_URL"
  echo "Using external database connection for backend deployment."
else
  EFFECTIVE_DATASOURCE_URL="jdbc:mysql://${MYSQL_CONTAINER}:3306/${MYSQL_DATABASE}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

  docker run -d \
    --name "$MYSQL_CONTAINER" \
    --network "$NETWORK_NAME" \
    --restart unless-stopped \
    -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
    -e MYSQL_DATABASE="$MYSQL_DATABASE" \
    -v "${MYSQL_VOLUME}:/var/lib/mysql" \
    mysql:8.4

  echo "Waiting for MySQL to accept connections..."
  until docker exec "$MYSQL_CONTAINER" mysqladmin ping -h 127.0.0.1 -p"$MYSQL_ROOT_PASSWORD" --silent >/dev/null 2>&1; do
    sleep 4
  done
fi

docker run -d \
  --name "$BACKEND_CONTAINER" \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p "${BACKEND_HOST_PORT}:8888" \
  -e SERVER_PORT=8888 \
  -e SPRING_DATASOURCE_URL="$EFFECTIVE_DATASOURCE_URL" \
  -e SPRING_DATASOURCE_USERNAME="$SPRING_DATASOURCE_USERNAME" \
  -e SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
  -e SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE="${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:-10}" \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-update}" \
  -e APP_CORS_ALLOWED_ORIGINS="$APP_CORS_ALLOWED_ORIGINS" \
  -e APP_CORS_ALLOWED_ORIGIN_PATTERNS="$APP_CORS_ALLOWED_ORIGIN_PATTERNS" \
  -e APP_BOOTSTRAP_ADMIN_ENABLED="$APP_BOOTSTRAP_ADMIN_ENABLED" \
  -e APP_BOOTSTRAP_ADMIN_NAME="$APP_BOOTSTRAP_ADMIN_NAME" \
  -e APP_BOOTSTRAP_ADMIN_EMAIL="$APP_BOOTSTRAP_ADMIN_EMAIL" \
  -e APP_BOOTSTRAP_ADMIN_PASSWORD="$APP_BOOTSTRAP_ADMIN_PASSWORD" \
  -e APP_MAIL_ENABLED="$APP_MAIL_ENABLED" \
  -e APP_MAIL_FROM="$APP_MAIL_FROM" \
  -e SPRING_MAIL_HOST="$SPRING_MAIL_HOST" \
  -e SPRING_MAIL_PORT="$SPRING_MAIL_PORT" \
  -e SPRING_MAIL_USERNAME="$SPRING_MAIL_USERNAME" \
  -e SPRING_MAIL_PASSWORD="$SPRING_MAIL_PASSWORD" \
  -e SPRING_MAIL_SMTP_AUTH="$SPRING_MAIL_SMTP_AUTH" \
  -e SPRING_MAIL_SMTP_STARTTLS_ENABLE="$SPRING_MAIL_SMTP_STARTTLS_ENABLE" \
  "$BACKEND_IMAGE"

docker run -d \
  --name "$FRONTEND_CONTAINER" \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p "${FRONTEND_HOST_PORT}:80" \
  -e BACKEND_UPSTREAM="${BACKEND_UPSTREAM:-http://${BACKEND_CONTAINER}:8888}" \
  "$FRONTEND_IMAGE"

echo
echo "Deployment complete for environment: $APP_ENV"
echo "Resolved backend host port: ${BACKEND_HOST_PORT}"
echo "Resolved frontend host port: ${FRONTEND_HOST_PORT}"
echo "External database mode: ${USE_EXTERNAL_DB}"
if [[ -n "$APP_PUBLIC_BASE_URL" ]]; then
  echo "Frontend: ${APP_PUBLIC_BASE_URL}/"
  echo "Frontend health: ${APP_PUBLIC_BASE_URL}/healthz"
  echo "Backend health: ${APP_PUBLIC_BASE_URL}/api/actuator/health"
  if [[ "$DO_POST_DEPLOY_SMOKE" == "true" && -f "$SMOKE_SCRIPT" ]]; then
    bash "$SMOKE_SCRIPT" "$APP_PUBLIC_BASE_URL"
  fi
else
  echo "Frontend: http://<server-public-ip>:${FRONTEND_HOST_PORT}/"
  echo "Frontend health: http://<server-public-ip>:${FRONTEND_HOST_PORT}/healthz"
  echo "Backend health: http://<server-public-ip>:${FRONTEND_HOST_PORT}/api/actuator/health"
fi
