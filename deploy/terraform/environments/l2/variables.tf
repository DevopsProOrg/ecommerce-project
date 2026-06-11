variable "org_name" { type = string default = "ogstyles" }
variable "project_name" { type = string default = "ogfashions" }
variable "aws_region" { type = string default = "us-east-1" }
variable "domain_name" { type = string default = "ogstyles.store" }
variable "hosted_zone_id" { type = string default = null }
variable "key_pair_name" { type = string default = null }
variable "ssh_ingress_cidrs" { type = list(string) default = [] }
variable "monitoring_ingress_cidrs" { type = list(string) default = [] }
variable "backend_health_ingress_cidrs" { type = list(string) default = [] }
variable "create_rds" { type = bool default = false }
variable "db_name" { type = string default = "ecommerce" }
variable "db_username" { type = string default = "root" }
variable "db_password" { type = string default = null }
variable "db_instance_class" { type = string default = "db.t3.micro" }
variable "db_allocated_storage" { type = number default = 20 }
variable "db_ingress_cidrs" { type = list(string) default = [] }
variable "vpc_cidr" { type = string default = "10.12.0.0/16" }
variable "public_subnets" { type = list(string) default = ["10.12.1.0/24", "10.12.2.0/24"] }
variable "private_subnets" { type = list(string) default = ["10.12.11.0/24", "10.12.12.0/24"] }
