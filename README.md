# OG Fashions Deployment Guide

OG Fashions is structured for:

- local development
- local Docker deployment
- Docker-first deployment on a Linux host such as EC2
- Kubernetes deployment with frontend, backend, and MySQL workloads
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

The app no longer depends on your laptop MySQL in deployed environments.

For Docker and Kubernetes:

- MySQL runs as its own workload with persistent storage
- frontend proxies `/api` traffic to the backend
- backend health endpoints are available through Spring Boot Actuator
- Gmail SMTP stays external and is supplied through secrets or env vars

Recommended production direction:

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

This path runs MySQL, backend, and frontend on one machine with Docker.

## Docker deployment on EC2 or Linux

Use:

- [deploy/docker/README.md](/E:/Restart/Projects/ecommerce-project/deploy/docker/README.md)

The repo includes committed production defaults for `ogstyles.store` in:

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

Then deploy:

```bash
chmod +x deploy/docker/ec2-deploy.sh
./deploy/docker/ec2-deploy.sh
```

Access:

- Frontend: `https://ogstyles.store/`
- Frontend health: `https://ogstyles.store/healthz`
- Backend health: `https://ogstyles.store/api/actuator/health`

## Kubernetes deployment

Files are in:

- [deploy/k8s](/E:/Restart/Projects/ecommerce-project/deploy/k8s)

Use:

- [deploy/k8s/README.md](/E:/Restart/Projects/ecommerce-project/deploy/k8s/README.md)

Example apply:

```bash
bash deploy/k8s/scripts/deploy.sh lp yarrav7 latest
```

This repo includes overlays for:

- `l1.ogstyles.store`
- `l2.ogstyles.store`
- `l3.ogstyles.store`
- `l6.ogstyles.store`
- `ogstyles.store`

Legacy `staging` and `production` manifests are still present for compatibility, but the current org-level flow uses `l1`, `l2`, `l3`, `l6`, and `lp`.

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

## Jenkins pipeline

This repo includes:

- [Jenkinsfile](/E:/Restart/Projects/ecommerce-project/Jenkinsfile)
- [deploy/README.md](/E:/Restart/Projects/ecommerce-project/deploy/README.md)
- [deploy/jenkins/README.md](/E:/Restart/Projects/ecommerce-project/deploy/jenkins/README.md)
- [deploy/jenkins/PIPELINE-STAGES.md](/E:/Restart/Projects/ecommerce-project/deploy/jenkins/PIPELINE-STAGES.md)
- [deploy/DEVOPS-RUNBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/DEVOPS-RUNBOOK.md)

The pipeline is designed for a real branch flow:

1. Feature branches validate code only.
2. `main` and `master` can build and push images.
3. `release` and `release/*` can deploy to production-style targets.
4. Jenkins can deploy to Docker on EC2, Kubernetes, or both.

## Validation checklist

After deployment, verify:

1. Frontend loads from the server domain.
2. Login and admin pages work.
3. Product listing loads through `/api`.
4. Cart and wishlist still function.
5. Support tickets are saved in MySQL.
6. Order emails and support emails are delivered.
7. Backend health is `UP`.

## Full platform runbook

See:

- [deploy/ORG-PLATFORM-RUNBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/ORG-PLATFORM-RUNBOOK.md)
