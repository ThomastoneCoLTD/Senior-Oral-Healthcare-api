variable "aws_region" {
  type = string
}

variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "account_id" {
  type = string
}

variable "vpc_cidr" {
  type = string
}

variable "public_subnet_cidrs" {
  type = list(string)
}

variable "private_app_subnet_cidrs" {
  type = list(string)
}

variable "private_db_subnet_cidrs" {
  type    = list(string)
  default = []
}

variable "single_nat_gateway" {
  type    = bool
  default = true
}

variable "artifact_bucket" {
  type = string
}

variable "artifact_prefix" {
  type = string
}

variable "release_type" {
  type = string
}

variable "spring_profile" {
  type = string
}

variable "app_port" {
  type = number
}

variable "health_path" {
  type = string
}

variable "alb_name" {
  type = string
}

variable "target_group_name" {
  type = string
}

variable "launch_template_name" {
  type = string
}

variable "asg_name" {
  type = string
}

variable "desired_capacity" {
  type = number
}

variable "min_size" {
  type = number
}

variable "max_size" {
  type = number
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

variable "ami_id" {
  type    = string
  default = ""
}

variable "certificate_arn" {
  type = string
}

variable "hosted_zone_name" {
  type = string
}

variable "origin_domain_name" {
  type = string
}

variable "create_route53_record" {
  type    = bool
  default = false
}

variable "enable_http_redirect" {
  type    = bool
  default = true
}

variable "enable_ssm" {
  type    = bool
  default = true
}
