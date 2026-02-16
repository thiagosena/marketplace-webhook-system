variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "marketplace"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

# Database
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 20
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
}

variable "marketplace_db_name" {
  description = "Marketplace database name"
  type        = string
  default     = "marketplace"
}

variable "receiver_db_name" {
  description = "Receiver database name"
  type        = string
  default     = "receiver"
}

# ECS
variable "marketplace_container_port" {
  description = "Marketplace service container port"
  type        = number
  default     = 8000
}

variable "receiver_container_port" {
  description = "Receiver service container port"
  type        = number
  default     = 8001
}

variable "marketplace_desired_count" {
  description = "Desired number of marketplace service tasks"
  type        = number
  default     = 2
}

variable "receiver_desired_count" {
  description = "Desired number of receiver service tasks"
  type        = number
  default     = 2
}

variable "marketplace_cpu" {
  description = "Marketplace service CPU units (256, 512, 1024, 2048, 4096)"
  type        = number
  default     = 256
}

variable "marketplace_memory" {
  description = "Marketplace service memory in MB (512, 1024, 2048 for 256 CPU)"
  type        = number
  default     = 512
}

variable "receiver_cpu" {
  description = "Receiver service CPU units (256, 512, 1024, 2048, 4096)"
  type        = number
  default     = 256
}

variable "receiver_memory" {
  description = "Receiver service memory in MB (512, 1024, 2048 for 256 CPU)"
  type        = number
  default     = 512
}