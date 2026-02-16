# AWS Infrastructure with Terraform

Infraestrutura completa para os serviços Marketplace e Receiver na AWS usando Terraform.

## Arquitetura

A infraestrutura provisiona:

- **VPC**: Rede isolada com subnets públicas e privadas em 2 AZs
- **NAT Gateways**: Para acesso à internet das subnets privadas
- **RDS PostgreSQL**: Banco de dados compartilhado com 2 databases
- **ECR**: Repositórios Docker para cada serviço
- **ECS Fargate**: Cluster com 2 serviços containerizados
- **Application Load Balancers**: Um ALB para cada serviço
- **CloudWatch**: Logs centralizados
- **Security Groups**: Isolamento de rede entre componentes

## Pré-requisitos

1. **Terraform** >= 1.0
   ```bash
   # Windows (Chocolatey)
   choco install terraform
   
   # Ou baixe em: https://www.terraform.io/downloads
   ```

2. **AWS CLI** configurado
   ```bash
   # Windows (Chocolatey)
   choco install awscli
   
   # Configure suas credenciais
   aws configure
   ```

3. **Credenciais AWS** com permissões para:
   - VPC, Subnets, Internet Gateway, NAT Gateway
   - RDS
   - ECR
   - ECS, Fargate
   - Application Load Balancer
   - IAM Roles e Policies
   - CloudWatch Logs

## Configuração

1. **Copie o arquivo de exemplo**
   ```bash
   cd infra/aws
   copy terraform.tfvars.example terraform.tfvars
   ```

2. **Edite `terraform.tfvars`** com seus valores:
   ```hcl
   aws_region   = "us-east-2"
   project_name = "marketplace"
   environment  = "prod"
   
   # IMPORTANTE: Altere a senha do banco!
   db_password = "SuaSenhaForteAqui123!"
   ```

## Comandos

### Criar toda a infraestrutura

```bash
# Inicializar Terraform
terraform init

# Ver o que será criado
terraform plan

# Criar a infraestrutura
terraform apply
```

O comando `terraform apply` pedirá confirmação. Digite `yes` para prosseguir.

### Destruir toda a infraestrutura

```bash
# Ver o que será destruído
terraform plan -destroy

# Destruir tudo
terraform destroy
```

O comando `terraform destroy` pedirá confirmação. Digite `yes` para prosseguir.

### Outros comandos úteis

```bash
# Ver outputs (URLs, endpoints, etc)
terraform output

# Ver output específico
terraform output marketplace_alb_dns

# Formatar arquivos
terraform fmt -recursive

# Validar configuração
terraform validate

# Ver estado atual
terraform show
```

## Outputs Importantes

Após o `terraform apply`, você terá acesso a:

```bash
# URLs dos serviços
terraform output marketplace_alb_dns
terraform output receiver_alb_dns

# Repositórios ECR
terraform output marketplace_ecr_repository_url
terraform output receiver_ecr_repository_url

# Informações do ECS
terraform output ecs_cluster_name
terraform output marketplace_service_name
terraform output receiver_service_name
```

## Primeiro Deploy

Após criar a infraestrutura, você precisa fazer o primeiro push das imagens:

1. **Autenticar no ECR**
   ```bash
   aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin $(terraform output -raw marketplace_ecr_repository_url | cut -d'/' -f1)
   ```

2. **Build e push das imagens**
   ```bash
   # Marketplace Service
   cd ../../marketplace-service
   docker build -t $(terraform output -raw marketplace_ecr_repository_url):latest -f docker/Dockerfile.prod .
   docker push $(terraform output -raw marketplace_ecr_repository_url):latest
   
   # Receiver Service
   cd ../receiver-service
   docker build -t $(terraform output -raw receiver_ecr_repository_url):latest -f docker/Dockerfile.prod .
   docker push $(terraform output -raw receiver_ecr_repository_url):latest
   ```

3. **Forçar novo deployment**
   ```bash
   aws ecs update-service --cluster $(terraform output -raw ecs_cluster_name) \
     --service $(terraform output -raw marketplace_service_name) \
     --force-new-deployment
   
   aws ecs update-service --cluster $(terraform output -raw ecs_cluster_name) \
     --service $(terraform output -raw receiver_service_name) \
     --force-new-deployment
   ```

## Custos Estimados

Custos aproximados mensais (us-east-2):

- **VPC**: Grátis
- **NAT Gateway**: ~$32/mês (2 NAT Gateways)
- **RDS db.t3.micro**: ~$15/mês
- **ECS Fargate**: ~$15/mês (4 tasks com 0.25 vCPU e 512MB RAM)
- **ALB**: ~$20/mês (2 ALBs)
- **ECR**: ~$1/mês (storage)
- **CloudWatch Logs**: ~$5/mês

**Total estimado**: ~$88/mês

Para reduzir custos em ambientes de desenvolvimento:
- Use apenas 1 NAT Gateway
- Reduza `desired_count` para 1
- Use RDS menor ou Aurora Serverless

## Combinações Válidas de CPU/Memória no Fargate

O AWS Fargate tem combinações específicas de CPU e memória permitidas:

| CPU (vCPU) | Memória (MB) |
|------------|--------------|
| 256 (.25)  | 512, 1024, 2048 |
| 512 (.5)   | 1024, 2048, 3072, 4096 |
| 1024 (1)   | 2048, 3072, 4096, 5120, 6144, 7168, 8192 |
| 2048 (2)   | 4096 até 16384 (incrementos de 1024) |
| 4096 (4)   | 8192 até 30720 (incrementos de 1024) |

**Configuração padrão**: 256 CPU + 512 MB (mais econômica)

## Estrutura de Arquivos

```
infra/aws/
├── main.tf              # Configuração principal
├── variables.tf         # Variáveis de entrada
├── outputs.tf           # Outputs
├── terraform.tfvars     # Valores das variáveis (não commitado)
├── modules/
│   ├── vpc/            # Módulo de rede
│   ├── rds/            # Módulo de banco de dados
│   ├── ecr/            # Módulo de repositórios Docker
│   └── ecs/            # Módulo de containers
└── README.md
```

## Segurança

- RDS em subnets privadas (sem acesso público)
- ECS tasks em subnets privadas
- Security Groups restritivos
- Senhas via variáveis sensíveis
- Logs centralizados no CloudWatch
- Encryption at rest no RDS

## Troubleshooting

### Tasks não iniciam
```bash
# Ver logs do ECS
aws ecs describe-services --cluster <cluster-name> --services <service-name>

# Ver logs do CloudWatch
aws logs tail /ecs/marketplace-marketplace-service --follow
```

### Não consigo acessar o ALB
- Verifique se as tasks estão healthy
- Verifique os Security Groups
- Aguarde alguns minutos após o deploy

### Erro de autenticação no ECR
```bash
# Re-autenticar
aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-2.amazonaws.com
```

## Próximos Passos

1. Configure um domínio personalizado no Route 53
2. Adicione certificado SSL/TLS no ALB
3. Configure Auto Scaling para os serviços ECS
4. Implemente backup automatizado do RDS
5. Configure alertas no CloudWatch
6. Adicione WAF no ALB para proteção

## Suporte

Para mais informações sobre Terraform:
- [Documentação Terraform](https://www.terraform.io/docs)
- [AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)