# OG Fashions Deployment Guide

OG Fashions is now structured for:

- local development
- local Docker deployment
- Docker-first deployment on a real Linux host such as EC2
- Kubernetes deployment with separate frontend, backend, and MySQL workloads
- Terraform-based AWS infrastructure
- Ansible-based server configuration
- Prometheus and Grafana monitoring

## Architecture

- `frontend/`
  Static storefront and admin UI served by Nginx
- `backend/`
  Spring Boot API on port `8888`
- `deploy/docker/`
  Docker-first deployment assets for EC2 or any Linux server
- `deploy/local/`
  Local Docker deployment scripts and env templates
- `deploy/k8s/`
  Organization-style Kubernetes base and overlays
- `deploy/terraform/`
  AWS infrastructure provisioning
- `deploy/ansible/`
  Host and app configuration management
- `deploy/monitoring/`
  Prometheus and Grafana deployment assets
- `deploy/jenkins/`
  Jenkins setup references and environment examples
- `deploy/ORG-PLATFORM-RUNBOOK.md`
  End-to-end execution guide across all layers

## Production notes

The app no longer needs your laptop MySQL to run in deployed environments.

For Docker and Kubernetes:

- MySQL runs as its own workload with persistent storage
- frontend proxies `/api` traffic to the backend
- backend health endpoints are available through Spring Boot Actuator
- Gmail SMTP stays external and is supplied through secrets/env vars

Recommended real production direction:

1. Use EC2 + Docker for the first internet-facing deployment.
2. Move MySQL to Amazon RDS when you want a stronger production database setup.
3. Move images to ECR or Docker Hub before Kubernetes deployment.
4. Use an ingress controller and a real domain with HTTPS in Kubernetes.

## Local development

### Backend

```powershell
cd backend
mvn spring-boot:run
```

Backend default URL:

- `http://localhost:8888`

### Frontend

Serve the `frontend` folder with any static web server, or use the frontend Docker image.

## Local Docker deployment

Use:

- [deploy/local/README.md](/E:/Restart/Projects/ecommerce-project/deploy/local/README.md)

This path runs:

- MySQL
- backend
- frontend

on one machine with Docker.

## Docker deployment on EC2 or Linux

### 1. Prepare secrets once

The repo now includes committed production defaults for `ogstyles.store` in:

- [deploy/docker/docker.env.defaults](/E:/Restart/Projects/ecommerce-project/deploy/docker/docker.env.defaults)

Create only the server-local secrets file:

```bash
cp deploy/docker/docker.secrets.env.example deploy/docker/docker.secrets.env
vi deploy/docker/docker.secrets.env
```

Set these real values:

- `MYSQL_ROOT_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `SPRING_MAIL_PASSWORD`

This keeps permanent non-secret settings in code and keeps secrets out of git.

### 2. Deploy

On Linux or EC2:

```bash
chmod +x deploy/docker/ec2-deploy.sh
./deploy/docker/ec2-deploy.sh
```

This script will:

- build the backend image
- build the frontend image
- create a Docker network
- run MySQL with a Docker volume
- run the backend container
- run the frontend container

### 3. Access

- Frontend: `https://ogstyles.store/`
- Frontend health: `https://ogstyles.store/healthz`
- Backend health: `https://ogstyles.store/api/actuator/health`

### 4. Cleanup

```bash
chmod +x deploy/docker/ec2-cleanup.sh
./deploy/docker/ec2-cleanup.sh
```

This removes containers and the network but keeps the MySQL volume.

## Kubernetes deployment

Files are in:

- [deploy/k8s](/E:/Restart/Projects/ecommerce-project/deploy/k8s)

Use:

- [deploy/k8s/README.md](/E:/Restart/Projects/ecommerce-project/deploy/k8s/README.md)

### 1. Build and push images

Replace the placeholder image names in:

- [backend-deployment.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/backend-deployment.yaml)
- [frontend-deployment.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/frontend-deployment.yaml)

Example:

- `your-dockerhub-or-ecr-user/ogfashions-backend:latest`
- `your-dockerhub-or-ecr-user/ogfashions-frontend:latest`

### 2. Create secret file

Copy:

- [ogfashions-secrets.example.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/secrets/ogfashions-secrets.example.yaml)

to a real secret manifest, fill the real values, set the target namespace, and apply it first.

### 3. Apply manifests

```bash
bash deploy/k8s/scripts/deploy.sh lp yarrav7 latest
```

### 4. Configure ingress

The repo already includes overlays for:

- `l1.ogstyles.store`
- `l2.ogstyles.store`
- `l3.ogstyles.store`
- `l6.ogstyles.store`
- `ogstyles.store`

Point DNS to your ingress controller or load balancer.

## Terraform

Use:

- [deploy/terraform/README.md](/E:/Restart/Projects/ecommerce-project/deploy/terraform/README.md)

This provisions AWS infrastructure for:

- network
- EC2 app hosts
- optional EKS clusters
- Route53 records

across `l1`, `l2`, `l3`, `l6`, and `lp`.

## Ansible

Use:

- [deploy/ansible/README.md](/E:/Restart/Projects/ecommerce-project/deploy/ansible/README.md)

This configures:

- Docker hosts
- app directories
- secrets files
- Node Exporter

after Terraform provisioning.

## Monitoring

Use:

- [deploy/monitoring/README.md](/E:/Restart/Projects/ecommerce-project/deploy/monitoring/README.md)

This includes:

- Docker Prometheus + Grafana
- Kubernetes Prometheus + Grafana
- backend metrics scraping through `/actuator/prometheus`

## Validation checklist

After deployment, verify:

1. Frontend loads from the server domain.
2. Login and admin pages work.
3. Product listing loads through `/api`.
4. Cart and wishlist still function.
5. Support tickets are saved in MySQL.
6. Order emails and support emails are delivered.
7. Backend health is `UP`.

## Current implementation details

- `docker-compose.yml` was removed as requested
- backend Docker build is now multi-stage
- frontend Nginx now supports configurable backend upstream
- backend now exposes health endpoints for container probes
- deployment assets are split into Docker-first and Kubernetes-ready folders

## Important production advice

The included MySQL container and StatefulSet are suitable for real testing and controlled deployment work, but for a stronger production setup you should move the database to a managed service such as Amazon RDS. That will reduce operational risk, improve backups, and make upgrades safer.

## Full platform runbook

See:

- [deploy/ORG-PLATFORM-RUNBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/ORG-PLATFORM-RUNBOOK.md)
