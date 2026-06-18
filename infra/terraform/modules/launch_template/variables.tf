variable "name_prefix" {
  type = string
}

variable "launch_template_name" {
  type = string
}

variable "app_name" {
  type = string
}

variable "environment" {
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

variable "spring_profile" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

variable "ami_id" {
  type    = string
  default = ""
}

variable "instance_profile_name" {
  type = string
}

variable "ec2_sg_id" {
  type = string
}

variable "tags" {
  type    = map(string)
  default = {}
}
