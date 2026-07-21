terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
  origin_domain_name = "api.soh.thomabio.com"
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

data "aws_vpc" "shared_dev" {
  filter {
    name   = "tag:Name"
    values = [var.shared_vpc_name]
  }
}

data "aws_subnet" "shared_public" {
  for_each = toset(var.shared_public_subnet_names)

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.shared_dev.id]
  }

  filter {
    name   = "tag:Name"
    values = [each.value]
  }
}

data "aws_subnet" "shared_private_app" {
  for_each = toset(var.shared_private_app_subnet_names)

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.shared_dev.id]
  }

  filter {
    name   = "tag:Name"
    values = [each.value]
  }
}

data "aws_subnet" "shared_private_db" {
  for_each = toset(var.shared_private_db_subnet_names)

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.shared_dev.id]
  }

  filter {
    name   = "tag:Name"
    values = [each.value]
  }
}

locals {
  vpc_id                 = data.aws_vpc.shared_dev.id
  public_subnet_ids      = [for name in var.shared_public_subnet_names : data.aws_subnet.shared_public[name].id]
  private_app_subnet_ids = [for name in var.shared_private_app_subnet_names : data.aws_subnet.shared_private_app[name].id]
  private_db_subnet_ids  = [for name in var.shared_private_db_subnet_names : data.aws_subnet.shared_private_db[name].id]
}

module "security" {
  source = "../../modules/security"

  name_prefix = local.name_prefix
  vpc_id      = local.vpc_id
  app_port    = var.app_port
  tags        = local.common_tags
}

module "rds" {
  source = "../../modules/rds"

  name_prefix           = local.name_prefix
  vpc_id                = local.vpc_id
  db_subnet_ids         = local.private_db_subnet_ids
  app_security_group_id = module.security.ec2_sg_id

  db_identifier              = var.db_identifier
  engine                     = var.db_engine
  engine_version             = var.db_engine_version
  instance_class             = var.db_instance_class
  allocated_storage          = var.db_allocated_storage
  max_allocated_storage      = var.db_max_allocated_storage
  storage_type               = var.db_storage_type
  storage_encrypted          = var.db_storage_encrypted
  db_name                    = var.db_name
  username                   = var.db_username
  port                       = var.db_port
  multi_az                   = var.db_multi_az
  backup_retention_period    = var.db_backup_retention_period
  backup_window              = var.db_backup_window
  maintenance_window         = var.db_maintenance_window
  auto_minor_version_upgrade = var.db_auto_minor_version_upgrade
  deletion_protection        = var.db_deletion_protection
  skip_final_snapshot        = var.db_skip_final_snapshot
  final_snapshot_identifier  = var.db_final_snapshot_identifier
  apply_immediately          = var.db_apply_immediately
  tags                       = local.common_tags
}

module "iam" {
  source = "../../modules/iam"

  name_prefix     = local.name_prefix
  artifact_bucket = var.artifact_bucket
  artifact_prefix = var.artifact_prefix
  release_type    = var.release_type
  enable_ssm      = var.enable_ssm
  tags            = local.common_tags
}

module "alb" {
  source = "../../modules/alb"

  name_prefix           = local.name_prefix
  vpc_id                = local.vpc_id
  public_subnet_ids     = local.public_subnet_ids
  alb_sg_id             = module.security.alb_sg_id
  alb_name              = var.alb_name
  target_group_name     = var.target_group_name
  app_port              = var.app_port
  health_path           = var.health_path
  certificate_arn       = var.certificate_arn
  enable_http_redirect  = var.enable_http_redirect
  create_route53_record = var.create_route53_record
  hosted_zone_name      = var.hosted_zone_name
  origin_domain_name    = local.origin_domain_name
  tags                  = local.common_tags
}

module "launch_template" {
  source = "../../modules/launch_template"

  name_prefix           = local.name_prefix
  launch_template_name  = var.launch_template_name
  app_name              = local.name_prefix
  environment           = var.environment
  artifact_bucket       = var.artifact_bucket
  artifact_prefix       = var.artifact_prefix
  release_type          = var.release_type
  spring_profile        = var.spring_profile
  aws_region            = var.aws_region
  instance_type         = var.instance_type
  ami_id                = var.ami_id
  instance_profile_name = module.iam.instance_profile_name
  ec2_sg_id             = module.security.ec2_sg_id
  tags                  = local.common_tags
}

module "autoscaling" {
  source = "../../modules/autoscaling"

  asg_name                = var.asg_name
  private_app_subnet_ids  = local.private_app_subnet_ids
  launch_template_id      = module.launch_template.launch_template_id
  launch_template_version = module.launch_template.launch_template_latest_version
  target_group_arn        = module.alb.target_group_arn
  desired_capacity        = var.desired_capacity
  min_size                = var.min_size
  max_size                = var.max_size
  tags                    = local.common_tags
}

output "vpc_id" {
  value = local.vpc_id
}

output "origin_domain_name" {
  value = local.origin_domain_name
}

output "alb_dns_name" {
  value = module.alb.alb_dns_name
}

output "target_group_name" {
  value = module.alb.target_group_name
}

output "asg_name" {
  value = module.autoscaling.asg_name
}

output "ec2_role_arn" {
  value = module.iam.role_arn
}

output "db_endpoint" {
  value = module.rds.db_endpoint
}

output "db_address" {
  value = module.rds.db_address
}

output "db_name" {
  value = module.rds.db_name
}

output "db_master_user_secret_arn" {
  value     = module.rds.master_user_secret_arn
  sensitive = true
}
