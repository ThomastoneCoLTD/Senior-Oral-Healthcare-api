variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "db_subnet_ids" {
  type = list(string)

  validation {
    condition     = length(var.db_subnet_ids) >= 2
    error_message = "RDS requires at least two DB subnet IDs in different Availability Zones."
  }
}

variable "app_security_group_id" {
  type = string
}

variable "db_identifier" {
  type = string
}

variable "engine" {
  type    = string
  default = "mysql"
}

variable "engine_version" {
  type    = string
  default = "8.0"
}

variable "instance_class" {
  type    = string
  default = "db.t3.small"
}

variable "allocated_storage" {
  type    = number
  default = 20
}

variable "max_allocated_storage" {
  type    = number
  default = 100
}

variable "storage_type" {
  type    = string
  default = "gp3"
}

variable "storage_encrypted" {
  type    = bool
  default = true
}

variable "db_name" {
  type = string
}

variable "username" {
  type = string
}

variable "port" {
  type    = number
  default = 3306
}

variable "multi_az" {
  type    = bool
  default = false
}

variable "backup_retention_period" {
  type    = number
  default = 7
}

variable "backup_window" {
  type    = string
  default = "18:00-19:00"
}

variable "maintenance_window" {
  type    = string
  default = "sun:19:00-sun:20:00"
}

variable "auto_minor_version_upgrade" {
  type    = bool
  default = true
}

variable "deletion_protection" {
  type    = bool
  default = true
}

variable "skip_final_snapshot" {
  type    = bool
  default = false
}

variable "final_snapshot_identifier" {
  type    = string
  default = null
}

variable "apply_immediately" {
  type    = bool
  default = false
}

variable "tags" {
  type    = map(string)
  default = {}
}
