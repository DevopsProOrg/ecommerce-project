#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/deploy/docker/docker.env}"
ROLLBACK_IMAGE_TAG="${2:-${ROLLBACK_IMAGE_TAG:-}}"

if [[ -z "$ROLLBACK_IMAGE_TAG" ]]; then
  echo "Usage: bash deploy/docker/ec2-rollback.sh [env-file] <image-tag>"
  echo "Example: bash deploy/docker/ec2-rollback.sh deploy/docker/docker.env release-42"
  exit 1
fi

export IMAGE_TAG="$ROLLBACK_IMAGE_TAG"
export BUILD_IMAGES_LOCALLY=false

"$ROOT_DIR/deploy/docker/ec2-deploy.sh" "$ENV_FILE"
