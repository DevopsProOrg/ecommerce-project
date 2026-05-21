# Jenkins Pipeline Stages Guide

This document explains the real pipeline behavior implemented in the repository `Jenkinsfile`.

Use it together with [README.md](./README.md).

## Pipeline type

The recommended Jenkins job type is:

- Multibranch Pipeline

This allows Jenkins to automatically discover and build:

- feature branches
- `main`
- `master`
- `release`
- `release/*`
- custom branches such as `venky`, `abcd`, or `defect-102`

## Pipeline parameters

The current `Jenkinsfile` exposes these parameters:

- `DEPLOY_TARGET`
  - `none`
  - `docker`
  - `k8s`
  - `both`
- `DEPLOY_LEVEL`
  - `none`
  - `l1`
  - `l2`
  - `l3`
  - `l6`
  - `lp`
- `K8S_OVERLAY`
  - `l1`
  - `l2`
  - `l3`
  - `l6`
  - `lp`
- `RUN_SONAR`
  - run SonarQube stage when enabled
- `RUN_TRIVY`
  - run Trivy file system and image scans when enabled
- `RUN_NEXUS_PUBLISH`
  - publish Maven artifact to Nexus when enabled
- `REMOTE_APP_DIR`
  - remote application checkout directory on the target Docker host
- `DOCKER_ENV_REMOTE_PATH`
  - remote path for the Docker environment file on the target EC2 or Linux server
- `DEPLOY_BASE_URL`
  - base URL used by smoke checks and notification emails

## Branch behavior

Branch names are no longer the deployment gate.

Any branch can:

- validate only
- push images
- deploy to lower levels
- deploy to `l6`
- request deployment to `lp`

What decides the behavior is:

- `DEPLOY_TARGET`
- `DEPLOY_LEVEL`

Examples:

- branch `venky` with `DEPLOY_TARGET=none`
  - validation only
- branch `defect-102` with `DEPLOY_TARGET=docker` and `DEPLOY_LEVEL=l2`
  - push image and deploy to `l2`
- branch `release` with `DEPLOY_TARGET=both` and `DEPLOY_LEVEL=lp`
  - push image, wait for approval, deploy production

## Stage-by-stage breakdown

### 1. Checkout

Purpose:

- pull source code from the SCM configured in Jenkins

### 2. Initialize Metadata

Purpose:

- generate image names
- generate image tag
- detect deployment intent from parameters

Outputs:

- `BACKEND_IMAGE`
- `FRONTEND_IMAGE`
- `IMAGE_TAG`
- `DEPLOY_LEVEL`
- `HAS_DEPLOY_REQUEST`
- `IS_PRODUCTION_LEVEL`
- `ALLOW_IMAGE_PUSH`

### 3. Compile Backend

Command:

```text
mvn -q -DskipTests compile
```

Purpose:

- fail fast on Java compile errors

### 4. Unit Tests

Command:

```text
mvn -q test
```

Purpose:

- validate backend behavior before building images

### 5. Frontend Validation

Commands:

```text
node --check frontend/js/admin.js
node --check frontend/js/auth.js
node --check frontend/js/app-config.js
```

Purpose:

- catch broken frontend JavaScript syntax before packaging

### 6. Package Backend

Command:

```text
mvn -q -DskipTests package
```

Purpose:

- verify the backend packages successfully

### 7. SonarQube Analysis

Runs only when:

- `RUN_SONAR=true`

Purpose:

- static quality analysis
- code smell and maintainability checks

Requirement:

- SonarQube server
- `sonar-token` Jenkins credential

### 8. Trivy Filesystem Scan

Runs only when:

- `RUN_TRIVY=true`

Purpose:

- scan repository contents for high and critical issues

### 9. Build Docker Images

Purpose:

- build deployable images

### 10. Trivy Image Scan

Runs only when:

- `RUN_TRIVY=true`

Purpose:

- scan built images before push and deployment

### 11. Publish Maven Artifact To Nexus

Runs only when:

- `RUN_NEXUS_PUBLISH=true`

Purpose:

- publish backend artifact to Nexus

Requirement:

- Nexus server
- `nexus-creds` Jenkins credential

### 12. Push Images To Registry

Runs when:

- deployment is requested for any branch
- or the branch is `main`, `master`, `release`, or `release/*`

Purpose:

- log in to the container registry
- push backend image
- push frontend image

Requirement:

- `docker-registry-creds`

### 13. Deploy Selected Non-Prod Environment

Runs only when:

- `DEPLOY_TARGET != none`
- `DEPLOY_LEVEL` is `l1`, `l2`, `l3`, or `l6`

Deployment options:

- Docker on EC2 or Linux
- Kubernetes selected overlay
- both in parallel

### 14. Production Approval

Runs only when:

- `DEPLOY_TARGET != none`
- `DEPLOY_LEVEL=lp`

Purpose:

- require manual human approval before production deployment

### 15. Deploy Production

Runs only when:

- `DEPLOY_LEVEL=lp`
- approval is granted

Deployment options:

- Docker production
- Kubernetes production overlay
- both in parallel

### 16. Post Deploy Smoke Check

Purpose:

- verify the deployed entrypoint after Docker or Kubernetes rollout

Current behavior:

- checks `<DEPLOY_BASE_URL>/healthz`
- checks `<DEPLOY_BASE_URL>/api/actuator/health`

### 17. Post-build Email Notification

Purpose:

- send build result emails after every run

Current behavior:

- sends `SUCCESS`, `FAILURE`, `UNSTABLE`, or `ABORTED`
- uses `NOTIFY_EMAIL`
- includes branch, build number, deploy target, image tags, base URL, and Jenkins build URL

## Notifications

The pipeline emails on:

- success
- failure
- unstable
- aborted

## Required external tools and services

The pipeline is ready for these integrations, but they must exist in your environment:

- GitHub
- Jenkins
- Docker registry
- SonarQube
- Nexus
- Trivy
- EC2 Docker host
- Kubernetes cluster
- SMTP email

## Recommended real-world promotion flow

### Lower environments

Use:

- any branch with `DEPLOY_LEVEL=l1`, `l2`, `l3`, or `l6`

Behavior:

- build
- validate
- push
- deploy

### Production

Use:

- any branch with `DEPLOY_LEVEL=lp`

Behavior:

- build
- scan
- push
- wait for approval
- deploy

## Placeholder values to replace in Jenkins or repo configuration

- `REGISTRY`
- `IMAGE_REPO`
- `EC2_SSH_HOST`
- `EC2_SSH_USER`
- `SONAR_HOST_URL`
- `NEXUS_MAVEN_REPO_URL`
- `NOTIFY_EMAIL`

## Troubleshooting

### Docker deploy stage fails

Check:

- Jenkins can SSH to EC2
- repo exists at `/opt/ogfashions`
- remote `docker.env` file exists
- images are already pushed

### Kubernetes deploy stage fails

Check:

- kubeconfig credential is valid
- `kubectl` is installed on Jenkins node
- cluster is reachable
- Kubernetes secrets are applied in the target namespace

### Sonar stage fails

Check:

- sonar scanner is installed or intentionally skipped
- `sonar-token`
- SonarQube server reachability

### Email not received

Check:

- Jenkins SMTP configuration
- `NOTIFY_EMAIL` value
- firewall access to SMTP
- Gmail app password if using Gmail SMTP
