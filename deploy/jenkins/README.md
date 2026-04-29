# Jenkins CI/CD Setup

This guide explains how to set up Jenkins for this project from scratch and connect it to the repository `Jenkinsfile`.

Use this file for:

- Jenkins installation planning
- plugins
- tools
- credentials
- environment variables
- GitHub webhook setup
- multibranch pipeline configuration

Use [PIPELINE-STAGES.md](./PIPELINE-STAGES.md) for the detailed stage-by-stage pipeline behavior.

## 1. What this Jenkins setup supports

The repository `Jenkinsfile` supports:

- validation on feature branches
- lower-environment deploy from `main` / `master`
- approval-gated production deploy from `release` / `release/*`
- Docker deployment on EC2
- Kubernetes deployment
- optional SonarQube, Trivy, and Nexus stages
- success and failure email notifications

## 2. Recommended Jenkins server options

You can host Jenkins in one of these ways:

- separate EC2 instance
- same EC2 host as Docker app deployment for learning and cost-saving
- on-prem or local VM

For cleaner production-style separation, prefer a separate Jenkins host.

## 3. Required Jenkins plugins

Install these plugins:

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

## 4. Required tools on the Jenkins agent

Install and make available in PATH:

- Java 17+
- Maven
- Node.js
- Docker CLI
- `kubectl`
- `bash`

Optional but recommended:

- Sonar Scanner
- Trivy

## 5. Jenkins credentials to create

Create these credentials in Jenkins global credentials:

### `docker-registry-creds`

Type:

- Username with password

Purpose:

- Docker Hub or ECR push

### `ec2-ssh-key`

Type:

- SSH Username with private key

Purpose:

- connect Jenkins to the EC2 Docker host

### `kubeconfig-credential`

Type:

- Secret file

Purpose:

- provide kubeconfig to deployment stages

### `sonar-token`

Type:

- Secret text

Purpose:

- authenticate SonarQube scan

### `nexus-creds`

Type:

- Username with password

Purpose:

- publish Maven artifact to Nexus

### `jenkins-notify-email`

Type:

- Secret text

Purpose:

- notification email address or email distribution list

## 6. Jenkins environment variables

Set these in Jenkins global environment variables, folder properties, or job environment:

- `EC2_SSH_HOST`
- `EC2_SSH_USER`
- `REGISTRY`
- `IMAGE_REPO`

Optional:

- `SONAR_HOST_URL`
- `NEXUS_MAVEN_REPO_URL`
- `TRIVY_SEVERITY`

Example values are available in:

- [jenkins.env.example](./jenkins.env.example)

## 7. Jenkins email configuration

Go to:

- Manage Jenkins
- System
- Extended E-mail Notification

Recommended Gmail SMTP values:

- SMTP server: `smtp.gmail.com`
- SMTP port: `587`
- Authentication: enabled
- Username: `contact.ogfashions@gmail.com`
- Password: Gmail app password
- TLS: enabled

Test the email configuration before using pipeline notifications.

## 8. GitHub webhook setup

Configure the repository webhook to point to:

```text
http://<jenkins-host>:8080/github-webhook/
```

Recommended event:

- push event

## 9. Recommended Jenkins job type

Use a:

- Multibranch Pipeline

This is the best match for the branch-driven behavior already coded in the `Jenkinsfile`.

## 10. Multibranch pipeline configuration

When creating the Jenkins job:

1. choose `Multibranch Pipeline`
2. add GitHub as the branch source
3. configure credentials if the repository is private
4. keep the script path as:

```text
Jenkinsfile
```

5. enable webhook-based triggering

## 11. First recommended pipeline run

Use a `main` branch build first with:

- `DEPLOY_TARGET=docker`
- `RUN_SONAR=false` if Sonar is not ready
- `RUN_TRIVY=false` if Trivy is not ready
- `RUN_NEXUS_PUBLISH=false` if Nexus is not ready

This gives you the simplest first successful org-style rollout.

## 12. AWS free-tier recommendation

If you stop and start EC2 and the public IP changes, attach an **Elastic IP**.

That keeps Jenkins variables stable and avoids changing deployment config repeatedly.

## 13. Recommended setup order

1. Deploy application manually with Docker on EC2
2. Confirm the application works
3. Install Jenkins
4. Add credentials and environment variables
5. Configure webhook
6. Create multibranch pipeline
7. Trigger `main`
8. Later enable Sonar, Trivy, Nexus, and Kubernetes

## 14. Related docs

- [PIPELINE-STAGES.md](./PIPELINE-STAGES.md)
- [../docker/README.md](../docker/README.md)
- [../k8s/README.md](../k8s/README.md)
- [../DEVOPS-RUNBOOK.md](../DEVOPS-RUNBOOK.md)
