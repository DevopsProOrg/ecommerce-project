# OG Styles Platform Runbook

This is the end-to-end rollout order for the ecommerce platform across local Docker, server Docker, Kubernetes, Terraform, Ansible, Jenkins, Prometheus, and Grafana.

For the exact checklist and configuration items at each layer, use [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md) first and then return here for the rollout sequence.

## 1. Local Docker first

Do this before anything else.

### Step 1. Create local env files

```bash
cp deploy/local/local-docker.env.example deploy/local/local-docker.env
cp deploy/local/local-docker.secrets.env.example deploy/local/local-docker.secrets.env
```

### Step 2. Run locally

Linux, Ubuntu, macOS, WSL, or Git Bash:

```bash
bash deploy/local/local-docker.sh
```

Windows PowerShell:

```powershell
./deploy/local/local-docker.ps1
```

### Step 3. Test locally

Use the printed frontend URL.

Check:

1. home page
2. login
3. products
4. cart
5. admin page
6. health endpoints
7. backend metrics endpoint [example: `http://localhost:8080/api/actuator/prometheus`]

## 2. Terraform next

Provision infrastructure before server configuration.

### Step 1. Choose a level

- `l1`
- `l2`
- `l3`
- `l6`
- `lp`

### Step 2. Create `terraform.tfvars`

```bash
cd deploy/terraform/environments/lp
cp terraform.tfvars.example terraform.tfvars
```

### Step 3. Run Terraform

```bash
terraform init
terraform plan
terraform apply
```

### Step 4. Save outputs

Keep:

- EC2 IPs
- Elastic IP
- EKS details
- Route53 outputs
- security group IDs [example: app SG, LB SG]
- subnet IDs [example: public subnets for ALB]

## 3. Ansible after Terraform

### Step 1. Update inventory

```bash
cd deploy/ansible
vi inventories/lp/hosts.yml
```

### Step 2. Ping all hosts

```bash
ansible -i inventories/lp/hosts.yml all -m ping
```

### Step 3. Run site playbook

```bash
ansible-playbook -i inventories/lp/hosts.yml playbooks/site.yml
```

### Step 4. Confirm host readiness

Check:

1. Docker installed
2. app directory exists [example: `/opt/ogfashions`]
3. Node Exporter installed if enabled
4. SSH user can run Docker
5. server-local secrets file exists if Ansible is rendering it

## 4. Server Docker deployment

Use this as the first live deployment path.

### Step 1. Prepare secrets

```bash
cp deploy/docker/docker.secrets.env.example deploy/docker/docker.secrets.env
vi deploy/docker/docker.secrets.env
```

### Step 2. Run Docker deploy

Linux, Ubuntu, EC2, WSL, or Git Bash:

```bash
bash deploy/docker/ec2-deploy.sh
```

Windows PowerShell:

```powershell
./deploy/docker/ec2-deploy.ps1
```

### Step 3. Test the deployed app

By domain:

- `https://ogstyles.store/`
- `https://ogstyles.store/healthz`
- `https://ogstyles.store/api/actuator/health`
- `https://ogstyles.store/api/actuator/prometheus`

### Step 4. Validate host networking

Check:

1. inbound `80` open
2. inbound `443` open if HTTPS is enabled
3. DNS points to the active public IP or load balancer
4. SSL termination path is known [example: Nginx on host, ALB, or ingress]

## 5. Kubernetes rollout

Use this after Docker is proven.

### Step 1. Create Kubernetes secret

Start from:

- `deploy/k8s/secrets/ogfashions-secrets.example.yaml`

### Step 2. Apply the chosen level

```bash
bash deploy/k8s/scripts/deploy.sh lp yarrav7 latest
```

Example domains:

- `l1.ogstyles.store`
- `l2.ogstyles.store`
- `l3.ogstyles.store`
- `l6.ogstyles.store`
- `ogstyles.store`

### Step 3. Validate Kubernetes

```bash
kubectl get all -n ogfashions-lp
kubectl logs deploy/ogfashions-backend -n ogfashions-lp
kubectl logs deploy/ogfashions-frontend -n ogfashions-lp
```

### Step 4. Validate ingress and DNS

Check:

1. ingress controller is installed
2. ingress hostname matches overlay domain
3. DNS record points to ingress load balancer
4. TLS is configured if production uses HTTPS

## 6. Jenkins automation

Use Jenkins only after manual Docker or Kubernetes deployment is already understood.

### Step 1. Configure Jenkins credentials

- Docker registry
- EC2 SSH key
- kubeconfig
- Sonar token
- Nexus creds

### Step 2. Configure env values

- `EC2_SSH_HOST`
- `EC2_SSH_USER`
- `IMAGE_REPO`
- `DEPLOY_BASE_URL`
- `NOTIFY_EMAIL`

### Step 3. Run branch flow

1. feature branch validation
2. `main` or `master` image build and push
3. `release` approval and production deploy

### Step 4. Validate email notifications

You should receive:

1. success email
2. failure email
3. unstable email if the pipeline marks unstable
4. aborted email if approval is cancelled or build is aborted

## 7. Prometheus and Grafana

### Docker monitoring

```bash
bash deploy/monitoring/docker/run-monitoring.sh
```

### Kubernetes monitoring

```bash
kubectl apply -k deploy/monitoring/k8s
```

### Validate monitoring

1. Prometheus opens
2. Grafana opens
3. backend metrics appear
4. health endpoints stay green
5. dashboards show current containers or pods
6. notification path is planned [example: Alertmanager email later]

## 8. Recommended full sequence

1. local Docker
2. Terraform
3. Ansible
4. Docker on EC2 or Ubuntu
5. DNS and domain validation
6. Jenkins automation
7. Kubernetes overlays `l1`, `l2`, `l3`, `l6`, `lp`
8. Prometheus and Grafana
9. release sign-off and operational handover

## 9. Port conflict policy

The project now follows this model:

1. local Docker uses automatic fallback ports
2. Docker monitoring uses automatic fallback ports
3. production Docker prefers fixed ports and fails early unless you explicitly switch to auto mode
4. Kubernetes avoids host-port conflicts by using Services and Ingress instead of fixed host ports

## 10. Cross-platform usage

Supported operator paths in this repo:

- Linux
- Ubuntu
- WSL
- Git Bash
- Windows PowerShell

For Windows, PowerShell wrappers are included for local Docker, server Docker, and monitoring entrypoints.

Production deployments should still target Linux-based servers [example: Ubuntu 22.04, Amazon Linux 2023] even if the operator machine is Windows.
