variable "asg_name" {
  type = string
}

variable "private_app_subnet_ids" {
  type = list(string)
}

variable "launch_template_id" {
  type = string
}

variable "launch_template_version" {
  type = string
}

variable "target_group_arn" {
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

variable "health_check_grace_period" {
  type    = number
  default = 300
}

variable "tags" {
  type    = map(string)
  default = {}
}
