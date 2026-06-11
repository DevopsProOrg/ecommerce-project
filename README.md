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

## Image Tag Rule

Use version tags such as `v1.0`, `v1.1`, and `v1.2`. Do not use `latest` for real environment promotion.
