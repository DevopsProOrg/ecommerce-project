#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/deploy/docker/docker.env}"

export BUILD_IMAGES_LOCALLY=false
bash "$ROOT_DIR/deploy/docker/ec2-deploy.sh" "$ENV_FILE"
