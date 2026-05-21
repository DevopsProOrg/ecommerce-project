variable "org_name" { type = string default = "ogstyles" }
variable "project_name" { type = string default = "ogfashions" }
variable "aws_region" { type = string default = "us-east-1" }
variable "domain_name" { type = string default = "ogstyles.store" }
variable "hosted_zone_id" { type = string default = null }
variable "key_pair_name" { type = string default = null }
variable "vpc_cidr" { type = string default = "10.11.0.0/16" }
variable "public_subnets" { type = list(string) default = ["10.11.1.0/24", "10.11.2.0/24"] }
variable "private_subnets" { type = list(string) default = ["10.11.11.0/24", "10.11.12.0/24"] }
