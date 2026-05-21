variable "org_name" {
  type        = string
  description = "Organization or business name."
}

variable "project_name" {
  type        = string
  description = "Project name."
}

variable "environment_code" {
  type        = string
  description = "Environment level code such as l1, l2, l3, l6, or lp."
}

variable "aws_region" {
  type        = string
  description = "AWS region."
}

variable "vpc_cidr" {
  type        = string
  description = "VPC CIDR block."
}

variable "public_subnets" {
  type        = list(string)
  description = "Public subnet CIDRs."
}

variable "private_subnets" {
  type        = list(string)
  description = "Private subnet CIDRs."
}

variable "availability_zones" {
  type        = list(string)
  description = "Availability zones."
  default     = []
}

variable "domain_name" {
  type        = string
  description = "Base public domain."
  default     = "ogstyles.store"
}

variable "hosted_zone_id" {
  type        = string
  description = "Route53 hosted zone ID."
  default     = null
}

variable "app_subdomain" {
  type        = string
  description = "Subdomain label for the environment."
  default     = ""
}

variable "create_www_record" {
  type        = bool
  description = "Whether to create a www alias for production."
  default     = false
}

variable "ssh_ingress_cidrs" {
  type        = list(string)
  description = "CIDR ranges allowed to SSH to the app host."
  default     = []
}

variable "public_app_ingress_cidrs" {
  type        = list(string)
  description = "CIDR ranges allowed to reach the app host directly over HTTP when no ALB is used."
  default     = ["0.0.0.0/0"]
}

variable "backend_health_ingress_cidrs" {
  type        = list(string)
  description = "CIDR ranges allowed to reach the backend port directly."
  default     = []
}

variable "monitoring_ingress_cidrs" {
  type        = list(string)
  description = "CIDR ranges allowed to reach Prometheus and Grafana on the app host."
  default     = []
}

variable "enable_alb" {
  type        = bool
  description = "Whether to create an internet-facing Application Load Balancer."
  default     = false
}

variable "alb_certificate_arn" {
  type        = string
  description = "ACM certificate ARN for HTTPS listener on the ALB."
  default     = null
}

variable "create_public_dns_record" {
  type        = bool
  description = "Whether to create a Route53 alias for the application FQDN."
  default     = false
}

variable "create_app_host" {
  type        = bool
  description = "Whether to create an EC2 app host."
  default     = true
}

variable "create_eks" {
  type        = bool
  description = "Whether to create an EKS cluster."
  default     = false
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type for the app host."
  default     = "t3.large"
}

variable "key_pair_name" {
  type        = string
  description = "Existing EC2 key pair name."
  default     = null
}

variable "eks_cluster_version" {
  type        = string
  description = "Kubernetes version for EKS."
  default     = "1.30"
}

variable "tags" {
  type        = map(string)
  description = "Additional tags."
  default     = {}
}
