# OG Fashions End-to-End DevOps Runbook

This is the only main runbook. Start here every time.

Use the folder README files only when this runbook tells you to open them. This keeps the process clear and avoids jumping between too many documents.

## 1. Final Deployment Order

Follow this order exactly:

1. Local code validation
2. Local Docker validation
3. EC2 or Ubuntu Docker deployment for `l1`
4. Monitoring, Grafana, Alertmanager, and email alerts
5. Terraform infrastructure
6. Ansible server configuration
7. Jenkins CI/CD setup
8. Maven, SonarQube, Trivy, Nexus, and Artifactory validation
9. Deploy to `l1` and validate
10. Promote the same immutable image one environment per run through `l2`, `l3`, and `l6`
11. Complete production readiness checks and promote that same image to `lp` after approval
12. Run post-deployment smoke checks, monitoring checks, alert checks, and verify rollback readiness

Docker/EC2 and Kubernetes are deployment targets for the same environment levels; choose the target that matches the provisioned infrastructure. The pipeline deploys one selected level per run. Kubernetes is an alternative deployment target, not a required extra production rollout after a successful Docker rollout.

## 2. Environment Levels

| Level | Purpose | URL |
| --- | --- | --- |
| `local` | laptop or local Docker testing | `http://localhost:8081` |
| `l1` | dev validation | `http://<server-ip>` or load balancer URL |
| `l2` | QA/testing | `http://<server-ip>` or load balancer URL |
| `l3` | integration/UAT | `http://<server-ip>` or load balancer URL |
| `l6` | staging/pre-production | `http://<server-ip>` or load balancer URL |
| `lp` | production | `https://ogstyles.store` |

Use IP or load balancer URLs for `l1`, `l2`, `l3`, and `l6`. Use the real domain only for `lp`.

## 3. Build And Promotion Rule

Jenkins generates a unique image tag from its build number and the source commit. No release version input is required. Each Jenkins run targets the selected environment. To promote the exact image built by an earlier run, set `ROLLBACK_IMAGE_TAG` to that build's tag; Jenkins reuses the published image and skips building it again. Production deployments still require a change ticket and approval.

## Application Security Prerequisites

- Generate `APP_SECURITY_JWT_SECRET` with `openssl rand -hex 32`; keep the same secret on every backend replica in one environment and store it in the platform secret store.
- Set `SPRING_DATASOURCE_USERNAME=ecommerce_app`. Use a separate random `SPRING_DATASOURCE_PASSWORD`; keep the MySQL root password only for database administration.
- For the first startup only, enable `APP_BOOTSTRAP_ADMIN_ENABLED` and set a unique admin password of at least 12 characters; disable bootstrap after the admin account exists.
- New MySQL volumes create the application account from `MYSQL_USER` and `MYSQL_PASSWORD`. Existing Docker volumes or Kubernetes PVCs do not replay MySQL initialization variables; create/grant the `ecommerce_app` user on those databases before upgrading.
- User registration requires a 12 to 128 character password. Existing SHA-256 password hashes are upgraded to BCrypt after successful login.
- The API uses 15 minute bearer tokens held in the browser tab session. Users must sign in again after the token expires or the tab session ends.
- Keep CORS origins empty for the normal same-origin frontend/API deployment. Add only exact trusted origins when a separate frontend host is required; do not use cloud-provider wildcard origins.

## 4. Files You Actually Need

Main runbook:

- [START-HERE.md](./START-HERE.md)

Focused references:

- [local/README.md](./local/README.md) for local Docker
- [docker/README.md](./docker/README.md) for EC2 or Ubuntu Docker
- [terraform/README.md](./terraform/README.md) for AWS infrastructure
- [ansible/README.md](./ansible/README.md) for server configuration
- [jenkins/README.md](./jenkins/README.md) for Jenkins setup
- [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md) for pipeline stages
- [k8s/README.md](./k8s/README.md) for Kubernetes
- [monitoring/README.md](./monitoring/README.md) for Prometheus, Grafana, Loki, Promtail, Alertmanager, and email alerts
- [artifact-repositories/README.md](./artifact-repositories/README.md) for Nexus and Artifactory

## 5. Stage 1 - Local Code Validation

Use this before Docker, EC2, Jenkins, or Kubernetes.

Install required tools on Ubuntu or Linux:

```bash
sudo apt update
sudo apt install -y git curl unzip ca-certificates gnupg openjdk-17-jdk maven nodejs npm
```

Clone:

