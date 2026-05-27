# End-to-End Step-by-Step Deployment Guide

This document is the single step-by-step guide for this project.

Use it when you want to know:

- what to do first
- what to configure next
- what files to edit
- what commands to run
- how lower levels differ from production
- how Docker, Kubernetes, Terraform, Ansible, Jenkins, monitoring, logging, alerts, and rollback all connect

## 1. Environment model

This project uses these environments:

1. `local`
   your laptop or workstation
2. `l1`
   lower dev level
3. `l2`
   lower testing level
4. `l3`
   higher testing or shared QA level
5. `l6`
   pre-production or staging
6. `lp`
   production

Usage model:

1. `l1`, `l2`, `l3`
   use IP address, EC2 public IP, Elastic IP, or load balancer address
2. `l6`
   use IP or load balancer address for staging-style validation
3. `lp`
   use the permanent domain `https://ogstyles.store`

## 2. Global prerequisites

Do these once before any deployment work.

### Step 1. Install workstation tools

Windows, Linux, or Ubuntu examples:

1. Git
2. Java 17+
3. Maven
4. Node.js
5. Docker
6. Terraform
7. Ansible
8. `kubectl`
9. `bash`

### Step 2. Clone the project

```bash
git clone https://github.com/DevopsProOrg/ecommerce-project.git
cd ecommerce-project
```

### Step 3. Read the docs in this order

1. [README.md](/E:/Restart/Projects/ecommerce-project/README.md)
2. [deploy/README.md](/E:/Restart/Projects/ecommerce-project/deploy/README.md)
3. [deploy/END-TO-END-STEP-BY-STEP.md](/E:/Restart/Projects/ecommerce-project/deploy/END-TO-END-STEP-BY-STEP.md)
4. [deploy/REALTIME-DEVOPS-IMPLEMENTATION.md](/E:/Restart/Projects/ecommerce-project/deploy/REALTIME-DEVOPS-IMPLEMENTATION.md)

## 3. Common configuration inventory

These are the most important values you will configure across environments.

### Application values

Examples:

- `APP_NAME=ogfashions`
- `APP_ENV=l1` or `APP_ENV=production`
- `APP_PUBLIC_BASE_URL=http://44.208.191.131` for lower levels
- `APP_PUBLIC_BASE_URL=https://ogstyles.store` for production

### Database values

Examples:

- `MYSQL_DATABASE=ecommerce`
- `SPRING_DATASOURCE_USERNAME=root`
- `SPRING_DATASOURCE_PASSWORD=<real-password>`
- `USE_EXTERNAL_DB=false` for container MySQL
- `USE_EXTERNAL_DB=true` for RDS
- `SPRING_DATASOURCE_URL=jdbc:mysql://<rds-endpoint>:3306/ecommerce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`

### Mail values

Examples:

- `SPRING_MAIL_HOST=smtp.gmail.com`
- `SPRING_MAIL_PORT=587`
- `SPRING_MAIL_USERNAME=contact.ogfashions@gmail.com`
- `SPRING_MAIL_PASSWORD=<gmail-app-password>`

### Jenkins values

Examples:

- `EC2_SSH_HOST=44.208.191.131`
- `EC2_SSH_USER=ec2-user`
- `NOTIFY_EMAIL=contact.ogfashions@gmail.com`
- `DEPLOY_BASE_URL=https://ogstyles.store`

## 4. Local code validation

Do this before Docker, Terraform, Jenkins, or Kubernetes.

### Step 1. Validate backend

```bash
cd backend
mvn test
cd ..
```

### Step 2. Validate frontend JavaScript

```bash
node --check frontend/js/admin.js
node --check frontend/js/auth.js
node --check frontend/js/app-config.js
```

### Step 3. Fix failures before continuing

If local validation fails, do not move to Docker or CI/CD yet.

## 5. Local Docker deployment

Use this when you want the full application on your machine.

### Step 1. Create local env files

Main files:

1. [deploy/local/local-docker.env.example](/E:/Restart/Projects/ecommerce-project/deploy/local/local-docker.env.example)
2. [deploy/local/local-docker.secrets.env.example](/E:/Restart/Projects/ecommerce-project/deploy/local/local-docker.secrets.env.example)

Copy them:

```bash
cp deploy/local/local-docker.env.example deploy/local/local-docker.env
cp deploy/local/local-docker.secrets.env.example deploy/local/local-docker.secrets.env
```

### Step 2. Update local values

Examples:

- `APP_PUBLIC_BASE_URL=http://localhost:8081`
- `FRONTEND_HOST_PORT=8081`
- `BACKEND_HOST_PORT=8888`
- `PORT_CONFLICT_STRATEGY=auto`

### Step 3. Start local Docker deployment

