# Configuração do Bastion para Criação Automática do Database Receiver

## Por que precisamos disso?

O RDS PostgreSQL é criado em uma subnet privada (sem acesso à internet). Para criar o segundo database (`receiver`) automaticamente, precisamos de uma instância EC2 temporária (bastion) que:

1. Está em uma subnet pública (pode ser acessada via SSH)
2. Tem acesso ao RDS via security group
3. Executa o comando SQL para criar o database

## Como funciona?

1. Terraform cria o RDS com o database `marketplace`
2. Terraform cria uma EC2 t3.micro temporária (bastion) em subnet pública
3. Via SSH, o Terraform se conecta ao bastion e executa:
   - Verifica se o RDS está pronto
   - Cria o database `receiver` se não existir
4. O bastion permanece ativo (você pode destruí-lo depois se quiser economizar)

## Passo a Passo

### 1. Gerar chave SSH

**Linux/Mac:**
```bash
cd infra/aws
./scripts/generate-bastion-key.sh
```

**Windows:**
```cmd
cd infra\aws
.\scripts\generate-bastion-key.bat
```

### 2. Copiar as chaves para terraform.tfvars

O script vai mostrar as chaves formatadas. Copie e cole no seu `terraform.tfvars`:

```hcl
bastion_public_key = "ssh-rsa AAAAB3NzaC1yc2EA... bastion-temp-key"

bastion_private_key = <<-EOT
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAABFwAAAAdzc2gtcn
...
-----END OPENSSH PRIVATE KEY-----
EOT
```

### 3. Aplicar o Terraform

```bash
terraform init
terraform plan
terraform apply
```

## Custos

- EC2 t3.micro: ~$0.0104/hora (~$7.50/mês se deixar rodando)
- Você pode destruir o bastion após a criação do database se quiser

## Destruir o Bastion (opcional)

Se quiser economizar após criar o database:

```bash
# Remover apenas o bastion do state (mantém o database)
terraform state rm module.rds.aws_instance.bastion
terraform state rm module.rds.aws_key_pair.bastion
terraform state rm module.rds.aws_security_group.bastion
terraform state rm module.rds.null_resource.create_receiver_db

# Destruir manualmente via console AWS ou:
aws ec2 terminate-instances --instance-ids <INSTANCE_ID>
```

## Troubleshooting

### Erro: "Connection timeout"
- Verifique se a subnet pública tem Internet Gateway
- Verifique se o security group permite SSH (porta 22)

### Erro: "Permission denied (publickey)"
- Verifique se copiou a chave privada corretamente
- Verifique se não há espaços extras no terraform.tfvars

### Erro: "Database already exists"
- Normal se você rodar `terraform apply` novamente
- O script verifica se o database existe antes de criar
