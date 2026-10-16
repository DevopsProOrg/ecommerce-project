# OG Fashions

This is an ecommerce application with a real DevOps deployment structure.

If you are deploying or configuring the platform, start here:

1. [deploy/START-HERE.md](deploy/START-HERE.md)

Do not read every README first. The repository has many focused README files because each DevOps area has its own reference. The main path is [deploy/START-HERE.md](deploy/START-HERE.md).

## What This Project Includes

- frontend static ecommerce site
- Spring Boot backend API
- MySQL database
- Docker deployment
- Kubernetes overlays for `l1`, `l2`, `l3`, `l6`, and `lp`
- Terraform AWS infrastructure
- Ansible server configuration
- Jenkins CI/CD
- Nexus and Artifactory publishing support
- Prometheus, Grafana, Loki, Promtail, and Alertmanager
- retained alert incident history in the admin UI

## Quick Local Validation

Backend:

```bash
cd backend
mvn test
cd ..
```

Frontend syntax:

```bash
node --check frontend/js/admin.js
node --check frontend/js/auth.js
node --check frontend/js/app-config.js
node --check frontend/js/app.js
node --check frontend/js/products.js
node --check frontend/js/cart.js
node --check frontend/js/ui.js
node --check frontend/js/wishlist.js
```

Local Docker:

```bash
cp deploy/local/local-docker.env.example deploy/local/local-docker.env
cp deploy/local/local-docker.secrets.env.example deploy/local/local-docker.secrets.env
bash deploy/local/local-docker.sh
```

Open:

```text
http://localhost:8081/
http://localhost:8081/healthz
http://localhost:8081/api/actuator/health
```

## Environment Rule

- `l1`, `l2`, `l3`, `l6`: use server IP or load balancer address.
- `lp`: use `https://ogstyles.store`.

## Local To Production

Use [deploy/START-HERE.md](deploy/START-HERE.md) as the canonical runbook. The short flow is: validate backend and frontend locally, run the full local Docker stack, deploy to `l1`, promote that same immutable Jenkins image through `l2`, `l3`, and `l6`, then deploy to `lp` only after production approval and a change ticket. Select one environment per Jenkins run. The runbook contains the per-stage commands, secrets, validation, and rollback steps.

## Image Tag Rule

Jenkins creates immutable build tags automatically (`build-<build-number>-<commit>`). Select the target environment for each run; use the generated tag in `ROLLBACK_IMAGE_TAG` when promoting that exact build to another environment.
