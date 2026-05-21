# OG Fashions Deployment Checklists

This document is the operator checklist for every deployment path in the repository.

Use it when you want a strict order like:

1. configure this
2. run this
3. validate this
4. move to the next level

## 1. Decide the target first

Choose the deployment path before changing any files:

1. local app run
2. local Docker run
3. server Docker run on Ubuntu, EC2, or another Linux host
4. Jenkins-triggered Docker deployment
5. Kubernetes deployment
6. Terraform infrastructure provisioning
7. Ansible server configuration
8. Prometheus and Grafana monitoring

Recommended real-world order:

1. local app run
2. local Docker
3. Terraform
4. Ansible
5. server Docker
6. Jenkins automation
7. Kubernetes
8. monitoring
9. production approvals and release flow

## 2. Define your environment levels

This repo supports these levels:

- `l1`
- `l2`
- `l3`
- `l6`
- `lp`

Suggested example mapping:

- `l1` [example: developer shared validation]
- `l2` [example: QA or SIT]
- `l3` [example: UAT]
- `l6` [example: pre-production]
- `lp` [example: production]

Recommended access model:

- `l1`, `l2`, `l3`
  lower environments such as dev and testing, usually without domain
- `l6`
  pre-production or staging, usually accessed by IP or load balancer address
- `lp`
  production, accessed by permanent domain

If your company uses different meanings, keep the same folder names in the repo and map them internally.

## 3. Global configuration checklist

Configure these once before serious deployments:

### Git and repository

1. GitHub repository is reachable.
2. default branch is known [example: `master`].
3. release branch strategy is known [example: `release`].

Branch naming note:

- branch names can be anything [example: `main`, `master`, `release`, `venky`, `abcd`, `defect-102`]
- Jenkins deployment behavior is controlled by pipeline parameters, not by branch name alone

### Container registry

1. create Docker Hub or ECR namespace
2. decide image owner [example: `yarrav7`]
3. decide backend image [example: `docker.io/yarrav7/ogfashions-backend`]
4. decide frontend image [example: `docker.io/yarrav7/ogfashions-frontend`]

### Domain and DNS

1. permanent domain chosen [example: `ogstyles.store`]
2. decide which levels use domain and which use IP or load balancer address
3. DNS provider known [example: GoDaddy or Route53]

Recommended for this project:

- `l1`, `l2`, `l3`, `l6`
  use IP address or load balancer address
- `lp`
  use `ogstyles.store`

### Email and notifications

1. SMTP provider chosen [example: Gmail SMTP]
2. sender mailbox chosen [example: `contact.ogfashions@gmail.com`]
3. app password or SMTP credentials created
4. notification recipients chosen [example: `contact.ogfashions@gmail.com,ops@ogstyles.store`]

### AWS or infrastructure

1. AWS account access available
2. region chosen [example: `ap-south-1`]
3. VPC CIDR chosen [example: `10.20.0.0/16`]
4. public subnet CIDRs chosen [example: `10.20.1.0/24`, `10.20.2.0/24`]
5. private subnet CIDRs chosen if needed [example: `10.20.11.0/24`, `10.20.12.0/24`]
6. EC2 key pair created [example: `ogstyles-lp-key`]
7. Route53 hosted zone ready if production DNS is needed
8. ACM certificate ARN ready if production ALB HTTPS is needed

### Security and secrets

Do not commit real secrets into Git. Put them only in:

- Jenkins credentials
- `deploy/docker/docker.secrets.env`
- Kubernetes Secret manifests
- Ansible Vault later if you adopt it

Real secrets you will usually need:

- MySQL root password
- datasource password
- bootstrap admin password
- Gmail app password
- Docker registry password
- SSH private key

## 4. Local non-Docker run checklist

Use this when you want the fastest developer feedback.

### Configure

1. Java 17 installed
2. Maven installed
3. Node.js installed if you are serving frontend with tooling
4. MySQL available locally or use the test profile only

### Run backend

```bash
cd backend
mvn spring-boot:run
```

Optional custom port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8891
```

### Run frontend

Serve the `frontend` folder with any static server.

Examples:

- VS Code Live Server [example: `http://127.0.0.1:5500`]
- Python static server [example: `python -m http.server 5500`]

