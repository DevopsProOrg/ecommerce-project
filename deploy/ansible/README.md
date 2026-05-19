# Ansible Configuration Management

This Ansible layout configures hosts after Terraform provisioning.

## Layout

- `inventories/l1`
- `inventories/l2`
- `inventories/l3`
- `inventories/l6`
- `inventories/lp`
- `playbooks/`
- `roles/`

## Typical usage

```bash
cd deploy/ansible
ansible-playbook -i inventories/lp/hosts.yml playbooks/site.yml
```

## What the playbooks do

- install common packages
- install Docker
- prepare the ecommerce app host
- write server-local Docker secrets
- optionally install Node Exporter for monitoring
