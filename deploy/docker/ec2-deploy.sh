#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/deploy/docker/docker.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE"
  echo "Copy deploy/docker/docker.env.example to deploy/docker/docker.env and update the values."
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

APP_NAME="${APP_NAME:-ogfashions}"
APP_ENV="${APP_ENV:-production}"
NETWORK_NAME="${APP_NAME}-net"
MYSQL_CONTAINER="${APP_NAME}-mysql"
BACKEND_CONTAINER="${APP_NAME}-backend"
FRONTEND_CONTAINER="${APP_NAME}-frontend"
MYSQL_VOLUME="${APP_NAME}-mysql-data"
IMAGE_TAG="${IMAGE_TAG:-latest}"
BACKEND_IMAGE="${BACKEND_IMAGE:-${APP_NAME}-backend:${IMAGE_TAG}}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-${APP_NAME}-frontend:${IMAGE_TAG}}"
BUILD_IMAGES_LOCALLY="${BUILD_IMAGES_LOCALLY:-true}"
APP_PUBLIC_BASE_URL="${APP_PUBLIC_BASE_URL:-}"
APP_CORS_ALLOWED_ORIGINS="${APP_CORS_ALLOWED_ORIGINS:-${APP_PUBLIC_BASE_URL}}"
APP_CORS_ALLOWED_ORIGIN_PATTERNS="${APP_CORS_ALLOWED_ORIGIN_PATTERNS:-}"
if [[ -n "${APP_PUBLIC_BASE_URL}" ]]; then
  APP_CORS_ALLOWED_ORIGINS="${APP_CORS_ALLOWED_ORIGINS#,}"
fi

docker network inspect "$NETWORK_NAME" >/dev/null 2>&1 || docker network create "$NETWORK_NAME"
docker volume inspect "$MYSQL_VOLUME" >/dev/null 2>&1 || docker volume create "$MYSQL_VOLUME"

if [[ "$BUILD_IMAGES_LOCALLY" == "true" ]]; then
  docker build -t "$BACKEND_IMAGE" "$ROOT_DIR/backend"
  docker build -t "$FRONTEND_IMAGE" "$ROOT_DIR/frontend"
else
  docker pull "$BACKEND_IMAGE"
  docker pull "$FRONTEND_IMAGE"
fi

docker rm -f "$MYSQL_CONTAINER" "$BACKEND_CONTAINER" "$FRONTEND_CONTAINER" >/dev/null 2>&1 || true

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

docker run -d \
  --name "$BACKEND_CONTAINER" \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p 8888:8888 \
  -e SERVER_PORT=8888 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://${MYSQL_CONTAINER}:3306/${MYSQL_DATABASE}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
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
  -p 80:80 \
  -e BACKEND_UPSTREAM="${BACKEND_UPSTREAM:-http://${BACKEND_CONTAINER}:8888}" \
  "$FRONTEND_IMAGE"

echo
echo "Deployment complete for environment: $APP_ENV"
if [[ -n "$APP_PUBLIC_BASE_URL" ]]; then
  echo "Frontend: ${APP_PUBLIC_BASE_URL}/"
  echo "Backend health: ${APP_PUBLIC_BASE_URL}/api/actuator/health"
else
  echo "Frontend: http://<server-public-ip>/"
  echo "Backend health: http://<server-public-ip>/api/actuator/health"
fi
