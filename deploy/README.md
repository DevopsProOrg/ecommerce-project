# OG Fashions Deployment Docs

This folder is the main deployment and operations guide for the project.

Use these documents in order:

1. [DEVOPS-RUNBOOK.md](./DEVOPS-RUNBOOK.md)
   Start here for the complete end-to-end deployment roadmap.
2. [docker/README.md](./docker/README.md)
   Use this for local Docker or EC2 Docker deployment.
3. [jenkins/README.md](./jenkins/README.md)
   Use this for Jenkins server setup, credentials, webhooks, and multibranch pipeline configuration.
4. [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)
   Use this to understand the pipeline stages, branch strategy, approvals, and failure handling.
5. [k8s/README.md](./k8s/README.md)
   Use this for Kubernetes cluster deployment.
6. [monitoring/README.md](./monitoring/README.md)
   Use this for Prometheus and Grafana setup.

## Quick path by deployment type

### Docker on EC2

Read in this order:

1. [DEVOPS-RUNBOOK.md](./DEVOPS-RUNBOOK.md)
2. [docker/README.md](./docker/README.md)
3. [jenkins/README.md](./jenkins/README.md) if Jenkins-triggered deployment is needed

### Kubernetes

Read in this order:

1. [DEVOPS-RUNBOOK.md](./DEVOPS-RUNBOOK.md)
2. [k8s/README.md](./k8s/README.md)
3. [monitoring/README.md](./monitoring/README.md)
4. [jenkins/README.md](./jenkins/README.md)
5. [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)

### Jenkins CI/CD

Read in this order:

1. [jenkins/README.md](./jenkins/README.md)
2. [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)
3. [docker/README.md](./docker/README.md) and/or [k8s/README.md](./k8s/README.md)

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

## Recommended real-world rollout order

1. Push code to GitHub
2. Deploy with Docker on EC2
3. Attach Elastic IP
4. Validate application end to end
5. Set up Jenkins
6. Trigger Docker deployment from Jenkins
7. Set up Kubernetes
8. Add monitoring
9. Add production approval and release flow