### Validate

1. open frontend URL
2. test login
3. test product listing
4. test cart
5. test admin page
6. open backend health [example: `http://localhost:8888/actuator/health`]

## 5. Local Docker checklist

Use this before any server deployment.

Files used:

- [deploy/local/local-docker.env.example](/E:/Restart/Projects/ecommerce-project/deploy/local/local-docker.env.example)
- [deploy/local/local-docker.secrets.env.example](/E:/Restart/Projects/ecommerce-project/deploy/local/local-docker.secrets.env.example)
- [deploy/local/local-docker.sh](/E:/Restart/Projects/ecommerce-project/deploy/local/local-docker.sh)
- [deploy/local/local-docker.ps1](/E:/Restart/Projects/ecommerce-project/deploy/local/local-docker.ps1)

### Configure

1. copy env files
2. set local secrets
3. keep port strategy as `auto` unless you need fixed ports

Commands:

```bash
cp deploy/local/local-docker.env.example deploy/local/local-docker.env
cp deploy/local/local-docker.secrets.env.example deploy/local/local-docker.secrets.env
```

Example values:

- `FRONTEND_HOST_PORT=8081` [example]
- `BACKEND_HOST_PORT=8888` [example]
- `PORT_CONFLICT_STRATEGY=auto` [recommended for local]

### Run

Linux, Ubuntu, macOS, WSL, Git Bash:

```bash
bash deploy/local/local-docker.sh
```

Windows PowerShell:

```powershell
./deploy/local/local-docker.ps1
```

### Validate

1. home page opens
2. frontend health opens
3. backend health opens through frontend proxy

Examples:

- `http://localhost:8081/`
- `http://localhost:8081/healthz`
- `http://localhost:8081/api/actuator/health`

If port `8081` is already used, use the printed replacement port.

## 6. Terraform checklist

Use this to create infrastructure before Ansible or server deployment.

Files used:

- [deploy/terraform/README.md](/E:/Restart/Projects/ecommerce-project/deploy/terraform/README.md)
- [deploy/terraform/environments](/E:/Restart/Projects/ecommerce-project/deploy/terraform/environments)

### Configure

1. choose the target level
2. copy `terraform.tfvars.example` to `terraform.tfvars`
3. fill network, compute, DNS, and optional EKS values

Example:

```bash
cd deploy/terraform/environments/lp
cp terraform.tfvars.example terraform.tfvars
```

Typical fields to set:

- AWS region [example: `ap-south-1`]
- environment name [example: `lp`]
- VPC CIDR [example: `10.20.0.0/16`]
- instance type [example: `t3.medium`]
- hosted zone name [example: `ogstyles.store`]
- `enable_eks` [example: `true` for `lp`, `false` for `l1`]

### Run

```bash
terraform init
terraform plan
terraform apply
```

### Validate

1. note EC2 public IP or Elastic IP
2. note EKS cluster outputs if enabled
3. note Route53 or DNS outputs
4. save outputs in your ops notes

## 7. Ansible checklist

Use this after Terraform and before the first live deployment.

Files used:

- [deploy/ansible/README.md](/E:/Restart/Projects/ecommerce-project/deploy/ansible/README.md)
- [deploy/ansible/inventories](/E:/Restart/Projects/ecommerce-project/deploy/ansible/inventories)
- [deploy/ansible/group_vars/all.yml](/E:/Restart/Projects/ecommerce-project/deploy/ansible/group_vars/all.yml)

### Configure

1. choose inventory [example: `inventories/lp/hosts.yml`]
2. put real server IPs in inventory
3. update shared variables
4. confirm SSH private key works

Example inventory target:

- app host [example: `44.208.191.131`]
- SSH user [example: `ec2-user` or `ubuntu`]

### Run

Ping:

```bash
ansible -i inventories/lp/hosts.yml all -m ping
```

Apply configuration:

```bash
ansible-playbook -i inventories/lp/hosts.yml playbooks/site.yml
```

### Validate

1. Docker installed
2. app directory exists
3. Node Exporter installed if enabled
4. secrets file rendered on host if configured

## 8. Server Docker checklist

