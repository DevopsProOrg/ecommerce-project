# Cloth Shop Ecommerce

Cloth Shop is a Spring Boot + MySQL + static frontend ecommerce application with:

- customer registration and login
- admin-only product management
- database-backed cart operations
- local laptop development support
- Docker-oriented deployment with a frontend reverse proxy for `/api`

## Local development

### Backend

1. Start MySQL locally.
2. Create an `ecommerce` database if it does not already exist.
3. Optionally set bootstrap admin environment variables if you want the backend to create the initial admin account.
4. Run:

```powershell
cd backend
mvn spring-boot:run
```

The backend runs on `http://localhost:8888`.

### Frontend

Serve the `frontend` folder with a static server. The frontend reads `window.APP_CONFIG.API_BASE_URL` from `frontend/js/app-config.js` and defaults to:

- `http://localhost:8888/api` when opened directly from file
- `<current-origin>/api` when served from a web server or reverse proxy

## Admin bootstrap

Admin credentials are no longer shown in the UI.

To create the first admin account, pass environment variables to the backend:

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED="true"
$env:APP_BOOTSTRAP_ADMIN_EMAIL="admin@example.com"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD="ChangeThisImmediately123!"
$env:APP_BOOTSTRAP_ADMIN_NAME="Platform Admin"
```

Use bootstrap only to create the first admin in a controlled environment. After the account exists, disable it again.

## Docker deployment

1. Copy `.env.example` to `.env`
2. Set strong secrets and the initial admin bootstrap values
3. Build the backend jar:

```powershell
cd backend
mvn clean package
cd ..
docker compose up --build
```

The frontend container is exposed on `http://localhost:8080` and proxies `/api` to the backend container.

## Production direction

This structure is now friendlier for future platform work:

- Docker: frontend, backend, and MySQL containers
- Kubernetes: split frontend, backend, and database into separate workloads with ConfigMaps and Secrets
- Terraform: provision network, compute, registry, ingress, and secret stores
- Ansible: configure hosts, runtime dependencies, and deployments

Recommended next upgrades before internet exposure:

1. Replace role-by-user-id checks with JWT or secure session auth.
2. Add admin user management instead of bootstrap-only creation.
3. Move secrets to a real secret manager.
4. Add order, checkout, and payment flows.
5. Add health checks, CI/CD, observability, and backups.
