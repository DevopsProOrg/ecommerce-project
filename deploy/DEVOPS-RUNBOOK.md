# OG Fashions DevOps Runbook

This is the top-level operations guide for the project.

If you are doing the full DevOps setup end to end, follow this document first and then move into the detailed READMEs for each deployment method.

For exact configure-this-then-run-this checklists, start with [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md).

## 1. Recommended rollout order

Use this order to avoid confusion and reduce rework:

1. local development validation
2. local Docker validation
3. Terraform infrastructure creation
4. Ansible host configuration
5. Docker deployment on EC2 or Ubuntu
6. attach Elastic IP or finalize DNS
7. validate application end to end on EC2
8. Jenkins setup
9. Jenkins-triggered Docker deployment
10. Kubernetes setup
11. Kubernetes deployment
12. monitoring setup
13. release branch approval and production flow

## 2. AWS free-tier recommendation

If you stop and start an EC2 instance, the public IP changes unless you attach an **Elastic IP**.

Use this for stable deployment:

1. launch one EC2 instance
2. allocate one Elastic IP
3. associate it to the EC2 instance
4. use that Elastic IP in:
   - Jenkins environment variables
   - `deploy/docker/docker.env`
   - browser access

If you do not want a domain yet, Elastic IP is enough.

## 3. Deployment modes

### Local development

Backend:

```powershell
cd backend
mvn spring-boot:run
```

Frontend:

Serve `frontend/` with Live Server or any static server.

Examples:

- VS Code Live Server [example: `http://127.0.0.1:5500`]
- Python static server [example: `python -m http.server 5500`]

Detailed checklist:

- [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)

### Docker on EC2 or Linux

Use:

```bash
./deploy/docker/ec2-deploy.sh
```

For registry-based rollout:

```bash
./deploy/docker/ec2-registry-deploy.sh
```

Detailed guide:

- [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
- [docker/README.md](./docker/README.md)

### Kubernetes

Use:

```bash
./deploy/k8s/deploy.sh staging your-registry/ogfashions-backend:tag your-registry/ogfashions-frontend:tag
```

or:

```bash
./deploy/k8s/deploy.sh production your-registry/ogfashions-backend:tag your-registry/ogfashions-frontend:tag
```

Detailed guide:

- [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
- [k8s/README.md](./k8s/README.md)

### Monitoring stack

Use:

```bash
kubectl apply -k deploy/monitoring/k8s
```

Detailed guide:

- [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
- [monitoring/README.md](./monitoring/README.md)

### Terraform and Ansible

Use Terraform first and Ansible second.

Examples:

```bash
cd deploy/terraform/environments/lp
terraform init
terraform plan
terraform apply
```

```bash
cd deploy/ansible
ansible -i inventories/lp/hosts.yml all -m ping
ansible-playbook -i inventories/lp/hosts.yml playbooks/site.yml
```

Detailed guides:

- [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
- [terraform/README.md](./terraform/README.md)
- [ansible/README.md](./ansible/README.md)

## 4. Jenkins org-level flow

Branch strategy:

- feature branches
  - checkout
  - compile
  - tests
  - frontend validation
  - optional Sonar and Trivy
- `main` / `master`
  - build
  - push image
  - deploy lower environment
- `release/*`
  - build
  - scan
  - push
  - approval gate
  - production deploy

Detailed Jenkins docs:

- [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
- [jenkins/README.md](./jenkins/README.md)
- [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)

## 5. Tooling supported by the repo

The repository is prepared for these real-world tools:

- Jenkins
- GitHub webhook integration
- Docker
- Kubernetes
- SonarQube
- Nexus Repository
- Trivy
- Prometheus
- Grafana
- SMTP email notifications

These are integrated at repo and pipeline level, but the actual servers still need to exist in your environment.

## 6. Secrets and configuration policy

Do not keep real secrets in Git.

Use:

- Jenkins credentials
- Kubernetes secrets
- private environment files on EC2
- AWS Secrets Manager later if needed

Placeholders that must be replaced:

- Docker env values in `deploy/docker/docker.env`
- Jenkins values in `deploy/jenkins/jenkins.env.example`
- Kubernetes secret values in `deploy/k8s/secret.yaml`
- Terraform `terraform.tfvars` values [example: region, subnets, domain, EKS toggle]
- Ansible inventory and `group_vars` values [example: host IP, SSH user, app directory]

## 7. Production-style recommendations

For stronger real-world maturity later:

- use Amazon RDS instead of self-managed MySQL
- use ECR instead of local-only images
- use HTTPS and a domain
- place Jenkins on a separate host
- add Alertmanager and Slack/email alert routing
- add automated smoke tests after deployment

## 8. Related docs

- [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
- [README.md](../README.md)
- [README.md](./README.md)
- [docker/README.md](./docker/README.md)
- [terraform/README.md](./terraform/README.md)
- [ansible/README.md](./ansible/README.md)
- [k8s/README.md](./k8s/README.md)
- [monitoring/README.md](./monitoring/README.md)
- [jenkins/README.md](./jenkins/README.md)
- [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)
