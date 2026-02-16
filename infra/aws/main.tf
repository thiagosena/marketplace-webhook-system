terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    # Descomente e configure para usar S3 como backend
    bucket = "marketplace-tf-infra"
    key    = "marketplace/terraform.tfstate"
    region = "us-east-2"
    # encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# VPC
module "vpc" {
  source = "./modules/vpc"

  project_name = var.project_name
  environment  = var.environment
  vpc_cidr     = var.vpc_cidr
}

# RDS PostgreSQL
module "rds" {
  source = "./modules/rds"

  project_name          = var.project_name
  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnet_ids
  db_instance_class     = var.db_instance_class
  db_allocated_storage  = var.db_allocated_storage
  db_username           = var.db_username
  db_password           = var.db_password
  marketplace_db_name   = var.marketplace_db_name
  receiver_db_name      = var.receiver_db_name
  ecs_security_group_id = module.ecs.ecs_security_group_id
}

# ECR
module "ecr" {
  source = "./modules/ecr"

  project_name = var.project_name
  environment  = var.environment
}

# ECS
module "ecs" {
  source = "./modules/ecs"

  project_name               = var.project_name
  environment                = var.environment
  vpc_id                     = module.vpc.vpc_id
  public_subnet_ids          = module.vpc.public_subnet_ids
  private_subnet_ids         = module.vpc.private_subnet_ids
  marketplace_ecr_repository = module.ecr.marketplace_repository_url
  receiver_ecr_repository    = module.ecr.receiver_repository_url
  marketplace_db_host        = module.rds.db_endpoint
  receiver_db_host           = module.rds.db_endpoint
  marketplace_db_name        = var.marketplace_db_name
  receiver_db_name           = var.receiver_db_name
  db_username                = var.db_username
  db_password                = var.db_password
  marketplace_container_port = var.marketplace_container_port
  receiver_container_port    = var.receiver_container_port
  marketplace_desired_count  = var.marketplace_desired_count
  receiver_desired_count     = var.receiver_desired_count
  marketplace_cpu            = var.marketplace_cpu
  marketplace_memory         = var.marketplace_memory
  receiver_cpu               = var.receiver_cpu
  receiver_memory            = var.receiver_memory
}