Use this as the first real internet-facing deployment path.

Files used:

- [deploy/docker/docker.env.defaults](/E:/Restart/Projects/ecommerce-project/deploy/docker/docker.env.defaults)
- [deploy/docker/docker.env.example](/E:/Restart/Projects/ecommerce-project/deploy/docker/docker.env.example)
- [deploy/docker/docker.secrets.env.example](/E:/Restart/Projects/ecommerce-project/deploy/docker/docker.secrets.env.example)
- [deploy/docker/ec2-deploy.sh](/E:/Restart/Projects/ecommerce-project/deploy/docker/ec2-deploy.sh)
- [deploy/docker/ec2-deploy.ps1](/E:/Restart/Projects/ecommerce-project/deploy/docker/ec2-deploy.ps1)

### Configure

1. clone repo on target host
2. create optional override file
3. create required secrets file
4. decide port strategy
5. confirm security group or firewall rules

Commands:

```bash
git clone <YOUR_GIT_URL> /opt/ogfashions
cd /opt/ogfashions
cp deploy/docker/docker.env.example deploy/docker/docker.env
cp deploy/docker/docker.secrets.env.example deploy/docker/docker.secrets.env
```

Important values:

- `APP_PUBLIC_BASE_URL=https://ogstyles.store` [example for `lp`]
- `APP_PUBLIC_BASE_URL=http://<server-ip>` [example for `l1`, `l2`, `l3`, `l6`]
- `MYSQL_ROOT_PASSWORD=<real value>`
- `SPRING_DATASOURCE_PASSWORD=<real value>`
- `APP_BOOTSTRAP_ADMIN_EMAIL=contact.ogfashions@gmail.com`
- `APP_BOOTSTRAP_ADMIN_PASSWORD=<real value>`
- `SPRING_MAIL_USERNAME=contact.ogfashions@gmail.com`
- `SPRING_MAIL_PASSWORD=<gmail app password>`

Port settings:

- `PORT_CONFLICT_STRATEGY=fail` [recommended for production]
- `FRONTEND_HOST_PORT=80`
- `BACKEND_HOST_PORT=8888`

If the server is shared and you do not want manual collisions:

- `PORT_CONFLICT_STRATEGY=auto` [example for test hosts]

### Run

Linux, Ubuntu, EC2, WSL, Git Bash:

```bash
bash deploy/docker/ec2-deploy.sh
```

Windows PowerShell:

```powershell
./deploy/docker/ec2-deploy.ps1
```

### Validate

Check containers:

```bash
docker ps
docker logs ogfashions-frontend --tail 100
docker logs ogfashions-backend --tail 100
docker logs ogfashions-mysql --tail 100
```

Check browser:

- `https://ogstyles.store/`
- `https://ogstyles.store/healthz`
- `https://ogstyles.store/api/actuator/health`

If domain is not ready yet:

- `http://<server-ip>/` [example]

## 9. Jenkins checklist

Use Jenkins only after you can deploy manually first.

Files used:

- [Jenkinsfile](/E:/Restart/Projects/ecommerce-project/Jenkinsfile)
- [deploy/jenkins/README.md](/E:/Restart/Projects/ecommerce-project/deploy/jenkins/README.md)
- [deploy/jenkins/PIPELINE-STAGES.md](/E:/Restart/Projects/ecommerce-project/deploy/jenkins/PIPELINE-STAGES.md)
- [deploy/jenkins/jenkins.env.example](/E:/Restart/Projects/ecommerce-project/deploy/jenkins/jenkins.env.example)

### Configure Jenkins server

1. install Git
2. install Java
3. install Maven
4. install Docker
5. install Trivy if desired
6. install `Email Extension Plugin`
7. install SSH Agent plugin if not already present
8. install Kubernetes CLI if K8s deployment is needed

### Configure SMTP

1. set SMTP host [example: `smtp.gmail.com`]
2. set SMTP port [example: `587`]
3. set username [example: `contact.ogfashions@gmail.com`]
4. set app password
5. set default recipients if desired

### Configure Jenkins credentials

Create credentials for:

