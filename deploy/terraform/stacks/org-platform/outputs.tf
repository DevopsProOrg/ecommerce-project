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

output "rds_endpoint" {
  value = var.create_rds ? aws_db_instance.app[0].address : null
}

output "rds_port" {
  value = var.create_rds ? aws_db_instance.app[0].port : null
}

output "spring_datasource_url" {
  value     = var.create_rds ? "jdbc:mysql://${aws_db_instance.app[0].address}:${aws_db_instance.app[0].port}/${var.db_name}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" : null
  sensitive = true
}

output "ansible_inventory_app_host" {
  value = var.create_app_host ? <<-EOT
    [app_hosts]
    ${var.create_public_dns_record ? local.fqdn : aws_eip.app_host[0].public_ip} ansible_host=${aws_eip.app_host[0].public_ip}
  EOT : null
}