```bash
git clone https://github.com/DevopsProOrg/ecommerce-project.git
cd ecommerce-project
```

Validate backend:

```bash
cd backend
mvn test
cd ..
```

Validate frontend:

```bash
node --check frontend/js/admin.js
node --check frontend/js/auth.js
node --check frontend/js/app-config.js
```

Expected result:

```text
Backend tests pass.
Frontend syntax checks pass.
No merge conflicts are present.
```

## 6. Stage 2 - Local Docker Validation

Use this to test the full app locally.

Create env files:

```bash
cp deploy/local/local-docker.env.example deploy/local/local-docker.env
cp deploy/local/local-docker.secrets.env.example deploy/local/local-docker.secrets.env
```

Edit `deploy/local/local-docker.env`:

```text
APP_ENV=local
APP_PUBLIC_BASE_URL=http://localhost:8081
FRONTEND_HOST_PORT=8081
BACKEND_HOST_PORT=8888
PORT_CONFLICT_STRATEGY=auto
IMAGE_TAG=<local-tag>
BUILD_IMAGES_LOCALLY=true
```

Edit `deploy/local/local-docker.secrets.env`:

```text
MYSQL_ROOT_PASSWORD=<local-mysql-password>
SPRING_DATASOURCE_PASSWORD=<local-mysql-password>
APP_BOOTSTRAP_ADMIN_PASSWORD=<local-admin-password>
APP_SECURITY_JWT_SECRET=<unique-random-secret-at-least-32-bytes>
SPRING_MAIL_PASSWORD=<gmail-app-password-or-local-placeholder>
```

Start:

```bash
bash deploy/local/local-docker.sh
```

Test:

```text
http://localhost:8081/
http://localhost:8081/healthz
http://localhost:8081/api/actuator/health
```

Check containers:

```bash
docker ps
docker logs ogfashions-backend --tail 100
docker logs ogfashions-frontend --tail 100
docker logs ogfashions-mysql --tail 100
```

## 7. Stage 3 - EC2 Or Ubuntu Docker Deployment

Use this first for `l1`. After it works, use the same pattern for `l2`, `l3`, `l6`, and simple `lp`.

On the target Ubuntu server:

```bash
sudo apt update
sudo apt install -y git curl
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"
```

Log out and log back in.

Clone:

```bash
sudo mkdir -p /opt
sudo chown "$USER:$USER" /opt
git clone https://github.com/DevopsProOrg/ecommerce-project.git /opt/ogfashions
cd /opt/ogfashions
```

Create env files:

```bash
cp deploy/docker/docker.env.example deploy/docker/docker.env
cp deploy/docker/docker.secrets.env.example deploy/docker/docker.secrets.env
```

For lower levels, edit `deploy/docker/docker.env`:

```text
APP_NAME=ogfashions
APP_ENV=l1
APP_PUBLIC_BASE_URL=http://<server-public-ip>
USE_EXTERNAL_DB=false
FRONTEND_HOST_PORT=80
BACKEND_HOST_PORT=8888
PORT_CONFLICT_STRATEGY=auto
IMAGE_TAG=<local-tag>
BUILD_IMAGES_LOCALLY=true
```

For production Docker, edit:

```text
APP_ENV=production
APP_PUBLIC_BASE_URL=https://ogstyles.store
FRONTEND_HOST_PORT=80
BACKEND_HOST_PORT=8888
PORT_CONFLICT_STRATEGY=auto
IMAGE_TAG=<local-tag>
```

Edit `deploy/docker/docker.secrets.env`:

```text
MYSQL_ROOT_PASSWORD=<real-mysql-root-password>
SPRING_DATASOURCE_PASSWORD=<same-or-real-db-password>
APP_BOOTSTRAP_ADMIN_PASSWORD=<real-admin-password>
SPRING_MAIL_PASSWORD=<gmail-app-password>
```

Deploy:

```bash
bash deploy/docker/ec2-deploy.sh
```

Smoke test:

```bash
bash deploy/scripts/http-smoke.sh http://<server-public-ip>
```

Production smoke test:

```bash
bash deploy/scripts/http-smoke.sh https://ogstyles.store
```

## 8. Stage 4 - Monitoring, Grafana, Alertmanager, And Email Alerts

Run this after the application is working on Docker.

Create alert email config:

```bash
cp deploy/monitoring/docker/alertmanager.env.example deploy/monitoring/docker/alertmanager.env
```

Edit `deploy/monitoring/docker/alertmanager.env`:

