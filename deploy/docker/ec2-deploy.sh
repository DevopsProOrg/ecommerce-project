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
NETWORK_NAME="${APP_NAME}-net"
MYSQL_CONTAINER="${APP_NAME}-mysql"
BACKEND_CONTAINER="${APP_NAME}-backend"
FRONTEND_CONTAINER="${APP_NAME}-frontend"
MYSQL_VOLUME="${APP_NAME}-mysql-data"

docker network inspect "$NETWORK_NAME" >/dev/null 2>&1 || docker network create "$NETWORK_NAME"
docker volume inspect "$MYSQL_VOLUME" >/dev/null 2>&1 || docker volume create "$MYSQL_VOLUME"

docker build -t "${APP_NAME}-backend:latest" "$ROOT_DIR/backend"
docker build -t "${APP_NAME}-frontend:latest" "$ROOT_DIR/frontend"

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
  "${APP_NAME}-backend:latest"

docker run -d \
  --name "$FRONTEND_CONTAINER" \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p 80:80 \
  -e BACKEND_UPSTREAM="${BACKEND_UPSTREAM:-http://${BACKEND_CONTAINER}:8888}" \
  "${APP_NAME}-frontend:latest"

echo
echo "Deployment complete."
echo "Frontend: http://<server-public-ip>/"
echo "Backend health: http://<server-public-ip>/api/actuator/health"
