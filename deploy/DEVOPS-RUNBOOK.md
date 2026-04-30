# OG Fashions DevOps Runbook

This is the top-level operations guide for the project.

If you are doing the full DevOps setup end to end, follow this document first and then move into the detailed READMEs for each deployment method.

## 1. Recommended rollout order

Use this order to avoid confusion and reduce rework:

1. local development validation
2. Docker deployment on EC2
3. attach Elastic IP
4. validate application end to end on EC2
5. Jenkins setup
6. Jenkins-triggered Docker deployment
7. Kubernetes setup
8. Kubernetes deployment
9. monitoring setup
10. release branch approval and production flow

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

- [k8s/README.md](./k8s/README.md)

### Monitoring stack

Use:

```bash
kubectl apply -k deploy/monitoring/k8s
```

Detailed guide:

- [monitoring/README.md](./monitoring/README.md)

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

## 7. Production-style recommendations

For stronger real-world maturity later:

- use Amazon RDS instead of self-managed MySQL
- use ECR instead of local-only images
- use HTTPS and a domain
- place Jenkins on a separate host
- add Alertmanager and Slack/email alert routing
- add automated smoke tests after deployment

## 8. Related docs

- [README.md](../README.md)
- [README.md](./README.md)
- [docker/README.md](./docker/README.md)
- [k8s/README.md](./k8s/README.md)
- [monitoring/README.md](./monitoring/README.md)
- [jenkins/README.md](./jenkins/README.md)
- [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md)
