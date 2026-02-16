output "marketplace_repository_url" {
  description = "Marketplace ECR repository URL"
  value       = aws_ecr_repository.marketplace.repository_url
}

output "receiver_repository_url" {
  description = "Receiver ECR repository URL"
  value       = aws_ecr_repository.receiver.repository_url
}

output "marketplace_repository_name" {
  description = "Marketplace ECR repository name"
  value       = aws_ecr_repository.marketplace.name
}

output "receiver_repository_name" {
  description = "Receiver ECR repository name"
  value       = aws_ecr_repository.receiver.name
}
