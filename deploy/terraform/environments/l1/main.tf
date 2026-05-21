module "platform" {
  source = "../../stacks/org-platform"

  org_name         = var.org_name
  project_name     = var.project_name
  environment_code = "l1"
  aws_region       = var.aws_region
  vpc_cidr         = var.vpc_cidr
  public_subnets   = var.public_subnets
  private_subnets  = var.private_subnets
  domain_name      = var.domain_name
  hosted_zone_id   = var.hosted_zone_id
  app_subdomain    = ""
  create_public_dns_record = false
  enable_alb       = false
  ssh_ingress_cidrs = var.ssh_ingress_cidrs
  monitoring_ingress_cidrs = var.monitoring_ingress_cidrs
  backend_health_ingress_cidrs = var.backend_health_ingress_cidrs
  create_app_host  = true
  create_eks       = false
  key_pair_name    = var.key_pair_name
}
