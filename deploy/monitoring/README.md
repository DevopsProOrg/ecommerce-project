# Monitoring Stack

This monitoring layout provides Prometheus and Grafana for the ecommerce platform.

## Included

- Docker monitoring stack for EC2 or local Docker hosts
- Kubernetes monitoring manifests for cluster deployments
- Prometheus scrape config for backend metrics
- Grafana with pre-provisioned datasource and dashboard

## Default ports

- Prometheus: `9090`
- Grafana: `3000`

## Primary targets

- backend metrics: `/actuator/prometheus`
- frontend health: `/healthz`
- backend health: `/actuator/health`

## Kubernetes usage

Apply:

```bash
kubectl apply -k deploy/monitoring/k8s
```

Verify:

```bash
kubectl get all -n monitoring
```

Access Prometheus:

```bash
kubectl port-forward svc/prometheus 9090:9090 -n monitoring
```

Access Grafana:

```bash
kubectl port-forward svc/grafana 3000:3000 -n monitoring
```

Open:

- `http://localhost:9090`
- `http://localhost:3000`

## Recommended next improvements

For a stronger real-world monitoring setup later, add:

- Alertmanager
- Slack or email alert routing
- node exporter
- kube-state-metrics
- persistent volumes for Prometheus and Grafana
- custom Grafana dashboards for orders, support tickets, mail delivery, and backend latency

## Troubleshooting

### Prometheus not scraping

Check:

```bash
kubectl logs deploy/prometheus -n monitoring
```

### Grafana not opening

Check:

```bash
kubectl logs deploy/grafana -n monitoring
```

### No metrics visible

Check the backend metrics endpoint manually from inside the cluster against the target namespace service.