1. GitHub token
2. Docker registry username and password
3. EC2 private key
4. Sonar token
5. Nexus credentials
6. kubeconfig [if using K8s]

### Configure environment values

Important items:

- `IMAGE_REPO=yarrav7` [example]
- `DEPLOY_BASE_URL=http://<lower-env-ip>` [example for `l1`, `l2`, `l3`, `l6`]
- `DEPLOY_BASE_URL=https://ogstyles.store` [example for `lp`]
- `EC2_SSH_HOST=44.208.191.131` [example]
- `EC2_SSH_USER=ec2-user` [example]
- `REMOTE_APP_DIR=/opt/ogfashions`
- `DOCKER_ENV_REMOTE_PATH=/opt/ogfashions/deploy/docker/docker.env`
- `NOTIFY_EMAIL=contact.ogfashions@gmail.com`

### Branch and parameter model

Use any branch name you want.

Examples:

- `main`
- `master`
- `release`
- `venky`
- `abcd`
- `defect-102`

Deployment is controlled by:

- `DEPLOY_TARGET`
- `DEPLOY_LEVEL`

Examples:

- validation only
  - `DEPLOY_TARGET=none`
- deploy lower environment from any branch
  - `DEPLOY_TARGET=docker`
  - `DEPLOY_LEVEL=l2`
- deploy production from any branch
  - `DEPLOY_TARGET=both`
  - `DEPLOY_LEVEL=lp`
  - manual approval still required

### Run the flow

1. push feature branch
2. validate compile and tests
3. push `master` for image build and lower-env path
4. push `release` for approval and production deployment

### Validate notifications

You should receive emails for:

- success
- failure
- unstable
- aborted

## 10. Kubernetes checklist

Use Kubernetes after Docker deployment is already understood.

Files used:

- [deploy/k8s/README.md](/E:/Restart/Projects/ecommerce-project/deploy/k8s/README.md)
- [deploy/k8s/scripts/deploy.sh](/E:/Restart/Projects/ecommerce-project/deploy/k8s/scripts/deploy.sh)
- [deploy/k8s/secrets/ogfashions-secrets.example.yaml](/E:/Restart/Projects/ecommerce-project/deploy/k8s/secrets/ogfashions-secrets.example.yaml)

### Configure cluster prerequisites

1. Kubernetes cluster exists
2. ingress controller installed
3. storage class available
4. `kubectl` access works
5. registry images already exist

### Configure secrets

1. copy the example secret manifest
2. set real values
3. set the correct namespace for the target overlay

Values to set:

- MySQL password
- datasource password
- admin password
- mail username and password

### Choose the level

Overlay examples:

- `l1` [example: shared dev]
- `l2` [example: QA]
- `l3` [example: UAT]
- `l6` [example: pre-prod]
- `lp` [example: prod]

Access examples:

- `l1`, `l2`, `l3`, `l6`
  use load balancer IP, ingress IP, or server IP
- `lp`
  use `https://ogstyles.store`

### Run

Example:

```bash
bash deploy/k8s/scripts/deploy.sh lp yarrav7 release-12
```

That means:

- overlay: `lp`
- image owner: `yarrav7`
- image tag: `release-12`

### Validate

Render before apply:

```bash
kubectl kustomize deploy/k8s/overlays/lp
```

Check namespace resources:

```bash
kubectl get all -n ogfashions-lp
```

Check logs:

```bash
kubectl logs deploy/ogfashions-backend -n ogfashions-lp
kubectl logs deploy/ogfashions-frontend -n ogfashions-lp
```

Check browser:

- `https://ogstyles.store/`
- `https://ogstyles.store/healthz`
- `https://ogstyles.store/api/actuator/health`

## 11. Monitoring checklist

Use monitoring after Docker or Kubernetes is stable.

Files used:

- [deploy/monitoring/README.md](/E:/Restart/Projects/ecommerce-project/deploy/monitoring/README.md)
- [deploy/monitoring/docker/run-monitoring.sh](/E:/Restart/Projects/ecommerce-project/deploy/monitoring/docker/run-monitoring.sh)
- [deploy/monitoring/docker/run-monitoring.ps1](/E:/Restart/Projects/ecommerce-project/deploy/monitoring/docker/run-monitoring.ps1)
- [deploy/monitoring/k8s](/E:/Restart/Projects/ecommerce-project/deploy/monitoring/k8s)

