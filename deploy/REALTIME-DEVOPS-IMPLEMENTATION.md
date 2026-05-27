# Real-Time DevOps Implementation Guide

This guide gives you the practical order to run this project as a DevOps engineer from local validation to production operations.

Use this order when you want the least friction:

1. validate code locally
2. validate with local Docker
3. provision AWS infrastructure with Terraform
4. configure servers with Ansible
5. deploy lower levels
6. add monitoring, logging, and alerts
7. wire Jenkins CI/CD
8. promote the same image to `l6`
9. approve and deploy `lp`
10. keep rollback ready

## 1. Local code validation

Run these first on your workstation:

```bash
cd backend
mvn test
cd ..
node --check frontend/js/admin.js
node --check frontend/js/auth.js
node --check frontend/js/app-config.js
```

Use this before any Docker, Kubernetes, or Jenkins activity.

## 2. Local Docker validation

Use this when you want the full app locally without touching AWS.

### Step 1. Create local overrides

Examples:

- `APP_PUBLIC_BASE_URL=http://localhost:8081`
- `FRONTEND_HOST_PORT=8081`
- `BACKEND_HOST_PORT=8888`
- `PORT_CONFLICT_STRATEGY=auto`

### Step 2. Start the app

```bash
bash deploy/local/local-docker.sh
```

### Step 3. Open the app

Examples:

- `http://localhost:8081/`
- `http://localhost:8081/healthz`
- `http://localhost:8081/api/actuator/health`

## 3. AWS free-tier infrastructure

Recommended free-tier-first model:

- `l1` or `l2`: EC2 Docker host
- `l3` or `l6`: optional small Kubernetes or EC2-based test environment
- `lp`: EC2 + ALB + Route53 + optional RDS

### Step 1. Prepare AWS

Create or confirm:

1. IAM user or role with Terraform permissions
2. AWS CLI credentials
3. key pair [example: `ogstyles-lp-key`]
4. Route53 hosted zone for `ogstyles.store`
5. ACM certificate for `ogstyles.store` and optionally `www.ogstyles.store`

### Step 2. Choose the Terraform environment

Examples:

- `deploy/terraform/environments/l1`
- `deploy/terraform/environments/l6`
- `deploy/terraform/environments/lp`

### Step 3. Fill `terraform.tfvars`

Examples:

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

### Step 5. Save the outputs

Keep:

- app host public IP
- ALB DNS name
- app URL
- RDS endpoint if created
- generated inventory snippet

## 4. Ansible server configuration

Use Ansible after Terraform creates hosts.

### Step 1. Update inventory

Examples:

- `ansible_host: 44.208.191.131`
- `app_public_base_url: http://44.208.191.131` for `l1`
- `app_public_base_url: https://ogstyles.store` for `lp`

### Step 2. Run the playbook

```bash
ansible-playbook -i deploy/ansible/inventories/l1/hosts.yml deploy/ansible/site.yml
```

That should prepare the server for Docker, checkout, and app deployment.

## 5. Docker deployment on Ubuntu or EC2

Use Docker deployment for:

- local validation
- lower levels on EC2
- production on EC2 if you are not using Kubernetes for `lp`

### Step 1. Create `docker.env`

Examples:

- lower level: `APP_PUBLIC_BASE_URL=http://44.208.191.131`
- production: `APP_PUBLIC_BASE_URL=https://ogstyles.store`
- production safe ports: `FRONTEND_HOST_PORT=80`
- shared host fallback: `PORT_CONFLICT_STRATEGY=auto`

### Step 2. Choose database mode

Containerized MySQL:

- `USE_EXTERNAL_DB=false`

Managed RDS:

- `USE_EXTERNAL_DB=true`
- `SPRING_DATASOURCE_URL=jdbc:mysql://ogstyles-lp-mysql.xxxxxx.ap-south-1.rds.amazonaws.com:3306/ecommerce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`

