# Monitoring Setup Guide

This folder contains the Kubernetes monitoring baseline for OG Fashions.

It includes:

- Prometheus
- Grafana
- backend metrics scraping through Spring Boot Actuator

## Folder structure

- [k8s](./k8s)
  Kubernetes manifests for monitoring

## What is monitored

The backend exposes:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

Prometheus is configured to scrape backend metrics from the staging and production service names defined in the manifests.

## Prerequisites

You need:

- a Kubernetes cluster
- `kubectl`
- the OG Fashions backend deployed in Kubernetes

## 1. Deploy monitoring stack

```bash
kubectl apply -k deploy/monitoring/k8s
```

## 2. Verify resources

```bash
kubectl get all -n ogfashions-monitoring
```

## 3. Access Prometheus

Example port-forward:

```bash
kubectl port-forward svc/prometheus 9090:9090 -n ogfashions-monitoring
```

Open:

- `http://localhost:9090`

## 4. Access Grafana

Example port-forward:

```bash
kubectl port-forward svc/grafana 3000:3000 -n ogfashions-monitoring
```

Open:

- `http://localhost:3000`

Default credentials from the current manifest:

- user: `admin`
- password: `admin123`

Change the password immediately after first login.

## 5. What is already included

The monitoring manifests already provide:

- Prometheus deployment
- Grafana deployment
- starter dashboard provisioning
- datasource provisioning

## 6. Recommended next improvements

For a stronger real-world monitoring setup later, add:

- Alertmanager
- Slack or email alert routing
- node exporter
- kube-state-metrics
- persistent volumes for Prometheus and Grafana
- custom Grafana dashboards for:
  - orders
  - support tickets
  - mail delivery success/failure
  - backend response times

## 7. Troubleshooting

### Prometheus not scraping

Check:

```bash
kubectl logs deploy/prometheus -n ogfashions-monitoring
```

Also confirm backend service names still match the scrape targets in:

- [k8s/prometheus-configmap.yaml](./k8s/prometheus-configmap.yaml)

### Grafana not opening

Check:

```bash
kubectl logs deploy/grafana -n ogfashions-monitoring
```

### No metrics visible

Check backend metrics endpoint manually:

```bash
kubectl exec -it deploy/prometheus -n ogfashions-monitoring -- wget -qO- http://ogfashions-backend.ogfashions-staging.svc.cluster.local:8888/actuator/prometheus
```
