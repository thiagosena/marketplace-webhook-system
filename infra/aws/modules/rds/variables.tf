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

variable "private_subnet_ids" {
  description = "Private subnet IDs"
  type        = list(string)
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
}

variable "db_username" {
  description = "Database master username"
  type        = string
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
}

variable "receiver_db_name" {
  description = "Receiver database name"
  type        = string
}

variable "ecs_security_group_id" {
  description = "ECS security group ID"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs for bastion"
  type        = list(string)
}

variable "bastion_public_key" {
  description = "SSH public key for bastion"
  type        = string
}

variable "bastion_private_key" {
  description = "SSH private key for bastion connection"
  type        = string
  sensitive   = true
}
