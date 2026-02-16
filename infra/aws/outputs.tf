output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "marketplace_ecr_repository_url" {
  description = "Marketplace ECR repository URL"
  value       = module.ecr.marketplace_repository_url
}

output "receiver_ecr_repository_url" {
  description = "Receiver ECR repository URL"
  value       = module.ecr.receiver_repository_url
}

output "marketplace_alb_dns" {
  description = "Marketplace ALB DNS name"
  value       = module.ecs.marketplace_alb_dns
}

output "receiver_alb_dns" {
  description = "Receiver ALB DNS name"
  value       = module.ecs.receiver_alb_dns
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.rds.db_endpoint
  sensitive   = true
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs.cluster_name
}

output "marketplace_service_name" {
  description = "Marketplace ECS service name"
  value       = module.ecs.marketplace_service_name
}

output "receiver_service_name" {
  description = "Receiver ECS service name"
  value       = module.ecs.receiver_service_name
}
