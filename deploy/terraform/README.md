# Terraform Infrastructure

This structure provisions organization-style AWS infrastructure for:

- `l1`
- `l2`
- `l3`
- `l6`
- `lp`

## Step-by-step

### Step 1. Choose the target level

Use:

- `l1`, `l2` for faster EC2-style rollout
- `l3`, `l6`, `lp` for bigger or Kubernetes-oriented rollout

### Step 2. Open the environment folder

Example:

```bash
cd deploy/terraform/environments/lp
```

### Step 3. Create the tfvars file

```bash
cp terraform.tfvars.example terraform.tfvars
```

### Step 4. Fill real infrastructure values

Update:

- AWS region
- VPC CIDR
- subnet CIDRs
- instance size
- domain settings
- Route53 zone details
- whether EKS should be enabled

### Step 5. Initialize Terraform

```bash
terraform init
```

### Step 6. Review the plan

```bash
terraform plan
```

### Step 7. Apply the infrastructure

```bash
terraform apply
```

### Step 8. Save the outputs

Capture:

- EC2 public IP
- Elastic IP
- EKS cluster details
- Route53 outputs

### Step 9. Move to Ansible

After infra exists, use Ansible to configure the hosts.