Linux, Ubuntu, WSL, or Git Bash:

```bash
bash deploy/local/local-docker.sh
```

Windows PowerShell:

```powershell
./deploy/local/local-docker.ps1
```

### Step 4. Test in browser

1. `http://localhost:8081/`
2. `http://localhost:8081/healthz`
3. `http://localhost:8081/api/actuator/health`

## 6. AWS setup for Terraform

Do this before infrastructure creation.

### Step 1. Prepare AWS account access

You need:

1. AWS account
2. IAM user or role with Terraform permissions
3. AWS CLI configured

### Step 2. Create EC2 key pairs

Examples:

1. `ogstyles-l1-key`
2. `ogstyles-l6-key`
3. `ogstyles-lp-key`

### Step 3. Decide free-tier usage

Recommended for free tier:

1. use EC2 Docker for `l1` and `l2`
2. keep Kubernetes optional until needed
3. use RDS only where it adds the most value
4. avoid too many always-on servers at the same time

### Step 4. Prepare production DNS and certificate

For `lp` only:

1. Route53 hosted zone for `ogstyles.store`
2. ACM certificate for `ogstyles.store`
3. optionally include `www.ogstyles.store`

## 7. Terraform for lower levels

Use this for `l1`, `l2`, `l3`, or `l6`.

### Step 1. Open the environment folder

Examples:

```bash
cd deploy/terraform/environments/l1
```

or

```bash
cd deploy/terraform/environments/l6
```

### Step 2. Create `terraform.tfvars`

```bash
cp terraform.tfvars.example terraform.tfvars
```

### Step 3. Edit values

Examples:

- `key_pair_name = "ogstyles-l1-key"`
- `ssh_ingress_cidrs = ["203.0.113.10/32"]`
- `monitoring_ingress_cidrs = ["203.0.113.10/32"]`
- `create_rds = false`

### Step 4. Apply Terraform

```bash
terraform init
terraform plan
terraform apply
```

### Step 5. Save outputs

Keep:

1. public IP
2. app URL
3. ansible inventory output
4. RDS endpoint if enabled

## 8. Terraform for production `lp`

Use this for production only.

### Step 1. Open the production folder

```bash
cd deploy/terraform/environments/lp
```

### Step 2. Create `terraform.tfvars`

```bash
cp terraform.tfvars.example terraform.tfvars
```

### Step 3. Edit production values

Examples:

- `hosted_zone_id = "ZXXXXXXXXXXXXX"`
- `key_pair_name = "ogstyles-lp-key"`
- `alb_certificate_arn = "arn:aws:acm:ap-south-1:123456789012:certificate/replace-me"`
- `ssh_ingress_cidrs = ["203.0.113.10/32"]`
- `monitoring_ingress_cidrs = ["203.0.113.10/32"]`
- `create_rds = true`
- `db_password = "replace-with-real-db-password"`

### Step 4. Apply Terraform

```bash
terraform init
terraform plan
terraform apply
```

### Step 5. Save production outputs

Keep:

1. ALB DNS name
2. public app URL
3. RDS endpoint
4. generated datasource URL

## 9. Ansible server configuration

Use Ansible after Terraform.

### Step 1. Choose the right inventory

Examples:

1. [deploy/ansible/inventories/l1/hosts.yml](/E:/Restart/Projects/ecommerce-project/deploy/ansible/inventories/l1/hosts.yml)
2. [deploy/ansible/inventories/l6/hosts.yml](/E:/Restart/Projects/ecommerce-project/deploy/ansible/inventories/l6/hosts.yml)
3. [deploy/ansible/inventories/lp/hosts.yml](/E:/Restart/Projects/ecommerce-project/deploy/ansible/inventories/lp/hosts.yml)

### Step 2. Update inventory values

Examples:

- `ansible_host: 44.208.191.131`
- `app_public_base_url: http://44.208.191.131` for lower levels
- `app_public_base_url: https://ogstyles.store` for production

### Step 3. Run Ansible

```bash
ansible-playbook -i deploy/ansible/inventories/l1/hosts.yml deploy/ansible/site.yml
```

Change inventory path per level.

## 10. Docker deployment on EC2 or Ubuntu

Use Docker deployment when you want application containers on a server.

### Step 1. Create non-secret override file

Files:

1. [deploy/docker/docker.env.example](/E:/Restart/Projects/ecommerce-project/deploy/docker/docker.env.example)
2. [deploy/docker/docker.env.defaults](/E:/Restart/Projects/ecommerce-project/deploy/docker/docker.env.defaults)

Copy:

```bash
cp deploy/docker/docker.env.example deploy/docker/docker.env
```

### Step 2. Create secrets file

```bash
cp deploy/docker/docker.secrets.env.example deploy/docker/docker.secrets.env
```