### Step 3. Deploy

```bash
bash deploy/docker/ec2-deploy.sh
```

### Step 4. Roll back if needed

```bash
bash deploy/docker/ec2-rollback.sh deploy/docker/docker.env release-42
```

## 6. Monitoring, logging, and alerting

This project now supports:

- Prometheus
- Grafana
- Loki
- Promtail
- Alertmanager

### Docker observability

```bash
bash deploy/monitoring/docker/run-monitoring.sh
```

Optional email alert file:

```bash
cp deploy/monitoring/docker/alertmanager.env.example deploy/monitoring/docker/alertmanager.env
```

### Kubernetes observability

```bash
kubectl apply -k deploy/monitoring/k8s
```

### What to verify

1. Prometheus is scraping `/actuator/prometheus`
2. Grafana shows backend panels
3. Loki receives logs
4. Alertmanager can route mail after SMTP values are updated

## 7. Jenkins CI/CD

Use Jenkins after manual Docker deployment works once.

### Step 1. Configure credentials

Required examples:

- `docker-registry-creds`
- `ec2-ssh-key`
- `kubeconfig-credential`
- `sonar-token`

### Step 2. Configure environment values

Examples:

- `EC2_SSH_HOST=44.208.191.131`
- `EC2_SSH_USER=ec2-user`
- `NOTIFY_EMAIL=contact.ogfashions@gmail.com`

### Step 3. Use deployment parameters

Examples:

- validation only: `DEPLOY_TARGET=none`
- lower Docker: `DEPLOY_TARGET=docker`, `DEPLOY_LEVEL=l1`
- lower K8s: `DEPLOY_TARGET=k8s`, `DEPLOY_LEVEL=l6`, `K8S_OVERLAY=l6`
- production: `DEPLOY_TARGET=docker`, `DEPLOY_LEVEL=lp`
- rollback: `ROLLBACK_IMAGE_TAG=release-42`

### Step 4. Keep the evidence

Each build now produces:

- release notes
- deployment summary
- approval identity for production
- email result notification

## 8. Kubernetes deployment flow

Use Kubernetes when you want promotion by overlay.

### Step 1. Prepare secrets

Create secrets from:

- `deploy/k8s/secrets/ogfashions-secrets.example.yaml`

### Step 2. Deploy an overlay

```bash
bash deploy/k8s/scripts/deploy.sh l6 yarrav7 release-42 http://<load-balancer-address>
```

### Step 3. Roll back if needed

Use previous rollout:

```bash
bash deploy/k8s/scripts/rollback.sh l6
```

Use a specific image tag:

```bash
bash deploy/k8s/scripts/rollback.sh l6 yarrav7 release-41 http://<load-balancer-address>
```

## 9. Production flow for `lp`

Recommended sequence:

1. deploy the same image to `l6`
2. validate smoke checks
3. review metrics and logs
4. approve `lp` in Jenkins
5. deploy the same immutable image tag to production
6. validate browser and API health

Production access examples:

- `https://ogstyles.store/`
- `https://ogstyles.store/healthz`
- `https://ogstyles.store/api/actuator/health`

## 10. Daily DevOps routine

Recommended daily loop:

1. check Jenkins build status
2. review Grafana dashboards
3. review Loki logs for failed requests
4. review Alertmanager mail
5. check current deployed image tag
6. promote or roll back safely

## 11. Free-tier recommendations

To stay closer to AWS free tier:

1. keep only one always-on lower environment at a time
2. use `db.t3.micro` for RDS when you enable it
3. avoid EKS in early phases if cost matters
4. prefer EC2 Docker for `l1` and `l2`
5. use Kubernetes mainly when you specifically want to demonstrate cluster delivery

## 12. What to improve next

After this baseline, next high-value upgrades are:

1. Helm charts
2. Argo CD or GitOps flow
3. canary deployments
4. external secrets
5. network policies
6. load testing with `k6`