```text
SMTP_SMARTHOST=smtp.gmail.com:587
SMTP_FROM=alerts@ogstyles.store
SMTP_AUTH_USERNAME=contact.ogfashions@gmail.com
SMTP_AUTH_PASSWORD=<gmail-app-password>
ALERT_EMAIL_TO=contact.ogfashions@gmail.com
ALERT_WEBHOOK_URL=http://ogfashions-backend:8888/api/monitoring/alerts/webhook
```

Start monitoring:

```bash
bash deploy/monitoring/docker/run-monitoring.sh
```

Open:

```text
Prometheus: http://<server-ip>:9090
Grafana: http://<server-ip>:3000
Alertmanager: http://<server-ip>:9093
Loki: http://<server-ip>:3100
```

Validate:

```text
Prometheus -> Status -> Targets -> ogfashions-backend is UP
Grafana -> OG Fashions dashboard loads
Alertmanager -> configuration is loaded
Admin UI -> Monitoring incidents section stores firing/resolved alerts
Email -> alert email arrives when an alert fires
Email -> resolved email arrives when alert resolves
```

Important behavior:

- Prometheus shows current alert state.
- Alertmanager shows current alert groups.
- The application stores alert history for review after alerts resolve.

## 9. Stage 5 - Terraform AWS Infrastructure

Use Terraform when you want repeatable AWS resources instead of manual EC2 creation.

Configure AWS CLI:

```bash
aws configure
```

For `l1`:

```bash
cd deploy/terraform/environments/l1
cp terraform.tfvars.example terraform.tfvars
vi terraform.tfvars
terraform init
terraform plan
terraform apply
```

Important `terraform.tfvars` values:

```text
aws_region = "us-east-1"
environment = "l1"
key_pair_name = "<your-aws-keypair>"
ssh_ingress_cidrs = ["<your-public-ip>/32"]
monitoring_ingress_cidrs = ["<your-public-ip>/32"]
create_rds = false
```

For `lp`, configure production values:

```text
environment = "lp"
domain_name = "ogstyles.store"
create_rds = true
db_password = "<real-rds-password>"
```

After apply, save:

```text
public IP
load balancer DNS
RDS endpoint if created
```

## 10. Stage 6 - Ansible Server Configuration

Use Ansible after Terraform creates servers.

Update inventory:

```bash
vi deploy/ansible/inventories/l1/hosts.yml
```

Example:

```yaml
all:
  hosts:
    ogfashions-l1:
      ansible_host: <server-public-ip>
      ansible_user: ubuntu
```

Run:

```bash
ansible-playbook -i deploy/ansible/inventories/l1/hosts.yml deploy/ansible/playbooks/site.yml
```

Expected result:

```text
Docker is installed.
App directory exists.
Deployment env files are prepared.
Node exporter is installed if enabled.
```

## 11. Stage 7 - Jenkins CI/CD Setup

Use Jenkins after manual Docker deployment works.

Install Jenkins plugins:

```text
Pipeline
Git
GitHub
Credentials Binding
SSH Agent
Workspace Cleanup
ANSI Color
Email Extension
```

Optional tools:

```text
SonarQube Scanner
Trivy
Docker CLI
kubectl
Maven
Node.js
```

Create Jenkins credentials:

```text
docker-registry-creds      username/password for Docker Hub or registry
ec2-ssh-key                SSH private key for Ubuntu/EC2
kubeconfig-credential      kubeconfig file for Kubernetes
sonar-token                SonarQube token
nexus-creds                Nexus username/password
artifactory-creds          Artifactory username/token
```

Configure Jenkins SMTP:

```text
SMTP server: smtp.gmail.com
SMTP port: 587
SMTP auth: enabled
SMTP username: contact.ogfashions@gmail.com
SMTP password: Gmail app password
TLS: enabled
```

Create multibranch pipeline:

```text
Repository: https://github.com/DevopsProOrg/ecommerce-project.git
Script path: Jenkinsfile
Webhook: http://<jenkins-host>:8080/github-webhook/
```

## 12. Stage 8 - Maven, SonarQube, Trivy, Nexus, And Artifactory

Jenkins validates these stages:

```text
Maven compile
Maven test
Maven package
Frontend node --check
SonarQube scan if configured
Trivy filesystem scan (required when `RUN_TRIVY=true`; default is true)
Docker image build
Trivy image scan (required when `RUN_TRIVY=true`; default is true)
Optional Nexus publish
Optional Artifactory publish
Docker image push
Deployment
Smoke check
Email notification
```

