output "vpc_id" {
  value = module.vpc.vpc_id
}

output "app_host_public_ip" {
  value = var.create_app_host ? aws_eip.app_host[0].public_ip : null
}

output "app_url" {
  value = var.create_app_host ? "http://${local.fqdn}" : null
}

output "eks_cluster_name" {
  value = var.create_eks ? module.eks[0].cluster_name : null
}

output "eks_cluster_endpoint" {
  value = var.create_eks ? module.eks[0].cluster_endpoint : null
}

output "ansible_inventory_app_host" {
  value = var.create_app_host ? <<-EOT
    [app_hosts]
    ${local.fqdn} ansible_host=${aws_eip.app_host[0].public_ip}
  EOT : null
}
