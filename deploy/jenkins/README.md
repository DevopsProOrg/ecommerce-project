# Jenkins CI/CD Setup

This project now includes a root `Jenkinsfile` for org-style CI/CD.

## Required Jenkins plugins

- Pipeline
- Git
- Credentials Binding
- SSH Agent
- Workspace Cleanup
- ANSI Color

## Required Jenkins tools on the agent

- Java 17+
- Maven
- Node.js
- Docker CLI
- `kubectl`
- `bash`

## Required Jenkins credentials

- `docker-registry-creds`
  Docker Hub or ECR user/password
- `ec2-ssh-key`
  SSH key for the Docker deployment host
- `kubeconfig-credential`
  File credential containing kubeconfig for the cluster

## Required Jenkins environment variables

Set these on the job or folder:

- `EC2_SSH_HOST`
- `EC2_SSH_USER`

Optional overrides:

- `REGISTRY`
- `IMAGE_REPO`

## Pipeline behavior

1. Checkout code
2. Compile backend
3. Validate frontend JS syntax
4. Build frontend/backend Docker images
5. Push images on `main`, `master`, `release`, or `release/*`
6. Deploy to Docker host, Kubernetes, or both depending on parameters

## Recommended org flow

- Feature branches: validate only
- `main` or `master`: deploy to staging
- `release` or `release/*`: deploy to production after approval

## Infrastructure advice

For Docker on EC2, use one of:

- an AWS Elastic IP
- a stable DNS name
- an ALB/NLB in front of the host

That avoids changing your public URL every time the server is recreated.
