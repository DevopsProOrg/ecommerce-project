module "platform" {
  source = "../../stacks/org-platform"

  org_name         = var.org_name
  project_name     = var.project_name
  environment_code = "l6"
  aws_region       = var.aws_region
  vpc_cidr         = var.vpc_cidr
  public_subnets   = var.public_subnets
  private_subnets  = var.private_subnets
  domain_name      = var.domain_name
  hosted_zone_id   = var.hosted_zone_id
  app_subdomain    = "l6"
  create_app_host  = false
  create_eks       = true
  key_pair_name    = var.key_pair_name
}
