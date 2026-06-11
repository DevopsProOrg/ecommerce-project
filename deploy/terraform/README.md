# Terraform Infrastructure

This Terraform layout is for creating AWS resources level by level.

It supports:

- `l1`
- `l2`
- `l3`
- `l6`
- `lp`

## 1. What Terraform can create

The shared stack in [stacks/org-platform](stacks/org-platform) can create:

- VPC
- public subnets
- private subnets
- NAT gateway
- EC2 application host
- Elastic IP
- optional EKS cluster
- optional internet-facing Application Load Balancer
- optional Route53 records
- optional RDS for MySQL

## 2. Environment model

Recommended usage in this project:

- `l1`, `l2`
  lower Docker or EC2-style environments, usually without public domain
- `l3`, `l6`
  higher test or pre-production environments, often Kubernetes-oriented
- `lp`
  production

Access model:

- `l1`, `l2`, `l3`, `l6`
  use public IP, Elastic IP, ingress address, or load balancer address
- `lp`
  use `ogstyles.store`

## 3. What to do first

Before running Terraform, prepare these AWS items:

1. AWS account access
2. AWS CLI or credentials configured
3. region chosen [example: `ap-south-1`]
4. EC2 key pair created [example: `ogstyles-lp-key`]
5. Route53 hosted zone ready if production DNS is needed
6. ACM certificate created if production HTTPS ALB is needed
7. decide whether you want container MySQL or managed RDS

Production extras:

1. domain `ogstyles.store` available in Route53 or delegated there
2. ACM certificate issued for `ogstyles.store`
3. optionally certificate also covers `www.ogstyles.store`

## 4. Choose the target environment

Examples:

- [environments/l1](environments/l1)
- [environments/l2](environments/l2)
- [environments/l3](environments/l3)
- [environments/l6](environments/l6)
- [environments/lp](environments/lp)

## 5. Step-by-step for lower levels

Use this flow for `l1`, `l2`, `l3`, or `l6`.

### Step 1. Open the environment folder

Example:

```bash
cd deploy/terraform/environments/l1
```

### Step 2. Create the tfvars file

```bash
cp terraform.tfvars.example terraform.tfvars
```

### Step 3. Fill the values

Typical values:

- `hosted_zone_id = null`
- `key_pair_name = "ogstyles-l1-key"` [example]
- `ssh_ingress_cidrs = ["203.0.113.10/32"]` [example]
- `monitoring_ingress_cidrs = ["203.0.113.10/32"]` [example]

The environment variables file already contains:

- environment CIDRs
- default region
- domain placeholder

### Step 4. Run Terraform

```bash
terraform init
terraform plan
terraform apply
```

### Step 5. Save the outputs

Keep:

- VPC ID
- app host public IP
- app URL
- generated Ansible inventory snippet

### Step 6. Use the outputs

Lower-level examples:

- `http://<elastic-ip>/`
- `http://<elastic-ip>/healthz`
- `http://<elastic-ip>/api/actuator/health`

If you enable RDS, also save:

- RDS endpoint
- generated `spring_datasource_url`

## 6. Step-by-step for production

Use this flow for `lp`.

### Step 1. Prepare DNS and certificate

You need:

1. Route53 hosted zone for `ogstyles.store`
2. ACM certificate ARN for `ogstyles.store`
3. EC2 key pair

### Step 2. Open the production folder

```bash
cd deploy/terraform/environments/lp
```

### Step 3. Create the tfvars file

```bash
cp terraform.tfvars.example terraform.tfvars
```

### Step 4. Fill the production values

Examples:

```hcl
hosted_zone_id      = "ZXXXXXXXXXXXXX"
key_pair_name       = "ogstyles-lp-key"
alb_certificate_arn = "arn:aws:acm:ap-south-1:123456789012:certificate/replace-me"
ssh_ingress_cidrs   = ["203.0.113.10/32"]
monitoring_ingress_cidrs = ["203.0.113.10/32"]
```

### Step 5. Run Terraform

```bash
terraform init
terraform plan
terraform apply
```

### Step 6. What production creates

The `lp` environment now defaults to:

- EC2 app host
- Elastic IP
- EKS cluster
- internet-facing ALB
- public DNS record
- `www` alias if enabled
- optional MySQL RDS instance

### Step 6A. Understand the new security defaults

The stack now behaves more safely:

1. SSH stays closed unless you explicitly allow CIDRs
2. Prometheus and Grafana stay closed unless you explicitly allow CIDRs
3. backend port `8888` stays closed unless you explicitly allow CIDRs
4. when ALB is enabled, the instance accepts app traffic from the ALB security group instead of the internet

### Step 7. Production traffic flow

Typical flow:

1. browser opens `https://ogstyles.store`
2. Route53 points to ALB
3. ALB terminates HTTPS
4. ALB forwards traffic to EC2 app host on port `80`
5. frontend container serves UI
6. frontend proxies `/api/*` to backend container

## 7. Load balancer notes

The Terraform stack now supports:

- HTTP listener on port `80`
- HTTPS listener on port `443` when `alb_certificate_arn` is set
- redirect from `80` to `443` when certificate is configured
- target group health check on `/healthz`

This makes the `lp` stack suitable for a production-style internet-facing ecommerce path.

## 8. After Terraform

Run these next:

1. Ansible
2. Docker deploy or Jenkins deploy
3. production validation

If you created RDS, set Docker or application runtime to:

- `USE_EXTERNAL_DB=true`
- `SPRING_DATASOURCE_URL=<terraform-output-spring-datasource-url>`

Suggested order:

1. [../ansible/README.md](../ansible/README.md)
2. [../docker/README.md](../docker/README.md)
3. [../jenkins/README.md](../jenkins/README.md)

## 9. Real-world production checklist

For `lp`, confirm all of these:

1. ALB DNS name exists
2. Route53 record points to ALB
3. HTTPS certificate is valid
4. security groups allow `80` and `443`
5. EC2 app host is healthy
6. backend health works through frontend
7. Jenkins `DEPLOY_BASE_URL` is set to `https://ogstyles.store`

## 10. Related files

- [stacks/org-platform/main.tf](stacks/org-platform/main.tf)
- [stacks/org-platform/variables.tf](stacks/org-platform/variables.tf)
- [stacks/org-platform/outputs.tf](stacks/org-platform/outputs.tf)
- [../START-HERE.md](../START-HERE.md)
