# Local Docker Deployment

This path is for a full Linux or Ubuntu container-based run on one machine and is the safest first validation step before EC2, Kubernetes, Terraform, or Ansible.

## Files

- `local-docker.env.example`
- `local-docker.secrets.env.example`
- `local-docker.sh`
- `local-docker-cleanup.sh`

## Port behavior

Local Docker now supports automatic port conflict handling.

- `PORT_CONFLICT_STRATEGY=auto`
  - if `8081` or `8888` is busy, the script picks the next free port
- `PORT_SEARCH_LIMIT=25`
  - how many next ports to try

The script prints the actual frontend and backend ports after startup, so you do not have to guess.

## Step-by-step

### Step 1. Prepare local env files

```bash
cp deploy/local/local-docker.env.example deploy/local/local-docker.env
cp deploy/local/local-docker.secrets.env.example deploy/local/local-docker.secrets.env
```

### Step 2. Adjust ports only if you want fixed values

Default local values:

- frontend host port: `8081`
- backend host port: `8888`
- port strategy: `auto`

If you want different preferred ports, edit:

- `FRONTEND_HOST_PORT`
- `BACKEND_HOST_PORT`

### Step 3. Run locally

```bash
chmod +x deploy/local/local-docker.sh
bash deploy/local/local-docker.sh
```

### Step 4. Test locally

Open the printed frontend URL in browser.

Typical default URLs:

- `http://localhost:8081/`
- `http://localhost:8081/healthz`
- `http://localhost:8081/api/actuator/health`

If port `8081` was busy, use the printed resolved frontend port instead.

### Step 5. Validate the application

Check:

1. home page loads
2. login works
3. product list loads
4. cart works
5. admin page loads
6. backend health returns `UP`

### Step 6. Cleanup

```bash
./deploy/local/local-docker-cleanup.sh
```
