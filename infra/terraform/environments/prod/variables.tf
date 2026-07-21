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
  default = "t3.medium"
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

variable "db_identifier" {
  type = string
}

variable "db_engine" {
  type    = string
  default = "mysql"
}

variable "db_engine_version" {
  type    = string
  default = "8.0"
}

variable "db_instance_class" {
  type    = string
  default = "db.t3.small"
}

variable "db_allocated_storage" {
  type    = number
  default = 20
}

variable "db_max_allocated_storage" {
  type    = number
  default = 100
}

variable "db_storage_type" {
  type    = string
  default = "gp3"
}

variable "db_storage_encrypted" {
  type    = bool
  default = true
}

variable "db_name" {
  type = string
}

variable "db_username" {
  type = string
}

variable "db_port" {
  type    = number
  default = 3306
}

variable "db_multi_az" {
  type    = bool
  default = false
}

variable "db_backup_retention_period" {
  type    = number
  default = 7
}

variable "db_backup_window" {
  type    = string
  default = "18:00-19:00"
}

variable "db_maintenance_window" {
  type    = string
  default = "sun:19:00-sun:20:00"
}

variable "db_auto_minor_version_upgrade" {
  type    = bool
  default = true
}

variable "db_deletion_protection" {
  type    = bool
  default = true
}

variable "db_skip_final_snapshot" {
  type    = bool
  default = false
}

variable "db_final_snapshot_identifier" {
  type    = string
  default = null
}

variable "db_apply_immediately" {
  type    = bool
  default = false
}
