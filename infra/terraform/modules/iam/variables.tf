variable "name_prefix" {
  type = string
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

variable "database_secret_arn" {
  type    = string
  default = ""
}

variable "enable_ssm" {
  type    = bool
  default = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
