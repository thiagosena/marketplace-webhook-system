output "cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "cluster_id" {
  description = "ECS cluster ID"
  value       = aws_ecs_cluster.main.id
}

output "marketplace_service_name" {
  description = "Marketplace service name"
  value       = aws_ecs_service.marketplace.name
}

output "receiver_service_name" {
  description = "Receiver service name"
  value       = aws_ecs_service.receiver.name
}

output "marketplace_alb_dns" {
  description = "Marketplace ALB DNS name"
  value       = aws_lb.marketplace.dns_name
}

output "receiver_alb_dns" {
  description = "Receiver ALB DNS name"
  value       = aws_lb.receiver.dns_name
}

output "marketplace_task_definition_arn" {
  description = "Marketplace task definition ARN"
  value       = aws_ecs_task_definition.marketplace.arn
}

output "receiver_task_definition_arn" {
  description = "Receiver task definition ARN"
  value       = aws_ecs_task_definition.receiver.arn
}

output "ecs_security_group_id" {
  description = "ECS security group ID"
  value       = aws_security_group.ecs.id
}