Important Jenkins parameters:

```text
RUN_SONAR=true
RUN_TRIVY=true
ARTIFACT_REPOSITORY=none or nexus or artifactory
NOTIFY_EMAIL=contact.ogfashions@gmail.com
```

For Nexus:

```text
ARTIFACT_REPOSITORY=nexus
NEXUS_MAVEN_REPO_URL=http://<nexus-host>:8081/repository/maven-releases/
```

For Artifactory:

```text
ARTIFACT_REPOSITORY=artifactory
ARTIFACTORY_MAVEN_REPO_URL=https://<artifactory-host>/artifactory/libs-release-local/
```

## 13. Stage 9 - Jenkins Deploy To Lower Levels

For `l1` Docker deployment:

```text
DEPLOY_TARGET=docker
DEPLOY_LEVEL=l1
K8S_OVERLAY=l1
EC2_SSH_HOST=<l1-server-public-ip>
EC2_SSH_USER=ubuntu
EC2_SSH_CREDENTIALS_ID=ec2-ssh-key
REMOTE_APP_DIR=/opt/ogfashions
DOCKER_ENV_REMOTE_PATH=/opt/ogfashions/deploy/docker/docker.env
DEPLOY_BASE_URL=http://<l1-server-public-ip>
CONTAINER_REGISTRY=docker.io
CONTAINER_IMAGE_REPO=yarrav7
NOTIFY_EMAIL=contact.ogfashions@gmail.com
```

Promote to `l2`:

```text
DEPLOY_LEVEL=l2
ROLLBACK_IMAGE_TAG=<tag-from-l1-build>
EC2_SSH_HOST=<l2-server-public-ip>
DEPLOY_BASE_URL=http://<l2-server-public-ip>
```

Promote to `l3`:

```text
DEPLOY_LEVEL=l3
ROLLBACK_IMAGE_TAG=<tag-from-l1-build>
EC2_SSH_HOST=<l3-server-public-ip>
DEPLOY_BASE_URL=http://<l3-server-public-ip>
```

Promote to `l6`:

```text
DEPLOY_LEVEL=l6
ROLLBACK_IMAGE_TAG=<tag-from-l1-build>
EC2_SSH_HOST=<l6-server-public-ip>
DEPLOY_BASE_URL=http://<l6-server-public-ip>
```

## 14. Stage 10 - Kubernetes Lower-Level Deployment

Use Kubernetes after Docker deployments are stable.

Confirm cluster:

```bash
kubectl get nodes
```

Create secrets:

```bash
cp deploy/k8s/secrets/ogfashions-secrets.example.yaml deploy/k8s/secrets/ogfashions-secrets.yaml
vi deploy/k8s/secrets/ogfashions-secrets.yaml
kubectl apply -f deploy/k8s/secrets/ogfashions-secrets.yaml
```

Create the selected environment namespace before applying its Secret (for example, `kubectl create namespace ogfashions-l1`), and set `metadata.namespace` in the Secret manifest to that same namespace. Repeat with the target level for each environment; never reuse production credentials in lower levels. The overlay also declares the namespace, so `deploy.sh` will apply the matching namespace and workloads.

Deploy `l1`:

```bash
bash deploy/k8s/scripts/deploy.sh l1 yarrav7 <image-tag> http://<load-balancer-address>
```

Deploy `l6`:

```bash
bash deploy/k8s/scripts/deploy.sh l6 yarrav7 <image-tag> http://<load-balancer-address>
```

Validate:

```bash
kubectl get all -n ogfashions-l1
kubectl logs deploy/ogfashions-backend -n ogfashions-l1 --tail=100
kubectl logs deploy/ogfashions-frontend -n ogfashions-l1 --tail=100
```

## 15. Stage 11 - Kubernetes Production Deployment

Production uses `lp` and `https://ogstyles.store`.

Confirm DNS points to production load balancer before final cutover.

Deploy:

```bash
bash deploy/k8s/scripts/deploy.sh lp yarrav7 <image-tag> https://ogstyles.store
```

Jenkins production parameters:

```text
DEPLOY_TARGET=k8s
DEPLOY_LEVEL=lp
K8S_OVERLAY=lp
ROLLBACK_IMAGE_TAG=<tag-from-l6-build>
CHANGE_TICKET=CHG-001
DEPLOY_BASE_URL=https://ogstyles.store
```

Production requires manual Jenkins approval.

## 16. Stage 12 - Production Live Checklist

