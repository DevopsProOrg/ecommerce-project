#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK_NAME="${MONITORING_DOCKER_NETWORK:-ogfashions-net}"
PROMETHEUS_HOST_PORT="${PROMETHEUS_HOST_PORT:-9090}"
GRAFANA_HOST_PORT="${GRAFANA_HOST_PORT:-3000}"
ALERTMANAGER_HOST_PORT="${ALERTMANAGER_HOST_PORT:-9093}"
LOKI_HOST_PORT="${LOKI_HOST_PORT:-3100}"
PORT_CONFLICT_STRATEGY="${PORT_CONFLICT_STRATEGY:-auto}"
PORT_SEARCH_LIMIT="${PORT_SEARCH_LIMIT:-25}"
ALERTMANAGER_ENV_FILE="${ALERTMANAGER_ENV_FILE:-$ROOT_DIR/alertmanager.env}"
ALERTMANAGER_RENDERED_CONFIG="$ROOT_DIR/alertmanager.generated.yml"
ENABLE_PROMTAIL="${ENABLE_PROMTAIL:-true}"

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

docker rm -f ogfashions-prometheus ogfashions-grafana ogfashions-alertmanager ogfashions-loki ogfashions-promtail >/dev/null 2>&1 || true

PROMETHEUS_HOST_PORT="$(resolve_host_port prometheus "$PROMETHEUS_HOST_PORT" "$PORT_CONFLICT_STRATEGY" "$PORT_SEARCH_LIMIT")"
GRAFANA_HOST_PORT="$(resolve_host_port grafana "$GRAFANA_HOST_PORT" "$PORT_CONFLICT_STRATEGY" "$PORT_SEARCH_LIMIT")"
ALERTMANAGER_HOST_PORT="$(resolve_host_port alertmanager "$ALERTMANAGER_HOST_PORT" "$PORT_CONFLICT_STRATEGY" "$PORT_SEARCH_LIMIT")"
LOKI_HOST_PORT="$(resolve_host_port loki "$LOKI_HOST_PORT" "$PORT_CONFLICT_STRATEGY" "$PORT_SEARCH_LIMIT")"

if [[ -f "$ALERTMANAGER_ENV_FILE" ]]; then
  set -a
  source "$ALERTMANAGER_ENV_FILE"
  set +a
fi

cat > "$ALERTMANAGER_RENDERED_CONFIG" <<EOF
global:
  smtp_smarthost: '${SMTP_SMARTHOST:-smtp.gmail.com:587}'
  smtp_from: '${SMTP_FROM:-alerts@ogstyles.store}'
  smtp_auth_username: '${SMTP_AUTH_USERNAME:-contact.ogfashions@gmail.com}'
  smtp_auth_password: '${SMTP_AUTH_PASSWORD:-replace-with-real-app-password}'
  smtp_require_tls: true

route:
  receiver: operations-notifications
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 2h

receivers:
  - name: operations-notifications
    email_configs:
      - to: '${ALERT_EMAIL_TO:-contact.ogfashions@gmail.com}'
        send_resolved: true
    webhook_configs:
      - url: '${ALERT_WEBHOOK_URL:-http://ogfashions-backend:8888/api/monitoring/alerts/webhook}'
        send_resolved: true
EOF

docker volume create ogfashions-loki-data >/dev/null 2>&1 || true

MSYS_NO_PATHCONV=1 docker run -d \
  --name ogfashions-prometheus \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p "${PROMETHEUS_HOST_PORT}:9090" \
  -v "${ROOT_DIR}/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  -v "${ROOT_DIR}/alert-rules.yml:/etc/prometheus/alert-rules.yml:ro" \
  prom/prometheus:v2.53.0

MSYS_NO_PATHCONV=1 docker run -d \
  --name ogfashions-grafana \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p "${GRAFANA_HOST_PORT}:3000" \
  -v "$ROOT_DIR/grafana/provisioning:/etc/grafana/provisioning:ro" \
  -v "$ROOT_DIR/grafana/dashboards:/var/lib/grafana/dashboards:ro" \
  grafana/grafana:11.1.0

MSYS_NO_PATHCONV=1 docker run -d \
  --name ogfashions-alertmanager \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p "${ALERTMANAGER_HOST_PORT}:9093" \
  -v "$ALERTMANAGER_RENDERED_CONFIG:/etc/alertmanager/alertmanager.yml:ro" \
  prom/alertmanager:v0.27.0

MSYS_NO_PATHCONV=1 docker run -d \
  --name ogfashions-loki \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  -p "${LOKI_HOST_PORT}:3100" \
  -v "${ROOT_DIR}/loki-config.yml:/etc/loki/local-config.yaml:ro" \
  -v ogfashions-loki-data:/loki \
  grafana/loki:3.1.1 \
  -config.file=/etc/loki/local-config.yaml

if [[ "${ENABLE_PROMTAIL,,}" == "true" ]]; then
  MSYS_NO_PATHCONV=1 docker run -d \
    --name ogfashions-promtail \
    --network "$NETWORK_NAME" \
    --restart unless-stopped \
    -v "$ROOT_DIR/promtail-config.yml:/etc/promtail/config.yml:ro" \
    -v /var/lib/docker/containers:/var/lib/docker/containers:ro \
    grafana/promtail:3.1.1 -config.file=/etc/promtail/config.yml
else
  echo "Promtail startup skipped."
fi

echo "Prometheus: http://localhost:${PROMETHEUS_HOST_PORT}"
echo "Grafana: http://localhost:${GRAFANA_HOST_PORT}"
echo "Alertmanager: http://localhost:${ALERTMANAGER_HOST_PORT}"
echo "Loki: http://localhost:${LOKI_HOST_PORT}"
echo "Promtail enabled: ${ENABLE_PROMTAIL}"
