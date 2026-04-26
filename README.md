# OG Fashions Deployment Guide

OG Fashions is now structured for:

- local development
- Docker-first deployment on a real Linux host such as EC2
- Kubernetes deployment with separate frontend, backend, and MySQL workloads

## Architecture

- `frontend/`
  Static storefront and admin UI served by Nginx
- `backend/`
  Spring Boot API on port `8888`
- `deploy/docker/`
  Docker-first deployment assets for EC2 or any Linux server
- `deploy/k8s/`
  Kubernetes manifests for frontend, backend, and MySQL

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

## Docker deployment on EC2 or Linux

### 1. Prepare environment file

Copy:

- [deploy/docker/docker.env.example](/E:/Restart/Projects/ecommerce-project/deploy/docker/docker.env.example)

to:

- `deploy/docker/docker.env`

Then replace all placeholder passwords, admin credentials, allowed origins, and Gmail values.

Important values to set:

- `APP_CORS_ALLOWED_ORIGINS`
  Use your EC2 public URL or real domain
- `APP_BOOTSTRAP_ADMIN_EMAIL`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `SPRING_MAIL_PASSWORD`
  Use a Gmail app password, not a regular Gmail password

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

- Frontend: `http://YOUR_SERVER_PUBLIC_IP/`
- Backend health: `http://YOUR_SERVER_PUBLIC_IP:8888/actuator/health`

### 4. Cleanup

```bash
chmod +x deploy/docker/ec2-cleanup.sh
./deploy/docker/ec2-cleanup.sh
```

This removes containers and the network but keeps the MySQL volume.

## Kubernetes deployment

Files are in:

- [deploy/k8s](/E:/Restart/Projects/ecommerce-project/deploy/k8s)

### 1. Build and push images

Replace the placeholder image names in:

- [backend-deployment.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/backend-deployment.yaml)
- [frontend-deployment.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/frontend-deployment.yaml)

Example:

- `your-dockerhub-or-ecr-user/ogfashions-backend:latest`
- `your-dockerhub-or-ecr-user/ogfashions-frontend:latest`

### 2. Create secret file

Copy:

- [secret.example.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/secret.example.yaml)

to a real secret manifest, fill the real values, and apply it first.

### 3. Apply manifests

```bash
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/secret.yaml
kubectl apply -k deploy/k8s
```

### 4. Configure ingress

Update the host in:

- [deploy/k8s/ingress.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/ingress.yaml)

Set it to your real domain, then point DNS to your ingress controller/load balancer.

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