### Step 3. Edit Docker env for lower levels

Examples:

- `APP_PUBLIC_BASE_URL=http://44.208.191.131`
- `FRONTEND_HOST_PORT=80`
- `BACKEND_HOST_PORT=8888`
- `PORT_CONFLICT_STRATEGY=fail`

### Step 4. Edit Docker env for production

Examples:

- `APP_PUBLIC_BASE_URL=https://ogstyles.store`
- `FRONTEND_HOST_PORT=80`
- `PORT_CONFLICT_STRATEGY=fail`

### Step 5. Choose database mode

Container MySQL:

- `USE_EXTERNAL_DB=false`

RDS:

- `USE_EXTERNAL_DB=true`
- `SPRING_DATASOURCE_URL=<terraform-output>`

### Step 6. Run preflight

```bash
bash deploy/scripts/preflight-check.sh docker
```

### Step 7. Deploy

```bash
bash deploy/docker/ec2-deploy.sh
```

### Step 8. Verify containers

```bash
docker ps
docker logs ogfashions-backend --tail 100
docker logs ogfashions-frontend --tail 100
```

If `USE_EXTERNAL_DB=false`, also run:

```bash
docker logs ogfashions-mysql --tail 100
```

### Step 9. Test application

Lower level examples:

1. `http://44.208.191.131/`
2. `http://44.208.191.131/healthz`
3. `http://44.208.191.131/api/actuator/health`

Production examples:

1. `https://ogstyles.store/`
2. `https://ogstyles.store/healthz`
3. `https://ogstyles.store/api/actuator/health`

## 11. Monitoring, logging, and alerts for Docker

Use this after the app works once.

### Step 1. Create alertmanager env file

```bash
cp deploy/monitoring/docker/alertmanager.env.example deploy/monitoring/docker/alertmanager.env
```

### Step 2. Edit mail routing values

Examples:

- `SMTP_SMARTHOST=smtp.gmail.com:587`
- `SMTP_FROM=alerts@ogstyles.store`
- `SMTP_AUTH_USERNAME=contact.ogfashions@gmail.com`
- `SMTP_AUTH_PASSWORD=<gmail-app-password>`
- `ALERT_EMAIL_TO=contact.ogfashions@gmail.com`

### Step 3. Start monitoring

```bash
bash deploy/monitoring/docker/run-monitoring.sh
```

### Step 4. Open tools

1. `http://localhost:9090` Prometheus
2. `http://localhost:3000` Grafana
3. `http://localhost:9093` Alertmanager
4. `http://localhost:3100` Loki

### Step 5. Validate observability

1. Prometheus scrapes `/actuator/prometheus`
2. Grafana shows dashboards
3. Loki receives logs
4. Alertmanager has active config

## 12. Kubernetes deployment

Use Kubernetes when you want overlay-based promotion.

### Step 1. Confirm cluster access

```bash
kubectl get nodes
```

### Step 2. Install ingress controller

Use ingress-nginx or your cluster ingress of choice.

### Step 3. Create application secrets

Use:

- [deploy/k8s/secrets/ogfashions-secrets.example.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/secrets/ogfashions-secrets.example.yaml)

Create a real secret manifest and apply it to the target namespace.

### Step 4. Run preflight

```bash
bash deploy/scripts/preflight-check.sh k8s
```

### Step 5. Deploy an overlay

Examples:

```bash
bash deploy/k8s/scripts/deploy.sh l1 yarrav7 release-42 http://<load-balancer-address>
```

```bash
bash deploy/k8s/scripts/deploy.sh lp yarrav7 release-42 https://ogstyles.store
```

### Step 6. Verify rollout

```bash
kubectl get all -n ogfashions-l1
kubectl logs deploy/ogfashions-backend -n ogfashions-l1
kubectl logs deploy/ogfashions-frontend -n ogfashions-l1
```

### Step 7. Browser validation

Lower levels:

1. use load balancer IP or ingress address

Production:

1. `https://ogstyles.store/`
2. `https://ogstyles.store/healthz`
3. `https://ogstyles.store/api/actuator/health`

## 13. Monitoring, logging, and alerts for Kubernetes

### Step 1. Apply monitoring stack

```bash
kubectl apply -k deploy/monitoring/k8s
```

### Step 2. Port-forward access

```bash
kubectl port-forward svc/prometheus 9090:9090 -n monitoring
kubectl port-forward svc/grafana 3000:3000 -n monitoring
kubectl port-forward svc/alertmanager 9093:9093 -n monitoring
kubectl port-forward svc/loki 3100:3100 -n monitoring
```

### Step 3. Replace alert mail placeholders

Update:

- [deploy/monitoring/k8s/alertmanager-configmap.yaml](/E:/Restart/Projects/ecommerce-project/deploy/monitoring/k8s/alertmanager-configmap.yaml)

