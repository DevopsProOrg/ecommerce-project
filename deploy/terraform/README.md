# Terraform Infrastructure

This structure provisions organization-style infrastructure for the deployment levels:

- `l1`
- `l2`
- `l3`
- `l6`
- `lp`

## Layout

- `stacks/org-platform`
  - reusable AWS platform stack
- `environments/l1`
- `environments/l2`
- `environments/l3`
- `environments/l6`
- `environments/lp`
  - environment-specific entrypoints and sample tfvars

## What the platform stack can create

- VPC
- public and private subnets
- NAT gateway
- EC2 app host
- Elastic IP for the app host
- security groups
- optional EKS cluster
- optional Route53 records

## Typical pattern

- `l1`, `l2`
  - EC2 app host for fast iteration
- `l3`, `l6`, `lp`
  - EKS-focused environments

## Usage

```bash
cd deploy/terraform/environments/lp
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```
