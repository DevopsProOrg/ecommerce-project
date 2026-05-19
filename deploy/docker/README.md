# Docker Deployment

This folder is set up so your long-lived production values can stay in git, while secrets stay only on the server.

## Files

- `docker.env.defaults`
  - committed production defaults for `ogstyles.store`
- `docker.env`
  - optional local override file, not tracked by git
- `docker.secrets.env`
  - required secrets file, not tracked by git
- `docker.secrets.env.example`
  - template for the secrets file
- `ec2-deploy.sh`
  - builds locally or pulls images, then runs MySQL, backend, and frontend containers
- `ec2-cleanup.sh`
  - stops containers and removes the Docker network while keeping MySQL data

## First-time server setup

After cloning the repo on EC2:

```bash
cd /ecommerce-project/deploy/docker
cp docker.secrets.env.example docker.secrets.env
vi docker.secrets.env
```

Set these real secret values:

- `MYSQL_ROOT_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `SPRING_MAIL_PASSWORD`

Then deploy:

```bash
cd /ecommerce-project
chmod +x deploy/docker/ec2-deploy.sh
./deploy/docker/ec2-deploy.sh
```

## What deploys

Containers:

- `ogfashions-mysql`
- `ogfashions-backend`
- `ogfashions-frontend`

Browser URLs:

- `https://ogstyles.store/`
- `https://ogstyles.store/healthz`
- `https://ogstyles.store/api/actuator/health`

Default host port bindings:

- backend host port: `8888`
- frontend host port: `80`

## Override behavior

Order of precedence:

1. `docker.env.defaults`
2. `docker.env`
3. `docker.secrets.env`

Use `docker.env` only when you want to override a committed default on a specific server without editing git-tracked files.
