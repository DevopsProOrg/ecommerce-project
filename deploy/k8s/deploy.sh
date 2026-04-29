#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OVERLAY="${1:-staging}"
BACKEND_IMAGE="${2:-}"
FRONTEND_IMAGE="${3:-}"

if [[ -z "${BACKEND_IMAGE}" || -z "${FRONTEND_IMAGE}" ]]; then
  echo "Usage: ./deploy/k8s/deploy.sh <staging|production> <backend-image> <frontend-image>"
  exit 1
fi

OVERLAY_DIR="$ROOT_DIR/deploy/k8s/overlays/$OVERLAY"
if [[ ! -d "$OVERLAY_DIR" ]]; then
  echo "Missing overlay directory: $OVERLAY_DIR"
  exit 1
fi

kubectl apply -f "$ROOT_DIR/deploy/k8s/namespace.yaml"
kubectl apply -f "$ROOT_DIR/deploy/k8s/secret.yaml"

kubectl kustomize "$OVERLAY_DIR" \
  | sed "s|your-dockerhub-or-ecr-user/ogfashions-backend:latest|$BACKEND_IMAGE|g" \
  | sed "s|your-dockerhub-or-ecr-user/ogfashions-frontend:latest|$FRONTEND_IMAGE|g" \
  | kubectl apply -f -

echo "Kubernetes deployment complete for overlay: $OVERLAY"
