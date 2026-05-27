#!/usr/bin/env bash
set -euo pipefail

OVERLAY="${1:-}"
IMAGE_REPO="${2:-}"
IMAGE_TAG="${3:-}"
BASE_URL="${4:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SMOKE_SCRIPT="$ROOT_DIR/../scripts/http-smoke.sh"

if [[ -z "$OVERLAY" ]]; then
  echo "Usage:"
  echo "  bash deploy/k8s/scripts/rollback.sh <l1|l2|l3|l6|lp>"
  echo "  bash deploy/k8s/scripts/rollback.sh <l1|l2|l3|l6|lp> <image-repo> <image-tag> [base-url]"
  exit 1
fi

NAMESPACE="ogfashions-$OVERLAY"

if [[ -n "$IMAGE_REPO" && -n "$IMAGE_TAG" ]]; then
  kubectl -n "$NAMESPACE" set image deployment/ogfashions-backend backend="docker.io/${IMAGE_REPO}/ogfashions-backend:${IMAGE_TAG}"
  kubectl -n "$NAMESPACE" set image deployment/ogfashions-frontend frontend="docker.io/${IMAGE_REPO}/ogfashions-frontend:${IMAGE_TAG}"
else
  kubectl -n "$NAMESPACE" rollout undo deployment/ogfashions-backend
  kubectl -n "$NAMESPACE" rollout undo deployment/ogfashions-frontend
fi

kubectl -n "$NAMESPACE" rollout status deployment/ogfashions-backend --timeout=180s
kubectl -n "$NAMESPACE" rollout status deployment/ogfashions-frontend --timeout=180s

if [[ -n "$BASE_URL" && -f "$SMOKE_SCRIPT" ]]; then
  bash "$SMOKE_SCRIPT" "$BASE_URL"
fi
