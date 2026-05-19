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
