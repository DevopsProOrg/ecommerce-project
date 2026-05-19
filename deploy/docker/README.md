# Docker Deployment

This folder is set up so your long-lived production values can stay in git, while secrets stay only on the server.

## Files

- `docker.env.defaults`
  - committed production defaults for `ogstyles.store`
- `docker.env.example`
  - template for optional non-secret overrides
- `docker.env`
  - optional local override file, not tracked by git
- `docker.secrets.env`
  - required secrets file, not tracked by git
- `docker.secrets.env.example`
  - template for the secrets file
- `ec2-deploy.sh`
  - builds locally or pulls images, then runs MySQL, backend, and frontend containers
- `ec2-registry-deploy.sh`
  - wrapper that forces registry-based deployment
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

MySQL is kept inside the Docker network and is not exposed publicly by default.

## Override behavior

Order of precedence:

1. `docker.env.defaults`
2. `docker.env`
3. `docker.secrets.env`

Use `docker.env` only when you want to override a committed default on a specific server without editing git-tracked files.

## Registry-based deployment

Use registry mode for Jenkins-based rollouts or when you build elsewhere and deploy images on the server:

```bash
chmod +x deploy/docker/ec2-registry-deploy.sh
./deploy/docker/ec2-registry-deploy.sh
```

## Verification

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

## Cleanup

To stop the application containers but keep the MySQL volume:

```bash
chmod +x deploy/docker/ec2-cleanup.sh
./deploy/docker/ec2-cleanup.sh
```

## Common issues

### Port 80 not reachable

Check the EC2 security group and allow inbound `80/tcp`.

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

## Jenkins usage

Jenkins uses the same deployment path in registry mode and connects to the EC2 host through SSH to run:

- `deploy/docker/ec2-registry-deploy.sh`

See [../jenkins/README.md](../jenkins/README.md) for the full Jenkins configuration.
