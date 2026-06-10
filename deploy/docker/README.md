# Docker Deployment

This folder is for Linux, Ubuntu, and EC2 Docker deployment.

## Files

- `docker.env.defaults`
  - committed production defaults for `ogstyles.store`
- `docker.env.example`
  - template for optional non-secret overrides
- `docker.secrets.env.example`
  - template for secrets
- `ec2-deploy.sh`
  - main Docker deployment script
- `ec2-registry-deploy.sh`
  - registry-image deployment wrapper
- `ec2-rollback.sh`
  - rollback helper for previous image tags
- `ec2-cleanup.sh`
  - cleanup helper

## Port behavior

The Docker deploy script now supports:

- `PORT_CONFLICT_STRATEGY=fail`
  - strict mode for real servers where `80` and `8888` should be intentionally reserved
- `PORT_CONFLICT_STRATEGY=auto`
  - useful for local or shared hosts where a fallback port is acceptable
- `PORT_SEARCH_LIMIT=25`
  - how many next ports to try in auto mode

Preferred host ports:

- `FRONTEND_HOST_PORT`
- `BACKEND_HOST_PORT`

## Step-by-step

### Step 0. Run preflight

```bash
bash deploy/scripts/preflight-check.sh docker
```

### Step 1. Clone the repo on the server

```bash
git clone <YOUR_GIT_URL> /opt/ogfashions
cd /opt/ogfashions
```

### Step 2. Create the optional override file

```bash
cp deploy/docker/docker.env.example deploy/docker/docker.env
```

Edit only if needed.

For lower environments such as `l1`, `l2`, `l3`, or `l6`, you can:

- set `APP_PUBLIC_BASE_URL` to a server IP or load balancer URL [example: `http://44.208.191.131`]
- or leave it empty and access through the resolved frontend port

### Step 3. Create the secrets file

```bash
cp deploy/docker/docker.secrets.env.example deploy/docker/docker.secrets.env
vi deploy/docker/docker.secrets.env
```

Set:

- `MYSQL_ROOT_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `SPRING_MAIL_PASSWORD`

If you use managed RDS later, keep `SPRING_DATASOURCE_PASSWORD` here and set `USE_EXTERNAL_DB=true` plus `SPRING_DATASOURCE_URL` in `docker.env`.

### Step 4. Choose your port strategy

For internet-facing production:

- use `PORT_CONFLICT_STRATEGY=auto` when you want the script to avoid port conflicts automatically
- keep `FRONTEND_HOST_PORT=80`

For shared or test servers:

- set `PORT_CONFLICT_STRATEGY=auto`

### Step 5. Deploy

```bash
chmod +x deploy/docker/ec2-deploy.sh
bash deploy/docker/ec2-deploy.sh
```

### Step 6. Verify containers

```bash
docker ps
docker logs ogfashions-mysql --tail 100
docker logs ogfashions-backend --tail 100
docker logs ogfashions-frontend --tail 100
```

If `USE_EXTERNAL_DB=true`, you should not expect `ogfashions-mysql` to run locally on that host.

### Step 7. Test in browser

If production domain is already routed:

- `https://ogstyles.store/`
- `https://ogstyles.store/healthz`
- `https://ogstyles.store/api/actuator/health`

If testing by host port:

- `http://<server-ip>:<resolved-frontend-port>/`
- `http://<load-balancer-ip>/` [example for lower environments]

### Step 7A. Run smoke check

```bash
bash deploy/scripts/http-smoke.sh https://ogstyles.store
```

### Step 8. Registry-based deployment

Use this when Jenkins builds images and the server only needs to pull and run them:

```bash
chmod +x deploy/docker/ec2-registry-deploy.sh
bash deploy/docker/ec2-registry-deploy.sh
```

### Step 8A. Roll back to a previous image

```bash
chmod +x deploy/docker/ec2-rollback.sh
bash deploy/docker/ec2-rollback.sh deploy/docker/docker.env v1.0
```

This redeploys the selected release image tag without rebuilding locally.

### Step 9. Cleanup

```bash
chmod +x deploy/docker/ec2-cleanup.sh
./deploy/docker/ec2-cleanup.sh
```

## Common issues

### Frontend port already in use

If you want zero manual conflicts on shared hosts, use:

```env
PORT_CONFLICT_STRATEGY=auto
```

### Port 80 not reachable

Check:

- security group inbound `80`
- Nginx container running
- DNS pointing to the right host

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

### Move to managed RDS

Set:

```env
USE_EXTERNAL_DB=true
SPRING_DATASOURCE_URL=jdbc:mysql://<rds-endpoint>:3306/ecommerce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```
