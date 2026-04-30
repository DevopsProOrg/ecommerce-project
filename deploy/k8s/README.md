# Kubernetes Deployment Guide

This guide explains how to deploy OG Fashions to Kubernetes using the manifests and overlays in this repository.

## Folder structure

- [base](./base)
  Shared base manifests
- [overlays/staging](./overlays/staging)
  Staging-specific patch set
- [overlays/production](./overlays/production)
  Production-specific patch set
- [deploy.sh](./deploy.sh)
  Helper script to deploy an overlay with real image tags
- [secret.example.yaml](./secret.example.yaml)
  Template for runtime secrets

## What is deployed

Base components:

- namespace
- config map
- MySQL StatefulSet
- backend Deployment and Service
- frontend Deployment and Service
- ingress

## Prerequisites

You need:

- a working Kubernetes cluster
- `kubectl`
- `bash`
- access to push images to Docker Hub or ECR
- a real `deploy/k8s/secret.yaml`

Recommended choices:

- learning / low-cost: k3s on EC2
- AWS managed: EKS

## 1. Build and push Docker images

You must push both images before deploying.

Example:

```bash
docker build -t docker.io/YOUR_DOCKER_USER/ogfashions-backend:latest backend
docker build -t docker.io/YOUR_DOCKER_USER/ogfashions-frontend:latest frontend
docker push docker.io/YOUR_DOCKER_USER/ogfashions-backend:latest
docker push docker.io/YOUR_DOCKER_USER/ogfashions-frontend:latest
```

For traceable releases, prefer versioned tags:

```bash
docker build -t docker.io/YOUR_DOCKER_USER/ogfashions-backend:release-1 backend
docker build -t docker.io/YOUR_DOCKER_USER/ogfashions-frontend:release-1 frontend
docker push docker.io/YOUR_DOCKER_USER/ogfashions-backend:release-1
docker push docker.io/YOUR_DOCKER_USER/ogfashions-frontend:release-1
```

## 2. Create the Kubernetes secret manifest

Copy:

```bash
cp deploy/k8s/secret.example.yaml deploy/k8s/secret.yaml
```

Edit the real values:

```bash
vi deploy/k8s/secret.yaml
```

Replace placeholders for:

- MySQL password
- admin bootstrap password
- Gmail password
- any other secret runtime values

Do not commit `deploy/k8s/secret.yaml` with real secrets.

## 3. Validate the manifests

Check staging:

```bash
kubectl kustomize deploy/k8s/overlays/staging
```

Check production:

```bash
kubectl kustomize deploy/k8s/overlays/production
```

## 4. Deploy staging

```bash
bash deploy/k8s/deploy.sh staging docker.io/YOUR_DOCKER_USER/ogfashions-backend:release-1 docker.io/YOUR_DOCKER_USER/ogfashions-frontend:release-1
```

What the script does:

1. applies namespace
2. applies `deploy/k8s/secret.yaml`
3. renders the selected overlay
4. replaces placeholder backend and frontend image names
5. applies the rendered manifests

## 5. Deploy production

```bash
bash deploy/k8s/deploy.sh production docker.io/YOUR_DOCKER_USER/ogfashions-backend:release-1 docker.io/YOUR_DOCKER_USER/ogfashions-frontend:release-1
```

## 6. Verify deployment

Check namespace resources:

```bash
kubectl get all -n ogfashions
```

Check pods:

```bash
kubectl get pods -n ogfashions
```

Describe any failing pod:

```bash
kubectl describe pod <pod-name> -n ogfashions
```

Check backend logs:

```bash
kubectl logs deploy/ogfashions-backend -n ogfashions
```

Check frontend logs:

```bash
kubectl logs deploy/ogfashions-frontend -n ogfashions
```

## 7. Access the application

If you do not have a domain yet:

- use ingress/controller external IP or load balancer DNS
- or use `kubectl port-forward` temporarily

Example port-forward:

```bash
kubectl port-forward svc/ogfashions-frontend 8080:80 -n ogfashions
```

Then open:

- `http://localhost:8080/`

## 8. Environment overlays

### Staging overlay

Use staging for:

- lower-environment validation
- frequent deployments
- testing integration

### Production overlay

Use production for:

- controlled releases
- approval-based deploys
- larger replica counts or stricter settings later

## 9. Ingress notes

Current manifests are usable without a domain for basic testing, but for a real internet-facing setup later:

1. add a DNS name
2. point the DNS to the ingress controller or load balancer
3. enable HTTPS

If no domain exists yet, keep using:

- load balancer DNS
- ingress external IP
- port-forward for validation

## 10. Database note

The current Kubernetes setup uses MySQL as a StatefulSet. This is fine for learning, staging, and controlled testing.

For stronger production readiness later, move to:

- Amazon RDS for MySQL

That will simplify:

- backups
- upgrades
- high availability

## 11. Jenkins usage with Kubernetes deployment

The Jenkins pipeline deploys Kubernetes through:

- `deploy/k8s/deploy.sh`

Jenkins must have:

- `kubectl`
- a valid kubeconfig credential
- access to the cluster

See [../jenkins/README.md](../jenkins/README.md) and [../jenkins/PIPELINE-STAGES.md](../jenkins/PIPELINE-STAGES.md).