Before production:

```text
l6 is tested
same image tag is used
Docker image exists in registry
Maven artifact exists in Nexus or Artifactory if enabled
Jenkins email works
Alertmanager email works
Grafana dashboard works
Smoke checks pass
DNS for ogstyles.store points to production
TLS certificate is valid
Rollback tag is known
```

Deploy production:

```text
DEPLOY_LEVEL=lp
DEPLOY_BASE_URL=https://ogstyles.store
CHANGE_TICKET=<real-change-id>
```

Post-deploy smoke:

```bash
bash deploy/scripts/http-smoke.sh https://ogstyles.store
```

Browser checks:

```text
https://ogstyles.store/
https://ogstyles.store/healthz
https://ogstyles.store/api/actuator/health
https://ogstyles.store/admin.html
```

Business checks:

```text
Login works
Product list loads
Cart works
Checkout/order flow works
Admin page works
Monitoring incidents page works
Email sending works
```

## 17. Stage 13 - Post-Deployment Health Checks

Docker:

```bash
docker ps
docker logs ogfashions-backend --tail 100
docker logs ogfashions-frontend --tail 100
docker logs ogfashions-mysql --tail 100
```

Kubernetes:

```bash
kubectl get pods -n ogfashions-lp
kubectl rollout status deployment/ogfashions-backend -n ogfashions-lp
kubectl rollout status deployment/ogfashions-frontend -n ogfashions-lp
```

Monitoring:

```text
Prometheus targets are UP
Grafana dashboard shows traffic and health
Alertmanager has no unexpected firing alerts
Admin monitoring incidents show resolved/firing history
Loki logs are searchable
```

Email:

```text
Jenkins sends build success/failure email
Alertmanager sends firing email
Alertmanager sends resolved email
Application mail sends order/support email if enabled
```

## 18. Rollback

Docker rollback:

```bash
bash deploy/docker/ec2-rollback.sh deploy/docker/docker.env <known-good-image-tag>
```

Kubernetes rollback to previous revision:

```bash
bash deploy/k8s/scripts/rollback.sh lp
```

Kubernetes rollback to known image:

```bash
bash deploy/k8s/scripts/rollback.sh lp yarrav7 <known-good-image-tag> https://ogstyles.store
```

Jenkins rollback:

```text
ROLLBACK_IMAGE_TAG=<known-good-image-tag>
DEPLOY_TARGET=docker or k8s
DEPLOY_LEVEL=lp
CHANGE_TICKET=CHG-rollback-001
```

## 19. Daily DevOps Workflow

Use this every day:

1. Check latest Jenkins build.
2. Check deployed image tag.
3. Check Docker registry images.
4. Check Nexus or Artifactory artifact if enabled.
5. Open app URL.
6. Run smoke test.
7. Check Prometheus targets.
8. Check Grafana dashboard.
9. Check Alertmanager.
10. Check admin monitoring incidents.
11. Review Loki logs.
12. Fix issues in Git.
13. Let Jenkins generate the immutable build tag for the new change.
14. Promote that exact tag through environments again.

## 20. Common Problems And Fixes

Port conflict:

```text
Set PORT_CONFLICT_STRATEGY=auto
Use frontend 8081 locally because Jenkins uses 8080
Use frontend 80 on EC2
```

Docker push denied:

```text
Check docker-registry-creds
Check CONTAINER_IMAGE_REPO=yarrav7
Check repository exists in Docker Hub or registry
```

EC2 deploy permission denied:

```text
Run script through bash
Ensure Jenkins deploy stage runs chmod +x deploy/docker/*.sh deploy/scripts/*.sh
Ensure EC2 user owns /opt/ogfashions
```

Prometheus alert disappears:

```text
That is normal after resolve.
Use admin monitoring incidents for retained history.
```

No alert email:

```text
Check deploy/monitoring/docker/alertmanager.env
Check Gmail app password
Check Alertmanager logs
Check SMTP TLS settings
```

Maven package fails locally on Windows:

```text
Close VS Code Java/SonarLint processes or build on Ubuntu/Jenkins.
This project deployment target is Ubuntu/Linux.
```

## 21. Do Not Do These

- Do not commit real passwords.
- Do not use `latest` for real deployments.
- Do not use production domain for lower levels.
- Do not deploy production without `CHANGE_TICKET`.
- Do not skip smoke tests.
- Do not ignore resolved alerts; review alert history in the admin UI.
- Do not rebuild a new image when promoting from one level to the next.
