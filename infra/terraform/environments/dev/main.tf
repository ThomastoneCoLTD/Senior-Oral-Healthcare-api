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
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

module "network" {
  source = "../../modules/network"

  project_name             = var.project_name
  environment              = var.environment
  aws_region               = var.aws_region
  vpc_cidr                 = var.vpc_cidr
  public_subnet_cidrs      = var.public_subnet_cidrs
  private_app_subnet_cidrs = var.private_app_subnet_cidrs
  private_db_subnet_cidrs  = var.private_db_subnet_cidrs
  single_nat_gateway       = var.single_nat_gateway
  tags                     = local.common_tags
}

module "security" {
  source = "../../modules/security"

  name_prefix = local.name_prefix
  vpc_id      = module.network.vpc_id
  app_port    = var.app_port
  tags        = local.common_tags
}

module "rds" {
  source = "../../modules/rds"

  name_prefix           = local.name_prefix
  vpc_id                = module.network.vpc_id
  db_subnet_ids         = module.network.private_db_subnet_ids
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

  name_prefix          = local.name_prefix
  vpc_id               = module.network.vpc_id
  public_subnet_ids    = module.network.public_subnet_ids
  alb_sg_id            = module.security.alb_sg_id
  alb_name             = var.alb_name
  target_group_name    = var.target_group_name
  app_port             = var.app_port
  health_path          = var.health_path
  certificate_arn      = var.certificate_arn
  enable_http_redirect = var.enable_http_redirect
  create_route53_record = var.create_route53_record
  hosted_zone_name     = var.hosted_zone_name
  origin_domain_name   = var.origin_domain_name
  tags                 = local.common_tags
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
  private_app_subnet_ids  = module.network.private_app_subnet_ids
  launch_template_id      = module.launch_template.launch_template_id
  launch_template_version = module.launch_template.launch_template_latest_version
  target_group_arn        = module.alb.target_group_arn
  desired_capacity        = var.desired_capacity
  min_size                = var.min_size
  max_size                = var.max_size
  tags                    = local.common_tags
}

output "vpc_id" {
  value = module.network.vpc_id
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
