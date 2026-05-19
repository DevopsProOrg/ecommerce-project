provider "aws" {
  region = var.aws_region
}

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  name_prefix = "${var.project_name}-${var.environment_code}"
  azs         = length(var.availability_zones) > 0 ? var.availability_zones : slice(data.aws_availability_zones.available.names, 0, 2)
  common_tags = merge(
    {
      Project     = var.project_name
      Environment = var.environment_code
      ManagedBy   = "terraform"
      Org         = var.org_name
    },
    var.tags
  )
  fqdn = trim(var.app_subdomain) == "" ? var.domain_name : "${var.app_subdomain}.${var.domain_name}"
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.8"

  name = local.name_prefix
  cidr = var.vpc_cidr

  azs             = local.azs
  public_subnets  = var.public_subnets
  private_subnets = var.private_subnets

  enable_nat_gateway     = true
  single_nat_gateway     = true
  one_nat_gateway_per_az = false

  tags = local.common_tags
}

resource "aws_security_group" "app_host" {
  count       = var.create_app_host ? 1 : 0
  name        = "${local.name_prefix}-app-host"
  description = "Security group for the ecommerce app host"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Backend health"
    from_port   = 8888
    to_port     = 8888
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Grafana"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Prometheus"
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-x86_64"]
  }
}

resource "aws_instance" "app_host" {
  count                       = var.create_app_host ? 1 : 0
  ami                         = data.aws_ami.amazon_linux_2023.id
  instance_type               = var.instance_type
  subnet_id                   = module.vpc.public_subnets[0]
  vpc_security_group_ids      = [aws_security_group.app_host[0].id]
  associate_public_ip_address = true
  key_name                    = var.key_pair_name

  root_block_device {
    volume_size = 40
    volume_type = "gp3"
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-app-host"
  })
}

resource "aws_eip" "app_host" {
  count    = var.create_app_host ? 1 : 0
  instance = aws_instance.app_host[0].id
  domain   = "vpc"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-eip"
  })
}

module "eks" {
  count   = var.create_eks ? 1 : 0
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.15"

  cluster_name    = "${local.name_prefix}-eks"
  cluster_version = var.eks_cluster_version

  cluster_endpoint_public_access = true

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    default = {
      min_size       = 2
      max_size       = 4
      desired_size   = 2
      instance_types = ["t3.large"]
      capacity_type  = "ON_DEMAND"
    }
  }

  tags = local.common_tags
}

resource "aws_route53_record" "app_host" {
  count   = var.hosted_zone_id != null && var.create_app_host ? 1 : 0
  zone_id = var.hosted_zone_id
  name    = local.fqdn
  type    = "A"
  ttl     = 300
  records = [aws_eip.app_host[0].public_ip]
}

resource "aws_route53_record" "www" {
  count   = var.hosted_zone_id != null && var.create_www_record && var.create_app_host ? 1 : 0
  zone_id = var.hosted_zone_id
  name    = "www.${var.domain_name}"
  type    = "CNAME"
  ttl     = 300
  records = [local.fqdn]
}
