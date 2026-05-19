#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK_NAME="${MONITORING_DOCKER_NETWORK:-ogfashions-net}"

docker network inspect "$NETWORK_NAME" >/dev/null 2>&1 || docker network create "$NETWORK_NAME"

docker rm -f ogfashions-prometheus ogfashions-grafana >/dev/null 2>&1 || true

docker run -d \
  --name ogfashions-prometheus \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p 9090:9090 \
  -v "$ROOT_DIR/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  prom/prometheus:v2.53.0

docker run -d \
  --name ogfashions-grafana \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p 3000:3000 \
  -v "$ROOT_DIR/grafana/provisioning:/etc/grafana/provisioning:ro" \
  -v "$ROOT_DIR/grafana/dashboards:/var/lib/grafana/dashboards:ro" \
  grafana/grafana:11.1.0

echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000"
