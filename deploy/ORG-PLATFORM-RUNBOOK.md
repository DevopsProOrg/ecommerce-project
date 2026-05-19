# OG Styles Platform Runbook

This is the end-to-end deployment path for the ecommerce platform.

## 1. Local Docker

Use this when you want a full local test with MySQL, backend, and frontend containers.

```bash
cp deploy/local/local-docker.env.example deploy/local/local-docker.env
cp deploy/local/local-docker.secrets.env.example deploy/local/local-docker.secrets.env
bash deploy/local/local-docker.sh
```

Test:

- `http://localhost:8080/`
- `http://localhost:8080/healthz`
- `http://localhost:8080/api/actuator/health`

## 2. Docker on EC2

Use this for the first live internet-facing environment.

```bash
cp deploy/docker/docker.secrets.env.example deploy/docker/docker.secrets.env
vi deploy/docker/docker.secrets.env
bash deploy/docker/ec2-deploy.sh
```

Test:

- `https://ogstyles.store/`
- `https://ogstyles.store/healthz`
- `https://ogstyles.store/api/actuator/health`

## 3. Kubernetes

Pick one of the organization levels:

- `l1`
- `l2`
- `l3`
- `l6`
- `lp`

Create the matching secret first, then deploy:

```bash
kubectl apply -f deploy/k8s/secrets/ogfashions-secrets.example.yaml
bash deploy/k8s/scripts/deploy.sh lp yarrav7 release-20
```

## 4. Terraform

Provision infrastructure before Ansible or Kubernetes setup:

```bash
cd deploy/terraform/environments/lp
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform apply
```

## 5. Ansible

Configure the provisioned hosts:

```bash
cd deploy/ansible
ansible-playbook -i inventories/lp/hosts.yml playbooks/site.yml
```

## 6. Jenkins

Pipeline flow:

1. compile and test
2. validate frontend JavaScript
3. build Docker images
4. optionally scan with Trivy
5. push images
6. deploy Docker or Kubernetes by branch policy
7. run smoke checks

## 7. Monitoring

### Docker / EC2 monitoring

```bash
bash deploy/monitoring/docker/run-monitoring.sh
```

Access:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

### Kubernetes monitoring

```bash
kubectl apply -k deploy/monitoring/k8s
```

## 8. Recommended rollout sequence

1. local Docker
2. `l1`
3. `l2`
4. `l3`
5. `l6`
6. `lp`

## 9. Application validation checklist

1. home page loads
2. login works
3. product listing loads
4. cart operations work
5. order placement works
6. admin page loads
7. contact requests persist
8. backend health is `UP`
9. Prometheus receives backend metrics
