#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK_NAME="${MONITORING_DOCKER_NETWORK:-ogfashions-net}"
PROMETHEUS_HOST_PORT="${PROMETHEUS_HOST_PORT:-9090}"
GRAFANA_HOST_PORT="${GRAFANA_HOST_PORT:-3000}"
PORT_CONFLICT_STRATEGY="${PORT_CONFLICT_STRATEGY:-auto}"
PORT_SEARCH_LIMIT="${PORT_SEARCH_LIMIT:-25}"

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
      echo "Set a different port or use PORT_CONFLICT_STRATEGY=auto."
      exit 1
    fi

    tries=$((tries + 1))
    if (( tries > max_tries )); then
      echo "Unable to find a free port for ${name} after ${max_tries} attempts."
      exit 1
    fi
    candidate=$((candidate + 1))
  done

  echo "$candidate"
}

docker network inspect "$NETWORK_NAME" >/dev/null 2>&1 || docker network create "$NETWORK_NAME"

docker rm -f ogfashions-prometheus ogfashions-grafana >/dev/null 2>&1 || true

PROMETHEUS_HOST_PORT="$(resolve_host_port prometheus "$PROMETHEUS_HOST_PORT" "$PORT_CONFLICT_STRATEGY" "$PORT_SEARCH_LIMIT")"
GRAFANA_HOST_PORT="$(resolve_host_port grafana "$GRAFANA_HOST_PORT" "$PORT_CONFLICT_STRATEGY" "$PORT_SEARCH_LIMIT")"

docker run -d \
  --name ogfashions-prometheus \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p "${PROMETHEUS_HOST_PORT}:9090" \
  -v "$ROOT_DIR/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  prom/prometheus:v2.53.0

docker run -d \
  --name ogfashions-grafana \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p "${GRAFANA_HOST_PORT}:3000" \
  -v "$ROOT_DIR/grafana/provisioning:/etc/grafana/provisioning:ro" \
  -v "$ROOT_DIR/grafana/dashboards:/var/lib/grafana/dashboards:ro" \
  grafana/grafana:11.1.0

echo "Prometheus: http://localhost:${PROMETHEUS_HOST_PORT}"
echo "Grafana: http://localhost:${GRAFANA_HOST_PORT}"
