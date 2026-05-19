# Jenkins CI/CD Setup

This guide explains how to set up Jenkins for this project and connect it to the repository [Jenkinsfile](/E:/Restart/Projects/ecommerce-project/Jenkinsfile).

Use [PIPELINE-STAGES.md](./PIPELINE-STAGES.md) for the detailed stage-by-stage pipeline behavior.

## What this Jenkins setup supports

The repository pipeline supports:

- validation on feature branches
- lower-environment deploy from `main` or `master`
- approval-gated production deploy from `release` or `release/*`
- Docker deployment on EC2
- Kubernetes deployment
- optional SonarQube, Trivy, and Nexus stages
- browser-facing smoke checks
- post-build email notifications for success, failure, unstable, and aborted runs

## Recommended Jenkins server options

You can host Jenkins on:

- a separate EC2 instance
- the same EC2 host as the Docker app deployment for learning and cost-saving
- on-prem or a local VM

For cleaner production-style separation, prefer a separate Jenkins host.

## Required Jenkins plugins

Install:

- Pipeline
- Git
- GitHub
- Credentials Binding
- SSH Agent
- Workspace Cleanup
- ANSI Color
- Email Extension

Recommended optional plugins:

- SonarQube Scanner
- Docker Pipeline
- Warnings Next Generation

## Required tools on the Jenkins agent

Install and make available in `PATH`:

- Java 17+
- Maven
- Node.js
- Docker CLI
- `kubectl`
- `bash`

Optional but recommended:

- Sonar Scanner
- Trivy

## Suggested Jenkins credentials

Create these in Jenkins global credentials:

- `docker-registry-creds`
- `ec2-ssh-key`
- `kubeconfig-credential`
- `sonar-token`
- `nexus-creds`

## Suggested Jenkins environment variables

Set these in Jenkins global env vars, folder properties, or job configuration:

- `EC2_SSH_HOST`
- `EC2_SSH_USER`
- `REGISTRY`
- `IMAGE_REPO`
- `DEPLOY_BASE_URL`
- `NOTIFY_EMAIL`

Optional:

- `SONAR_HOST_URL`
- `NEXUS_MAVEN_REPO_URL`
- `TRIVY_SEVERITY`

Examples are available in:

- [jenkins.env.example](./jenkins.env.example)

## Email notifications

The pipeline now sends email on:

- success
- failure
- unstable
- aborted

### Step 1. Install the plugin

Install:

- Email Extension

### Step 2. Configure SMTP in Jenkins

Go to:

- Manage Jenkins
- System
- Extended E-mail Notification

Typical Gmail SMTP values:

- SMTP server: `smtp.gmail.com`
- SMTP port: `587`
- Use SMTP authentication: enabled
- Username: your sender mailbox
- Password: app password
- Use TLS: enabled

### Step 3. Set recipients

Set `NOTIFY_EMAIL` in Jenkins global environment variables, folder properties, or the multibranch job environment.

Example:

- `contact.ogfashions@gmail.com`

You can also use multiple recipients:

- `contact.ogfashions@gmail.com,team@example.com`

### Step 4. Test a build

Run one build and confirm you receive:

- build result
- branch name
- build number
- deploy target
- image tags
- build URL

## Branch policy

- feature branches:
  - validate only
- `main` and `master`:
  - push images
  - deploy lower environment
- `release` and `release/*`:
  - push images
  - require approval
  - deploy production

## Suggested pipeline parameters

- `DEPLOY_TARGET`
  - `none`
  - `docker`
  - `k8s`
  - `both`
- `K8S_OVERLAY`
  - `l1`
  - `l2`
  - `l3`
  - `l6`
  - `lp`
- `REMOTE_APP_DIR`
- `DOCKER_ENV_REMOTE_PATH`
- `DEPLOY_BASE_URL`

## GitHub webhook setup

Configure the repository webhook to point to:

```text
http://<jenkins-host>:8080/github-webhook/
```

Recommended event:

- push event

## Recommended Jenkins job type

Use a multibranch pipeline. It matches the branch-driven behavior already coded in the `Jenkinsfile`.

## Smoke test URLs

After deploy, Jenkins checks:

- `<DEPLOY_BASE_URL>/healthz`
- `<DEPLOY_BASE_URL>/api/actuator/health`

## Recommended setup order

1. Deploy the application manually with Docker on EC2.
2. Confirm the application works.
3. Install Jenkins.
4. Add credentials and environment variables.
5. Configure the webhook.
6. Create the multibranch pipeline.
7. Trigger `main` first.
8. Later enable Sonar, Trivy, Nexus, and Kubernetes.

## Related docs

- [PIPELINE-STAGES.md](./PIPELINE-STAGES.md)
- [../docker/README.md](../docker/README.md)
- [../k8s/README.md](../k8s/README.md)
- [../DEVOPS-RUNBOOK.md](../DEVOPS-RUNBOOK.md)
