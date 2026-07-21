variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "alb_sg_id" {
  type = string
}

variable "alb_name" {
  type = string
}

variable "target_group_name" {
  type = string
}

variable "app_port" {
  type    = number
  default = 8080
}

variable "health_path" {
  type    = string
  default = "/api/actuator/health"
}

variable "certificate_arn" {
  type = string
}

variable "enable_http_redirect" {
  type    = bool
  default = true
}

variable "create_route53_record" {
  type    = bool
  default = false
}

variable "hosted_zone_name" {
  type    = string
  default = ""
}

variable "origin_domain_name" {
  type = string
}

variable "tags" {
  type    = map(string)
  default = {}
}
