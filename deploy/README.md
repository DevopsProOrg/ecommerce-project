# OG Fashions Deployment Docs

This folder is the main deployment and operations guide for the project.

Use these documents in order:

1. [END-TO-END-STEP-BY-STEP.md](./END-TO-END-STEP-BY-STEP.md)
   Start here if you want one complete step-by-step deployment and configuration guide for all cases.
2. [REALTIME-DEVOPS-IMPLEMENTATION.md](./REALTIME-DEVOPS-IMPLEMENTATION.md)
   Start here for the practical step-by-step implementation order from local validation to production operations.
3. [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
   Start here for the complete configuration and deployment checklist from local to production.
4. [DEVOPS-RUNBOOK.md](./DEVOPS-RUNBOOK.md)
   Use this for the overall DevOps rollout order.
5. [ORG-PLATFORM-RUNBOOK.md](./ORG-PLATFORM-RUNBOOK.md)
   Use this for the organization-style platform sequence across Docker, Terraform, Ansible, Jenkins, Kubernetes, and monitoring.
6. [ORG-PRODUCTION-PLAYBOOK.md](./ORG-PRODUCTION-PLAYBOOK.md)
   Use this for preflight, smoke checks, promotion rules, rollback thinking, and production operations.
7. [DEVOPS-OPERATIONS-HANDBOOK.md](./DEVOPS-OPERATIONS-HANDBOOK.md)
   Use this as the day-to-day DevOps operating handbook for the project.
8. [ENVIRONMENT-MATRIX.md](./ENVIRONMENT-MATRIX.md)
   Use this to understand level purpose, access model, and promotion order.
9. [docker/README.md](./docker/README.md)
   Use this for local Docker or EC2 Docker deployment.
10. [jenkins/README.md](./jenkins/README.md)
   Use this for Jenkins server setup, credentials, webhooks, multibranch pipeline configuration, and email notifications.
11. [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)
   Use this to understand the pipeline stages, branch strategy, approvals, and failure handling.
12. [k8s/README.md](./k8s/README.md)
   Use this for Kubernetes cluster deployment.
13. [monitoring/README.md](./monitoring/README.md)
   Use this for Prometheus and Grafana setup.

## Recommended read order

If you want the safest full path, follow this order:

1. [END-TO-END-STEP-BY-STEP.md](./END-TO-END-STEP-BY-STEP.md)
2. [DEPLOYMENT-CHECKLISTS.md](./DEPLOYMENT-CHECKLISTS.md)
3. [REALTIME-DEVOPS-IMPLEMENTATION.md](./REALTIME-DEVOPS-IMPLEMENTATION.md)
4. [ORG-PRODUCTION-PLAYBOOK.md](./ORG-PRODUCTION-PLAYBOOK.md)
5. [DEVOPS-OPERATIONS-HANDBOOK.md](./DEVOPS-OPERATIONS-HANDBOOK.md)
6. [ENVIRONMENT-MATRIX.md](./ENVIRONMENT-MATRIX.md)
7. [local/README.md](./local/README.md)
8. [terraform/README.md](./terraform/README.md)
9. [ansible/README.md](./ansible/README.md)
10. [docker/README.md](./docker/README.md)
11. [jenkins/README.md](./jenkins/README.md)
12. [k8s/README.md](./k8s/README.md)
13. [monitoring/README.md](./monitoring/README.md)

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

For `l1`, `l2`, `l3`, and `l6`, plan to use IP address or load balancer address.

For `lp`, plan to use the production domain `ogstyles.store`.

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
  - alert email routing in Alertmanager
  - Loki and Promtail retention choices if you change them later

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
