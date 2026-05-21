# OG Fashions DevOps Operations Handbook

This is the day-to-day DevOps handbook for this project.

Use it as the operational guide for:

1. first-time setup
2. environment ownership
3. build and release flow
4. deployment validation
5. rollback
6. production support
7. monitoring checks
8. incident handling

## 1. Your role in this project

As the DevOps owner of this project, your responsibilities are:

1. keep build and deployment flows stable
2. provision and maintain environments
3. manage secrets and credentials safely
4. keep deployment paths repeatable
5. monitor availability and health
6. support releases and rollback if needed
7. keep documentation current

## 2. First-time setup sequence

Follow this order when setting up from scratch:

1. review [ENVIRONMENT-MATRIX.md](/E:/Restart/Projects/ecommerce-project/deploy/ENVIRONMENT-MATRIX.md)
2. review [DEPLOYMENT-CHECKLISTS.md](/E:/Restart/Projects/ecommerce-project/deploy/DEPLOYMENT-CHECKLISTS.md)
3. configure local run
4. configure local Docker
5. provision AWS with Terraform
6. configure servers with Ansible
7. run Docker deployment on lower level
8. configure Jenkins
9. configure monitoring
10. promote through levels
11. deploy production

## 3. Daily operating workflow

Use this as the normal DevOps daily pattern:

### Start of day

1. check Jenkins last build status
2. check Grafana dashboards
3. check Prometheus targets
4. check production site opens
5. check latest deployment email notifications

### Before a new deployment

1. confirm target level
2. confirm image tag
3. run preflight check
4. confirm secrets and credentials exist
5. confirm rollback image tag is known

### After a deployment

1. run smoke check
2. open the browser URL
3. confirm backend health
4. confirm logs are clean
5. confirm Jenkins notification mail arrived

## 4. Build and release process

### Branch workflow

Branch names can be anything.

Examples:

- `main`
- `master`
- `release`
- `venky`
- `feature-cart-fix`
- `defect-102`

### Jenkins rules

1. any branch can validate
2. any branch can deploy if parameters are selected
3. `DEPLOY_LEVEL=lp` requires approval
4. lower levels do not require approval

### Standard promotion flow

1. build image once
2. tag image uniquely
3. deploy same image tag to `l1`
4. validate
5. deploy same image tag to `l2`
6. validate
7. deploy same image tag to `l3`
8. validate
9. deploy same image tag to `l6`
10. validate
11. deploy same image tag to `lp`

## 5. Local validation process

### Local app run

1. run backend
2. serve frontend
3. test login
4. test product list
5. test cart

### Local Docker

1. create local env files
2. run preflight
3. run local Docker
4. validate frontend and backend health

Commands:

```bash
bash deploy/scripts/preflight-check.sh local
bash deploy/local/local-docker.sh
bash deploy/scripts/http-smoke.sh http://localhost:8081
```

## 6. Lower environment deployment process

Use this for `l1`, `l2`, `l3`, and `l6`.

### Infrastructure

1. Terraform apply for target level
2. save outputs
3. update Ansible inventory
4. run Ansible

### Application deployment

1. build and push image from Jenkins
2. deploy to Docker or K8s
3. run smoke checks
4. validate browser access

### Example lower-level validation

1. home page opens
2. admin page opens
3. login works
4. cart works
5. actuator health is `UP`

## 7. Production deployment process

Use this only for `lp`.

### Infrastructure prerequisites

1. Route53 hosted zone ready
2. ACM certificate ready
3. ALB created or ready to be created by Terraform
4. security groups reviewed
5. production secrets reviewed

### Production release steps

1. confirm image tag has passed `l6`
2. confirm Jenkins parameters
3. confirm `DEPLOY_BASE_URL=https://ogstyles.store`
4. trigger deployment
5. approve production in Jenkins
6. watch rollout
7. run smoke checks
8. run browser validation
9. monitor Grafana after release

### Browser validation for production

1. open `https://ogstyles.store/`
2. open `https://ogstyles.store/healthz`
3. open `https://ogstyles.store/api/actuator/health`
4. test login
5. test product list
6. test add to cart

## 8. Rollback process

### Docker rollback

1. identify previous good image tag
2. redeploy with previous image tag
3. run smoke checks
4. validate browser

### Kubernetes rollback

1. reapply previous image tag through deploy script
2. wait for rollout
3. run smoke checks
4. validate browser

### Rollback rule

Never rebuild during rollback.

Always redeploy the previous known-good immutable image tag.

## 9. Monitoring operations

### What to watch

1. application availability
2. health endpoint stability
3. Prometheus target status
4. Grafana dashboards
5. build emails

### Minimum checks after deployment

1. Prometheus target is up
2. Grafana opens
3. backend metrics are still scraped
4. no obvious spike in failures

## 10. Incident handling

Use this basic incident flow:

1. identify impacted environment
2. confirm if app is down or degraded
3. check Jenkins for last deployment
4. check Docker logs or pod logs
5. check health endpoints
6. check load balancer or ingress reachability
7. rollback if needed
8. document the issue and fix

## 11. Checklist before marking a deployment successful

1. preflight passed
2. deployment command succeeded
3. smoke checks passed
4. browser validation passed
5. logs are clean
6. notifications sent
7. monitoring looks healthy

## 12. Documents you should keep using

1. [README.md](/E:/Restart/Projects/ecommerce-project/README.md)
2. [deploy/README.md](/E:/Restart/Projects/ecommerce-project/deploy/README.md)
3. [deploy/DEPLOYMENT-CHECKLISTS.md](/E:/Restart/Projects/ecommerce-project/deploy/DEPLOYMENT-CHECKLISTS.md)
4. [deploy/ENVIRONMENT-MATRIX.md](/E:/Restart/Projects/ecommerce-project/deploy/ENVIRONMENT-MATRIX.md)
5. [deploy/ORG-PRODUCTION-PLAYBOOK.md](/E:/Restart/Projects/ecommerce-project/deploy/ORG-PRODUCTION-PLAYBOOK.md)
6. [deploy/terraform/README.md](/E:/Restart/Projects/ecommerce-project/deploy/terraform/README.md)
7. [deploy/jenkins/README.md](/E:/Restart/Projects/ecommerce-project/deploy/jenkins/README.md)

## 13. Practical owner reminder

For this project, the safest real-world habit is:

1. build once
2. promote the same tag
3. validate every level
4. approve only production
5. keep rollback ready
