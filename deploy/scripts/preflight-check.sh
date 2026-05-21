#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -z "$MODE" ]]; then
  echo "Usage: bash deploy/scripts/preflight-check.sh <local|docker|k8s|terraform|ansible|jenkins>"
  exit 1
fi

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

require_command() {
  local cmd="$1"
  if ! command_exists "$cmd"; then
    echo "Missing required command: $cmd"
    exit 1
  fi
}

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Missing required file: $path"
    exit 1
  fi
}

case "$MODE" in
  local)
    require_command docker
    require_file "$ROOT_DIR/deploy/local/local-docker.env"
    require_file "$ROOT_DIR/deploy/local/local-docker.secrets.env"
    ;;
  docker)
    require_command docker
    require_file "$ROOT_DIR/deploy/docker/docker.env.defaults"
    require_file "$ROOT_DIR/deploy/docker/docker.secrets.env"
    ;;
  k8s)
    require_command kubectl
    require_file "$ROOT_DIR/deploy/k8s/scripts/deploy.sh"
    ;;
  terraform)
    require_command terraform
    ;;
  ansible)
    require_command ansible
    require_command ansible-playbook
    ;;
  jenkins)
    require_command git
    require_command docker
    require_command mvn
    require_command node
    ;;
  *)
    echo "Unknown preflight mode: $MODE"
    exit 1
    ;;
esac

echo "Preflight checks passed for mode: $MODE"
