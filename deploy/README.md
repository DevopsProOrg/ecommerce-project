# OG Fashions Deployment Docs

This folder is the main deployment and operations guide for the project.

Use these documents in order:

1. [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
   Start here for the complete configuration and deployment checklist from local to production.
2. [DEVOPS-RUNBOOK.md](./DEVOPS-RUNBOOK.md)
   Use this for the overall DevOps rollout order.
3. [ORG-PLATFORM-RUNBOOK.md](./ORG-PLATFORM-RUNBOOK.md)
   Use this for the organization-style platform sequence across Docker, Terraform, Ansible, Jenkins, Kubernetes, and monitoring.
4. [docker/README.md](./docker/README.md)
   Use this for local Docker or EC2 Docker deployment.
5. [jenkins/README.md](./jenkins/README.md)
   Use this for Jenkins server setup, credentials, webhooks, multibranch pipeline configuration, and email notifications.
6. [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)
   Use this to understand the pipeline stages, branch strategy, approvals, and failure handling.
7. [k8s/README.md](./k8s/README.md)
   Use this for Kubernetes cluster deployment.
8. [monitoring/README.md](./monitoring/README.md)
   Use this for Prometheus and Grafana setup.

## Recommended read order

If you want the safest full path, follow this order:

1. [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
2. [local/README.md](./local/README.md)
3. [terraform/README.md](./terraform/README.md)
4. [ansible/README.md](./ansible/README.md)
5. [docker/README.md](./docker/README.md)
6. [jenkins/README.md](./jenkins/README.md)
7. [k8s/README.md](./k8s/README.md)
8. [monitoring/README.md](./monitoring/README.md)

## Quick path by deployment type

### Docker on EC2

Read in this order:

1. [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
2. [DEVOPS-RUNBOOK.md](./DEVOPS-RUNBOOK.md)
3. [docker/README.md](./docker/README.md)
4. [jenkins/README.md](./jenkins/README.md) if Jenkins-triggered deployment is needed

### Kubernetes

Read in this order:

1. [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
2. [ORG-PLATFORM-RUNBOOK.md](./ORG-PLATFORM-RUNBOOK.md)
3. [k8s/README.md](./k8s/README.md)
4. [monitoring/README.md](./monitoring/README.md)
5. [jenkins/README.md](./jenkins/README.md)
6. [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)

### Jenkins CI/CD

Read in this order:

1. [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
2. [jenkins/README.md](./jenkins/README.md)
3. [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)
4. [docker/README.md](./docker/README.md) and/or [k8s/README.md](./k8s/README.md)

### Terraform and Ansible

Read in this order:

1. [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
2. [terraform/README.md](./terraform/README.md)
3. [ansible/README.md](./ansible/README.md)
4. [docker/README.md](./docker/README.md) or [k8s/README.md](./k8s/README.md)

## Important placeholders to replace

Before any real deployment, replace these placeholders:

- Docker / EC2
  - `deploy/docker/docker.env`
  - `APP_PUBLIC_BASE_URL`
  - `MYSQL_ROOT_PASSWORD`
  - `APP_BOOTSTRAP_ADMIN_EMAIL`
  - `APP_BOOTSTRAP_ADMIN_PASSWORD`
  - `SPRING_MAIL_PASSWORD`
- Jenkins
  - `deploy/jenkins/jenkins.env.example`
  - all Jenkins credentials IDs and environment values
- Kubernetes
  - `deploy/k8s/secret.yaml` created from `secret.example.yaml`
  - image names in deploy commands
  - ingress hosts in overlays if you later add a real DNS name
- Monitoring
  - Grafana admin credentials if you customize them later
  - alert email routing if you add Alertmanager later

## Recommended real-world rollout order

1. Push code to GitHub
2. Validate without Docker if needed
3. Validate with local Docker
4. Create infrastructure with Terraform
5. Configure servers with Ansible
6. Deploy with Docker on EC2 or Ubuntu
7. Attach Elastic IP or validate DNS
8. Validate application end to end
9. Set up Jenkins and email notifications
10. Trigger Docker deployment from Jenkins
11. Set up Kubernetes for `l1`, `l2`, `l3`, `l6`, `lp`
12. Add monitoring
13. Add production approval and release flow
