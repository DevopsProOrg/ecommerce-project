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
  - environment-specific namespaces, domains, replica counts, and ingress rules
- `scripts/deploy.sh`
  - applies an overlay and updates the backend/frontend images
- `secrets/ogfashions-secrets.example.yaml`
  - secret template for any target namespace

## Environment model

You asked for levels:

- `l1`
- `l2`
- `l3`
- `l6`
- `lp`

This repo now treats them as first-class Kubernetes overlays.

## First-time setup

1. Install ingress-nginx in the cluster.
2. Create the namespace and workloads through the overlay.
3. Copy `secrets/ogfashions-secrets.example.yaml` to a real secret file.
4. Update the namespace in the secret file to match the overlay.
5. Apply the secret before or immediately after the first deploy.

## Example deploy

```bash
bash deploy/k8s/scripts/deploy.sh lp yarrav7 release-12
```

That deploys:

- backend image: `docker.io/yarrav7/ogfashions-backend:release-12`
- frontend image: `docker.io/yarrav7/ogfashions-frontend:release-12`

## Domains

- `l1`: `l1.ogstyles.store`
- `l2`: `l2.ogstyles.store`
- `l3`: `l3.ogstyles.store`
- `l6`: `l6.ogstyles.store`
- `lp`: `ogstyles.store`