### Docker monitoring

Run:

```bash
bash deploy/monitoring/docker/run-monitoring.sh
```

Port examples:

- Prometheus `9090`
- Grafana `3000`

If these are in use, the script can move automatically.

Validate:

- Prometheus opens [example: `http://localhost:9090`]
- Grafana opens [example: `http://localhost:3000`]
- backend metrics are scraped [example: `/actuator/prometheus`]

### Kubernetes monitoring

Run:

```bash
kubectl apply -k deploy/monitoring/k8s
```

Validate:

```bash
kubectl get all -n monitoring
kubectl port-forward svc/prometheus 9090:9090 -n monitoring
kubectl port-forward svc/grafana 3000:3000 -n monitoring
```

Recommended next steps later:

- Alertmanager
- email alerts
- Slack alerts
- persistent volumes
- kube-state-metrics

## 12. OS support checklist

This repo is designed so operators can work from multiple operating systems.

### Supported operator environments

- Ubuntu
- other Linux distributions
- EC2 Linux hosts
- WSL
- Git Bash
- Windows PowerShell

### Script model

Use shell scripts on Linux-style environments:

- `.sh`

Use PowerShell wrappers on Windows:

- `.ps1`

### Important note

Production servers should still be Linux-based [example: Ubuntu 22.04 or Amazon Linux], even if operators trigger deployments from Windows.

## 13. Post-deployment validation checklist

Run this after every deployment, regardless of target.

### Application

1. open home page
2. login with real user
3. browse products
4. add item to cart
5. place a sample order if safe
6. open admin page

### Health

1. frontend health
2. backend actuator health
3. backend metrics

Examples:

- `/healthz`
- `/api/actuator/health`
- `/api/actuator/prometheus`

### Runtime

1. backend logs are clean
2. frontend logs are clean
3. database container or pod is healthy
4. email sending works if a trigger exists

### Notifications

1. Jenkins success email received
2. Jenkins failure email received for a test failure case

## 14. Recommended promotion flow

Use a fixed promotion order instead of jumping directly to production.

Recommended order:

1. local run
2. local Docker
3. `l1`
4. `l2`
5. `l3`
6. `l6`
7. `lp`

Promotion rule example:

1. only promote if health is green
2. only promote if smoke test is green
3. only promote if image tag is immutable [example: `release-19`]

## 15. Best-practice improvements you can keep using

1. use fixed image tags for promoted releases [example: `release-21`]
2. avoid `latest` in higher environments
3. keep `PORT_CONFLICT_STRATEGY=auto` only for local or shared test hosts
4. keep production on fixed ports with explicit failure
5. move production MySQL to managed RDS later
6. add HTTPS certificate automation later [example: Nginx + Let's Encrypt or ALB + ACM]
7. add Alertmanager for email alerts beyond Jenkins build emails
8. keep Jenkins notifications enabled for all final build states

## 16. Related documents

- [deploy/README.md](/E:/Restart/Projects/ecommerce-project/deploy/README.md)
- [deploy/DEVOPS-RUNBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/DEVOPS-RUNBOOK.md)
- [deploy/ORG-PLATFORM-RUNBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/ORG-PLATFORM-RUNBOOK.md)
- [deploy/local/README.md](/E:/Restart/Projects/ecommerce-project/deploy/local/README.md)
- [deploy/docker/README.md](/E:/Restart/Projects/ecommerce-project/deploy/docker/README.md)
- [deploy/terraform/README.md](/E:/Restart/Projects/ecommerce-project/deploy/terraform/README.md)
- [deploy/ansible/README.md](/E:/Restart/Projects/ecommerce-project/deploy/ansible/README.md)
- [deploy/k8s/README.md](/E:/Restart/Projects/ecommerce-project/deploy/k8s/README.md)
- [deploy/monitoring/README.md](/E:/Restart/Projects/ecommerce-project/deploy/monitoring/README.md)
- [deploy/jenkins/README.md](/E:/Restart/Projects/ecommerce-project/deploy/jenkins/README.md)
