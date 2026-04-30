# Docker Deployment Guide

This guide covers:

- local Docker deployment
- EC2 Docker deployment
- registry-based deployment
- Jenkins-triggered Docker rollout

The Docker scripts in this folder are the primary deployment path for a simple real-world Linux or EC2 server.

## Files in this folder

- [docker.env.example](./docker.env.example)
  Template environment file
- [ec2-deploy.sh](./ec2-deploy.sh)
  Builds locally on the server or pulls images, then deploys MySQL, backend, and frontend containers
- [ec2-registry-deploy.sh](./ec2-registry-deploy.sh)
  Wrapper that forces registry-based deployment
- [ec2-cleanup.sh](./ec2-cleanup.sh)
  Stops and removes containers and Docker network while keeping the MySQL volume

## Architecture

Containers created by the scripts:

- `ogfashions-mysql`
- `ogfashions-backend`
- `ogfashions-frontend`

Default exposed ports:

- frontend: `80`
- backend: `8888`

MySQL is not exposed publicly by the script. It is kept inside the Docker network.

## Prerequisites

On the target Linux or EC2 host install:

- Docker
- Git
- Bash

Example for Amazon Linux:

```bash
sudo yum update -y
sudo yum install -y git docker
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
```

Reconnect after adding the user to the Docker group.

## 1. Clone the repo on the target host

```bash
sudo mkdir -p /opt/ogfashions
sudo chown -R $USER:$USER /opt/ogfashions
git clone <YOUR_GITHUB_REPO_URL> /opt/ogfashions
cd /opt/ogfashions
```

If it already exists:

```bash
cd /opt/ogfashions
git pull
```

## 2. Create the real environment file

Copy the template:

```bash
cp deploy/docker/docker.env.example deploy/docker/docker.env
```

Edit it:

```bash
vi deploy/docker/docker.env
```

## 3. Required variables in `docker.env`

At minimum, update these:

- `APP_PUBLIC_BASE_URL`
  - use your Elastic IP if you do not have a domain
- `MYSQL_ROOT_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_BOOTSTRAP_ADMIN_EMAIL`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `SPRING_MAIL_PASSWORD`

### Recommended example

```env
APP_NAME=ogfashions
APP_ENV=production
APP_PUBLIC_BASE_URL=http://YOUR_ELASTIC_IP

MYSQL_DATABASE=ecommerce
MYSQL_ROOT_PASSWORD=ChangeThisMysqlPassword123!
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=ChangeThisMysqlPassword123!
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
SPRING_JPA_HIBERNATE_DDL_AUTO=update

APP_CORS_ALLOWED_ORIGINS=http://YOUR_ELASTIC_IP,http://127.0.0.1:5500,http://localhost:5500
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://*.compute.amazonaws.com,https://*.compute.amazonaws.com,http://*.elb.amazonaws.com,https://*.elb.amazonaws.com

APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_NAME=Platform Admin
APP_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
APP_BOOTSTRAP_ADMIN_PASSWORD=ChangeThisImmediately123!

APP_MAIL_ENABLED=true
APP_MAIL_FROM=contact.ogfashions@gmail.com
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=contact.ogfashions@gmail.com
SPRING_MAIL_PASSWORD=YOUR_GMAIL_APP_PASSWORD
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS_ENABLE=true

BACKEND_UPSTREAM=http://ogfashions-backend:8888
IMAGE_TAG=latest
BACKEND_IMAGE=
FRONTEND_IMAGE=
BUILD_IMAGES_LOCALLY=true
```

## 4. Deploy using local build on the server

This is the simplest first deployment path.

```bash
chmod +x deploy/docker/ec2-deploy.sh
./deploy/docker/ec2-deploy.sh
```

What this does:

1. reads `deploy/docker/docker.env`
2. creates the Docker network if missing
3. creates the MySQL volume if missing
4. builds backend and frontend images locally when `BUILD_IMAGES_LOCALLY=true`
5. starts MySQL
6. waits for MySQL readiness
7. starts backend
8. starts frontend

## 5. Deploy using registry images

Use this for Jenkins-based deployments or when you build elsewhere and only deploy images on the server.

Update `deploy/docker/docker.env`:

```env
BUILD_IMAGES_LOCALLY=false
BACKEND_IMAGE=docker.io/YOUR_DOCKER_USER/ogfashions-backend:latest
FRONTEND_IMAGE=docker.io/YOUR_DOCKER_USER/ogfashions-frontend:latest
IMAGE_TAG=latest
```

Then run:

```bash
chmod +x deploy/docker/ec2-registry-deploy.sh
./deploy/docker/ec2-registry-deploy.sh
```

## 6. Verify deployment

Check containers:

```bash
docker ps
```

Check logs:

```bash
docker logs ogfashions-mysql --tail 100
docker logs ogfashions-backend --tail 100
docker logs ogfashions-frontend --tail 100
```

Open in browser:

- `http://YOUR_ELASTIC_IP/`
- `http://YOUR_ELASTIC_IP/api/actuator/health`

Expected health result:

```json
{"status":"UP"}
```

## 7. Update the application later

### Local-build deployment

```bash
cd /opt/ogfashions
git pull
./deploy/docker/ec2-deploy.sh
```

### Registry-based deployment

```bash
cd /opt/ogfashions
git pull
./deploy/docker/ec2-registry-deploy.sh
```

## 8. Cleanup

To stop the application containers but keep the MySQL volume:

```bash
chmod +x deploy/docker/ec2-cleanup.sh
./deploy/docker/ec2-cleanup.sh
```

## 9. AWS-specific recommendation

Always use an Elastic IP on EC2.

Without Elastic IP:

- stop/start changes the public IP
- you must update `APP_PUBLIC_BASE_URL`
- you may need to update Jenkins environment values

With Elastic IP:

- the application URL stays stable
- Docker and Jenkins configs stay stable

## 10. Common issues

### Port 80 not reachable

Check EC2 security group:

- allow inbound `80/tcp` from `0.0.0.0/0`

### Port 8888 conflict

Check for another backend process or container:

```bash
docker ps
ss -ltnp | grep 8888
```

### Mail not sending

Check:

- Gmail app password
- SMTP username
- backend logs

### MySQL not starting

Check:

```bash
docker logs ogfashions-mysql --tail 200
```

### Permission denied for Docker

Reconnect after:

```bash
sudo usermod -aG docker <linux-user>
```

## 11. Jenkins usage with Docker deployment

Jenkins uses the same deployment path in registry mode.

The pipeline connects to the remote EC2 host through SSH and runs:

- `deploy/docker/ec2-registry-deploy.sh`

To support Jenkins rollout, ensure:

1. repo exists on EC2 at `/opt/ogfashions`
2. `deploy/docker/docker.env` exists on the server
3. Jenkins SSH key can log in
4. Docker is installed and usable on the server

See [../jenkins/README.md](../jenkins/README.md) for the full Jenkins configuration.
