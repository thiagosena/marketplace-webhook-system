.PHONY: help init plan apply destroy output clean fmt validate

help: ## Mostra esta mensagem de ajuda
	@echo "Comandos disponíveis:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

init: ## Inicializa o Terraform
	terraform init

plan: ## Mostra o que será criado/modificado
	terraform plan

apply: ## Cria/atualiza a infraestrutura
	terraform apply

destroy: ## Destroi toda a infraestrutura
	terraform destroy

output: ## Mostra os outputs
	terraform output

fmt: ## Formata os arquivos Terraform
	terraform fmt -recursive

validate: ## Valida a configuração
	terraform validate

clean: ## Remove arquivos temporários
	rm -rf .terraform
	rm -f .terraform.lock.hcl
	rm -f terraform.tfstate*

# Comandos específicos
ecr-login: ## Faz login no ECR
	aws ecr get-login-password --region $$(terraform output -raw aws_region 2>/dev/null || echo "us-east-2") | \
		docker login --username AWS --password-stdin $$(terraform output -raw marketplace_ecr_repository_url | cut -d'/' -f1)

deploy-marketplace: ## Força novo deployment do marketplace
	aws ecs update-service \
		--cluster $$(terraform output -raw ecs_cluster_name) \
		--service $$(terraform output -raw marketplace_service_name) \
		--force-new-deployment

deploy-receiver: ## Força novo deployment do receiver
	aws ecs update-service \
		--cluster $$(terraform output -raw ecs_cluster_name) \
		--service $$(terraform output -raw receiver_service_name) \
		--force-new-deployment

logs-marketplace: ## Mostra logs do marketplace
	aws logs tail /ecs/marketplace-marketplace-service --follow

logs-receiver: ## Mostra logs do receiver
	aws logs tail /ecs/marketplace-receiver-service --follow

status: ## Mostra status dos serviços
	@echo "=== ECS Services Status ==="
	@aws ecs describe-services \
		--cluster $$(terraform output -raw ecs_cluster_name) \
		--services $$(terraform output -raw marketplace_service_name) $$(terraform output -raw receiver_service_name) \
		--query 'services[*].[serviceName,status,runningCount,desiredCount]' \
		--output table
