output "vpc_id" {
  value = module.vpc.vpc_id
}

output "app_host_public_ip" {
  value = var.create_app_host ? aws_eip.app_host[0].public_ip : null
}

output "app_url" {
  value = var.enable_alb ? (
    var.create_public_dns_record ? (
      var.alb_certificate_arn != null ? "https://${local.fqdn}" : "http://${local.fqdn}"
    ) : (
      var.alb_certificate_arn != null ? "https://${aws_lb.app[0].dns_name}" : "http://${aws_lb.app[0].dns_name}"
    )
  ) : (
    var.create_public_dns_record ? "http://${local.fqdn}" : (var.create_app_host ? "http://${aws_eip.app_host[0].public_ip}" : null)
  )
}

output "alb_dns_name" {
  value = var.enable_alb ? aws_lb.app[0].dns_name : null
}

output "alb_zone_id" {
  value = var.enable_alb ? aws_lb.app[0].zone_id : null
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
    ${var.create_public_dns_record ? local.fqdn : aws_eip.app_host[0].public_ip} ansible_host=${aws_eip.app_host[0].public_ip}
  EOT : null
}
