variable "project_name" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Environment"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "Private subnet IDs"
  type        = list(string)
}

variable "marketplace_ecr_repository" {
  description = "Marketplace ECR repository URL"
  type        = string
}

variable "receiver_ecr_repository" {
  description = "Receiver ECR repository URL"
  type        = string
}

variable "marketplace_db_host" {
  description = "Marketplace database host"
  type        = string
}

variable "receiver_db_host" {
  description = "Receiver database host"
  type        = string
}

variable "marketplace_db_name" {
  description = "Marketplace database name"
  type        = string
}

variable "receiver_db_name" {
  description = "Receiver database name"
  type        = string
}

variable "db_username" {
  description = "Database username"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "marketplace_container_port" {
  description = "Marketplace container port"
  type        = number
}

variable "receiver_container_port" {
  description = "Receiver container port"
  type        = number
}

variable "marketplace_desired_count" {
  description = "Marketplace desired task count"
  type        = number
}

variable "receiver_desired_count" {
  description = "Receiver desired task count"
  type        = number
}

variable "marketplace_cpu" {
  description = "Marketplace CPU units"
  type        = number
}

variable "marketplace_memory" {
  description = "Marketplace memory in MB"
  type        = number
}

variable "receiver_cpu" {
  description = "Receiver CPU units"
  type        = number
}

variable "receiver_memory" {
  description = "Receiver memory in MB"
  type        = number
}

variable "marketplace_service_shared_secret" {
  description = "Shared secret for marketplace service authentication"
  type        = string
  sensitive   = true
}

variable "receiver_service_shared_secret" {
  description = "Shared secret for receiver service authentication"
  type        = string
  sensitive   = true
}
