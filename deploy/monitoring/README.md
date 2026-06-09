# Monitoring Stack

This folder provides:

- Prometheus
- Grafana
- Loki
- Promtail
- Alertmanager

for Docker-based and Kubernetes-based deployments.

## Docker monitoring

### Step 1. Start monitoring containers

Linux, Ubuntu, WSL, or Git Bash:

```bash
bash deploy/monitoring/docker/run-monitoring.sh
```

Git Bash on Windows is now handled automatically by the script:

- `MSYS_NO_PATHCONV=1`
- `MSYS2_ARG_CONV_EXCL=*`

This prevents Docker bind mounts like `/etc/prometheus/...` and `/etc/loki/...` from being incorrectly rewritten to `C:\Program Files\Git\...`.

Windows PowerShell:

```powershell
./deploy/monitoring/docker/run-monitoring.ps1
```

### Step 1A. Configure runtime alert email

```bash
cp deploy/monitoring/docker/alertmanager.env.example deploy/monitoring/docker/alertmanager.env
```

Set:

- `SMTP_SMARTHOST`
- `SMTP_FROM`
- `SMTP_AUTH_USERNAME`
- `SMTP_AUTH_PASSWORD`
- `ALERT_EMAIL_TO`

### Step 2. Port behavior

Docker monitoring also supports automatic port conflict handling:

- `PROMETHEUS_HOST_PORT`
- `GRAFANA_HOST_PORT`
- `PORT_CONFLICT_STRATEGY`
- `PORT_SEARCH_LIMIT`

Default strategy is `auto`.

### Step 3. Open the dashboards

Use the printed ports.

Typical defaults:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Alertmanager: `http://localhost:9093`
- Loki: `http://localhost:3100`

Grafana now has:

- Prometheus datasource
- Loki datasource

### Step 3A. Windows note for Promtail

On native Windows hosts, Promtail is skipped by default because:

- `/var/lib/docker/containers` is a Linux host path
- Docker Desktop on Windows does not expose that path the same way as Linux

That means on native Windows local runs:

- Prometheus: works
- Grafana: works
- Alertmanager: works
- Loki: works
- Promtail: skipped intentionally

Use WSL Ubuntu, Linux, or EC2 if you want Docker-container log scraping with Promtail.

## Kubernetes monitoring

### Step 1. Apply the monitoring stack

```bash
kubectl apply -k deploy/monitoring/k8s
```

### Step 2. Verify pods and services

```bash
kubectl get all -n monitoring
```

### Step 3. Port-forward for access

```bash
kubectl port-forward svc/prometheus 9090:9090 -n monitoring
kubectl port-forward svc/grafana 3000:3000 -n monitoring
kubectl port-forward svc/alertmanager 9093:9093 -n monitoring
kubectl port-forward svc/loki 3100:3100 -n monitoring
```

### Step 4. Check backend metrics

Prometheus should scrape:

- `/actuator/prometheus`

### Step 5. Improve later

Recommended next layers:

1. replace placeholder SMTP values in `deploy/monitoring/k8s/alertmanager-configmap.yaml`
2. add persistent volumes for Grafana, Prometheus, and Loki
3. add kube-state-metrics and node exporter
4. route alerts to Slack or Teams
5. add retention tuning for logs and metrics
