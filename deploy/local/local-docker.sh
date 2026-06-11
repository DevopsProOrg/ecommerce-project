#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOCAL_ENV_FILE="${1:-$ROOT_DIR/deploy/local/local-docker.env}"
LOCAL_SECRETS_FILE="${LOCAL_DOCKER_SECRETS_FILE:-$ROOT_DIR/deploy/local/local-docker.secrets.env}"

if [[ ! -f "$LOCAL_ENV_FILE" ]]; then
  echo "Missing local env file: $LOCAL_ENV_FILE"
  echo "Copy deploy/local/local-docker.env.example to deploy/local/local-docker.env and update the values if needed."
  exit 1
fi

if [[ ! -f "$LOCAL_SECRETS_FILE" ]]; then
  echo "Missing local secrets file: $LOCAL_SECRETS_FILE"
  echo "Copy deploy/local/local-docker.secrets.env.example to deploy/local/local-docker.secrets.env and set real local values."
  exit 1
fi

DOCKER_SECRETS_FILE="$LOCAL_SECRETS_FILE" bash "$ROOT_DIR/deploy/docker/ec2-deploy.sh" "$LOCAL_ENV_FILE"