Then reapply:

```bash
kubectl apply -k deploy/monitoring/k8s
```

### Step 4. Validate tools

1. Prometheus scrapes all environment backends
2. Grafana shows metrics and logs
3. Loki receives pod logs
4. Alertmanager has active routes

## 14. Jenkins CI/CD setup

Do this after manual deployment works at least once.

### Step 1. Install Jenkins plugins

Required:

1. Pipeline
2. Git
3. GitHub
4. Credentials Binding
5. SSH Agent
6. Workspace Cleanup
7. ANSI Color
8. Email Extension

### Step 2. Add Jenkins credentials

Examples:

1. `docker-registry-creds`
2. `ec2-ssh-key`
3. `kubeconfig-credential`
4. `sonar-token`

### Step 3. Add Jenkins environment values

Examples:

- `EC2_SSH_HOST=44.208.191.131`
- `EC2_SSH_USER=ec2-user`
- `NOTIFY_EMAIL=contact.ogfashions@gmail.com`

### Step 4. Configure SMTP in Jenkins

Use Gmail SMTP values or your mail provider.

### Step 5. Create multibranch pipeline

Point it at the GitHub repo and use the repo `Jenkinsfile`.

### Step 6. Configure GitHub webhook

Example:

```text
http://<jenkins-host>:8080/github-webhook/
```

## 15. Jenkins step-by-step deployment cases

### Case 1. Validation only

Use:

- `DEPLOY_TARGET=none`
- `DEPLOY_LEVEL=none`

### Case 2. Lower Docker deploy

Use:

- `DEPLOY_TARGET=docker`
- `DEPLOY_LEVEL=l1` or `l2`
- `DEPLOY_BASE_URL=http://<server-ip>`

### Case 3. Lower Kubernetes deploy

Use:

- `DEPLOY_TARGET=k8s`
- `DEPLOY_LEVEL=l6`
- `K8S_OVERLAY=l6`
- `DEPLOY_BASE_URL=http://<load-balancer-address>`

### Case 4. Production deploy

Use:

- `DEPLOY_TARGET=docker` or `k8s`
- `DEPLOY_LEVEL=lp`
- `DEPLOY_BASE_URL=https://ogstyles.store`
- `CHANGE_TICKET=CR-2026-051`

Jenkins will then:

1. build
2. push immutable images
3. wait for approval
4. record approver name
5. deploy
6. run smoke checks
7. send email

### Case 5. Rollback by image tag

Use:

- `ROLLBACK_IMAGE_TAG=release-41`

Keep deploy target and level set to the environment you want to restore.

## 16. Smoke testing

Use after every deployment.

### Step 1. Run smoke script

```bash
bash deploy/scripts/http-smoke.sh https://ogstyles.store
```

Lower level example:

```bash
bash deploy/scripts/http-smoke.sh http://44.208.191.131
```

### Step 2. Confirm these pass

1. `/`
2. `/healthz`
3. `/api/actuator/health`

## 17. Rollback cases

### Docker rollback

```bash
bash deploy/docker/ec2-rollback.sh deploy/docker/docker.env release-41
```

### Kubernetes rollback to previous revision

```bash
bash deploy/k8s/scripts/rollback.sh l6
```

### Kubernetes rollback to known image

```bash
bash deploy/k8s/scripts/rollback.sh l6 yarrav7 release-41 http://<load-balancer-address>
```

### Jenkins rollback

Use:

- `ROLLBACK_IMAGE_TAG=release-41`

## 18. Production cutover order

Use this exact order for real releases:

1. validate locally
2. validate with Docker
3. deploy to `l1`
4. test
5. promote same image tag to `l2`
6. test
7. promote same image tag to `l3`
8. test
9. promote same image tag to `l6`
10. test
11. review metrics, logs, and alerts
12. approve `lp`
13. deploy `lp`
14. run browser tests
15. watch Grafana and Alertmanager

## 19. Daily DevOps operating loop

Recommended daily process:

1. check Jenkins build history
2. check latest deployed image tag
3. review Grafana dashboards
4. review Loki logs
5. check Alertmanager notifications
6. validate lower levels before production promotion
7. keep previous known-good image tags ready

## 20. Best next operating documents

After this guide, use these for day-to-day depth:

1. [deploy/DEVOPS-OPERATIONS-HANDBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/DEVOPS-OPERATIONS-HANDBOOK.md)
2. [deploy/ORG-PRODUCTION-PLAYBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/ORG-PRODUCTION-PLAYBOOK.md)
3. [deploy/ENVIRONMENT-MATRIX.md](/E:/Restart/Projects/ecommerce-project/deploy/ENVIRONMENT-MATRIX.md)
