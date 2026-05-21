# Monitoring Stack

This folder provides Prometheus and Grafana for Docker-based and Kubernetes-based deployments.

## Docker monitoring

### Step 1. Start monitoring containers

Linux, Ubuntu, WSL, or Git Bash:

```bash
bash deploy/monitoring/docker/run-monitoring.sh
```

Windows PowerShell:

```powershell
./deploy/monitoring/docker/run-monitoring.ps1
```

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
```

### Step 4. Check backend metrics

Prometheus should scrape:

- `/actuator/prometheus`

### Step 5. Improve later

Recommended next layers:

- Alertmanager
- Slack or email alert routing
- kube-state-metrics
- persistent volumes
- custom Grafana dashboards
