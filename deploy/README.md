# OG Fashions Deployment Documentation

This folder has many reference files, but there is only one main starting point:

## Read This First

1. [START-HERE.md](./START-HERE.md)

If you are confused, stop reading other files and go back to [START-HERE.md](./START-HERE.md).

## Why There Are Multiple README Files

The project is split like a real DevOps repository. Each area owns its own short reference:

| Area | Reference |
| --- | --- |
| Local Docker | [local/README.md](./local/README.md) |
| EC2 or Ubuntu Docker | [docker/README.md](./docker/README.md) |
| Terraform AWS infra | [terraform/README.md](./terraform/README.md) |
| Ansible server config | [ansible/README.md](./ansible/README.md) |
| Jenkins CI/CD | [jenkins/README.md](./jenkins/README.md) |
| Jenkins stages | [jenkins/PIPELINE-STAGES.md](./jenkins/PIPELINE-STAGES.md) |
| Kubernetes | [k8s/README.md](./k8s/README.md) |
| Monitoring and alerts | [monitoring/README.md](./monitoring/README.md) |
| Nexus and Artifactory | [artifact-repositories/README.md](./artifact-repositories/README.md) |

## Simple Deployment Order

Use this order:

1. Local validation
2. Local Docker
3. Terraform infrastructure
4. Ansible server configuration
5. Docker deployment on Ubuntu/EC2
6. Monitoring and alerts
7. Jenkins CI/CD
8. Kubernetes when Docker is stable
9. Production promotion to `lp`

For detailed commands, environment variables, Jenkins parameters, monitoring setup, and rollback steps, use [START-HERE.md](./START-HERE.md).

## Environment Rule

- `l1`, `l2`, `l3`, `l6`: use IP address or load balancer address.
- `lp`: use `https://ogstyles.store`.

## Image Tag Rule

Use version tags:

```text
v1.0
v1.1
v1.2
```

Do not use `latest` for real deployments.
