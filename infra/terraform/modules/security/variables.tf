variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "app_port" {
  type    = number
  default = 8080
}

variable "alb_allowed_cidrs" {
  type    = list(string)
  default = ["0.0.0.0/0"]
}

variable "alb_allowed_ipv6_cidrs" {
  type    = list(string)
  default = ["::/0"]
}

variable "tags" {
  type    = map(string)
  default = {}
}
