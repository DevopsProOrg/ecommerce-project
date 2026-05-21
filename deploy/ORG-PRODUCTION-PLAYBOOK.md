# OG Fashions Org Production Playbook

This document is the practical production playbook for a real ecommerce-style rollout.

Use it when you want:

1. fewer deployment mistakes
2. preflight checks before changes
3. validation after each change
4. a promotion path from lower levels to production
5. a rollback path if something breaks

## 1. Golden deployment model

Recommended promotion path:

1. local app run
2. local Docker
3. `l1`
4. `l2`
5. `l3`
6. `l6`
7. `lp`

Do not skip directly to `lp` unless the exact image has already passed the earlier levels.

## 2. Golden release rules

Use these rules for a safer org-level flow:

1. every promoted build gets a unique image tag [example: `release-21`, `defect-102-44`]
2. never promote `latest` to higher environments
3. promote the same image tag across levels
4. do not rebuild the image separately for each level
5. run preflight checks before deployment
6. run smoke checks after deployment
7. only promote when smoke checks are green

## 3. Preflight checks before any deployment

Shared script:

- [deploy/scripts/preflight-check.sh](/E:/Restart/Projects/ecommerce-project/deploy/scripts/preflight-check.sh)

Examples:

Local:

```bash
bash deploy/scripts/preflight-check.sh local
```

Docker host:

```bash
bash deploy/scripts/preflight-check.sh docker
```

Kubernetes:

```bash
bash deploy/scripts/preflight-check.sh k8s
```

Terraform:

```bash
bash deploy/scripts/preflight-check.sh terraform
```

Ansible:

```bash
bash deploy/scripts/preflight-check.sh ansible
```

Jenkins agent:

```bash
bash deploy/scripts/preflight-check.sh jenkins
```

## 4. Smoke checks after deployment

Shared script:

- [deploy/scripts/http-smoke.sh](/E:/Restart/Projects/ecommerce-project/deploy/scripts/http-smoke.sh)

Example:

```bash
bash deploy/scripts/http-smoke.sh https://ogstyles.store
```

It checks:

1. `/`
2. `/healthz`
3. `/api/actuator/health`

## 5. Lower environment operating model

Use:

- `l1`
- `l2`
- `l3`
- `l6`

Access model:

- IP address
- Elastic IP
- load balancer address
- ingress address

Examples:

- `http://44.208.191.131`
- `http://internal-lb-123456.ap-south-1.elb.amazonaws.com`

## 6. Production operating model

Use:

- `lp`

Access model:

- `https://ogstyles.store`

Traffic model:

1. Route53 record points to ALB
2. ALB accepts internet traffic
3. ALB terminates HTTPS with ACM certificate
4. ALB forwards to app host or ingress
5. frontend serves UI
6. frontend proxies `/api` to backend

## 7. Recommended AWS production setup

For a real production-style ecommerce rollout:

1. VPC
2. public subnets
3. private subnets
4. NAT gateway
5. EC2 app host or EKS
6. internet-facing ALB
7. Route53 hosted zone
8. ACM certificate
9. security groups
10. monitoring

## 8. Recommended deployment pattern by platform

### Docker on EC2

1. Terraform creates VPC, EC2, Elastic IP, optional ALB, Route53
2. Ansible installs Docker and common packages
3. Jenkins builds and pushes images
4. Jenkins deploys the same image tag to lower level or production
5. Jenkins runs smoke checks
6. Jenkins sends email notification

### Kubernetes

1. Terraform creates VPC, EKS, optional DNS infrastructure
2. install ingress controller
3. apply secrets
4. Jenkins builds and pushes images
5. Jenkins deploys same image tag using overlay
6. rollout status is checked
7. smoke checks run against ingress URL
8. Jenkins sends email notification

## 9. Rollback pattern

### Docker rollback

Rollback idea:

1. identify previous good image tag
2. set `IMAGE_TAG` to that value
3. redeploy the registry-based Docker flow

Example:

- current failed image: `release-30`
- rollback image: `release-29`

### Kubernetes rollback

Rollback idea:

1. set previous image tag with the deploy script
2. or use `kubectl rollout undo` if appropriate for your process

Preferred org-level rule:

- rollback by reapplying the previous known-good immutable image tag

## 10. Production readiness checklist

Before `lp`, confirm:

1. lower levels passed
2. smoke checks passed
3. SMTP notifications work
4. Docker registry images exist
5. ALB is healthy
6. Route53 record resolves
7. TLS certificate is valid
8. security groups allow required traffic only
9. monitoring is live
10. rollback image tag is known

## 11. Real-time operations checklist

During production deployment:

1. watch Jenkins build
2. watch Docker logs or pod rollout
3. run smoke check
4. test browser home page
5. test login
6. test product listing
7. test add to cart
8. confirm email notifications

After production deployment:

1. check Grafana dashboards
2. check Prometheus targets
3. check Jenkins success email
4. verify no 5xx spikes
5. keep previous image tag ready for rollback

## 12. Best-practice enhancements already in this repo

1. port conflict handling for local and monitoring Docker flows
2. parameter-driven Jenkins deployment
3. production approval gate for `lp`
4. Docker and Kubernetes deployment paths
5. Prometheus and Grafana support
6. build status email notifications
7. preflight and smoke-check scripts

## 13. Related docs

- [deploy/DEPLOYMENT-CHECKLISTS.md](/E:/Restart/Projects/ecommerce-project/deploy/DEPLOYMENT-CHECKLISTS.md)
- [deploy/DEVOPS-RUNBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/DEVOPS-RUNBOOK.md)
- [deploy/ORG-PLATFORM-RUNBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/ORG-PLATFORM-RUNBOOK.md)
- [deploy/terraform/README.md](/E:/Restart/Projects/ecommerce-project/deploy/terraform/README.md)
- [deploy/jenkins/README.md](/E:/Restart/Projects/ecommerce-project/deploy/jenkins/README.md)
