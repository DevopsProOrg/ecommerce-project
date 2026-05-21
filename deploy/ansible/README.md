# Ansible Configuration Management

This Ansible layout configures servers after Terraform provisioning.

## Step-by-step

### Step 1. Pick the inventory

Available inventories:

- `inventories/l1`
- `inventories/l2`
- `inventories/l3`
- `inventories/l6`
- `inventories/lp`

### Step 2. Put the real host IPs into the inventory

Example:

```bash
cd deploy/ansible
vi inventories/lp/hosts.yml
```

Use:

- real server IPs for `l1`, `l2`, `l3`, and `l6`
- production IP or production DNS for `lp`

### Step 3. Review shared variables

Edit:

- `group_vars/all.yml`

Set:

- app directory
- docker settings
- secrets-related values
- monitoring options

### Step 4. Check SSH access

```bash
ansible -i inventories/lp/hosts.yml all -m ping
```

### Step 5. Run the full playbook

```bash
ansible-playbook -i inventories/lp/hosts.yml playbooks/site.yml
```

### Step 6. What this configures

The playbooks can:

- install common packages
- install Docker
- prepare the ecommerce app host
- write server-local Docker secrets
- install Node Exporter

### Step 7. Deploy the app

After Ansible finishes, run Docker or Jenkins deployment on the configured host.
