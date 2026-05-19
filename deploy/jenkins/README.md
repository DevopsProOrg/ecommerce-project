# Jenkins Deployment Flow

The repository root contains the main pipeline file:

- [Jenkinsfile](/E:/Restart/Projects/ecommerce-project/Jenkinsfile)

## What Jenkins does

1. checks out the selected branch
2. compiles the backend
3. runs backend tests
4. validates frontend JavaScript
5. packages the backend
6. optionally runs SonarQube
7. optionally runs Trivy
8. builds Docker images
9. pushes Docker images
10. deploys Docker or Kubernetes depending on branch and parameters
11. runs browser-facing smoke checks

## Branch policy

- feature branches:
  - validate only
- `main` / `master`:
  - push images
  - deploy lower environment
- `release` / `release/*`:
  - push images
  - require approval
  - deploy production

## Suggested parameters

- `DEPLOY_TARGET`
  - `none`
  - `docker`
  - `k8s`
  - `both`
- `REMOTE_APP_DIR`
- `DOCKER_ENV_REMOTE_PATH`
- `DEPLOY_BASE_URL`

## Suggested Jenkins credentials

- `docker-registry-creds`
- `ec2-ssh-key`
- `kubeconfig-credential`
- `sonar-token`
- `nexus-creds`

## Smoke test URLs

After deploy, Jenkins checks:

- `<DEPLOY_BASE_URL>/healthz`
- `<DEPLOY_BASE_URL>/api/actuator/health`
