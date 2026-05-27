# Kubernetes Deployment

This folder is organized for multi-environment, org-style deployment.

## Layout

- `base/`
  - shared workloads and services
- `overlays/l1`
- `overlays/l2`
- `overlays/l3`
- `overlays/l6`
- `overlays/lp`
  - environment-specific namespaces, access model, replica counts, and ingress rules
- `scripts/deploy.sh`
  - applies an overlay and updates the backend and frontend images
- `scripts/rollback.sh`
  - rolls deployments back either to the previous rollout or to a specific immutable image tag
- `secrets/ogfashions-secrets.example.yaml`
  - secret template for any target namespace

Legacy `staging` and `production` overlays are still present for compatibility, but the current platform flow uses `l1`, `l2`, `l3`, `l6`, and `lp`.

## Environment model

The repo now treats these as first-class Kubernetes overlays:

- `l1`
- `l2`
- `l3`
- `l6`
- `lp`

Meaning:

- `l1`, `l2`, `l3`
  lower environments such as dev and testing
- `l6`
  pre-production or staging
- `lp`
  production

## What is deployed

Base components include:

- config map
- MySQL StatefulSet
- backend Deployment and Service
- frontend Deployment and Service
- ingress
- backend and frontend HPAs
- backend and frontend PodDisruptionBudgets

## Prerequisites

You need:

- a working Kubernetes cluster
- `kubectl`
- `bash`
- access to push images to Docker Hub or ECR
- a real secret manifest based on `secrets/ogfashions-secrets.example.yaml`
- an ingress controller such as ingress-nginx

Recommended choices:

- learning or low-cost: k3s on EC2
- AWS managed: EKS

## First-time setup

1. Install ingress-nginx in the cluster.
2. Copy `secrets/ogfashions-secrets.example.yaml` to a real secret file.
3. Update the namespace in the secret file to match the overlay.
4. Apply the secret before or immediately after the first deploy.

## Preflight

Run:

```bash
bash deploy/scripts/preflight-check.sh k8s
```

## Example deploy

```bash
bash deploy/k8s/scripts/deploy.sh lp yarrav7 release-12
```

That deploys:

- backend image: `docker.io/yarrav7/ogfashions-backend:release-12`
- frontend image: `docker.io/yarrav7/ogfashions-frontend:release-12`

## Example rollback

Roll back to the previous ReplicaSet revision:

```bash
bash deploy/k8s/scripts/rollback.sh lp
```

Roll back to a known image tag:

```bash
bash deploy/k8s/scripts/rollback.sh lp yarrav7 release-11 https://ogstyles.store
```

## Access model

- `l1`, `l2`, `l3`, `l6`
  use IP address, load balancer address, or server address
- `lp`
  uses the permanent production domain `ogstyles.store`

For lower levels, the overlay ingress rules are intentionally hostless so you can access them by load balancer IP or DNS without needing subdomains.

## Validation

Render an overlay before applying it:

```bash
kubectl kustomize deploy/k8s/overlays/lp
```

Check resources after deploy:

```bash
kubectl get all -n ogfashions-lp
```

Check backend logs:

```bash
kubectl logs deploy/ogfashions-backend -n ogfashions-lp
```

Check frontend logs:

```bash
kubectl logs deploy/ogfashions-frontend -n ogfashions-lp
```

Run smoke check if the ingress URL is reachable:

```bash
bash deploy/scripts/http-smoke.sh https://ogstyles.store
```

## Ingress and DNS

For lower levels, you can directly use the ingress or load balancer IP.

For production, point DNS to your ingress controller or load balancer.

For a real internet-facing setup:

1. add the DNS record
2. point it to the ingress controller or load balancer
3. enable HTTPS

## Database note

The current Kubernetes setup uses MySQL as a StatefulSet. This is fine for learning, staging, and controlled testing.

For stronger production readiness later, move to:

- Amazon RDS for MySQL

That will simplify backups, upgrades, and high availability.

The Docker deployment path already supports external RDS by setting `USE_EXTERNAL_DB=true` and `SPRING_DATASOURCE_URL`.

## Jenkins usage

The Jenkins pipeline deploys Kubernetes through:

- `deploy/k8s/scripts/deploy.sh`

Jenkins must have:

- `kubectl`
- a valid kubeconfig credential
- access to the cluster

See [../jenkins/README.md](../jenkins/README.md) and [../jenkins/PIPELINE-STAGES.md](../jenkins/PIPELINE-STAGES.md).
