#!/bin/bash
set -e

echo "==================================="
echo "Setup da Infraestrutura AWS"
echo "==================================="
echo ""

# Verificar se terraform está instalado
if ! command -v terraform &> /dev/null; then
    echo "❌ Terraform não encontrado. Instale em: https://www.terraform.io/downloads"
    exit 1
fi

# Verificar se AWS CLI está instalado
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI não encontrado. Instale em: https://aws.amazon.com/cli/"
    exit 1
fi

# Verificar credenciais AWS
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ Credenciais AWS não configuradas. Execute: aws configure"
    exit 1
fi

echo "✅ Pré-requisitos verificados"
echo ""
cd ..

# Verificar se terraform.tfvars existe
if [ ! -f "terraform.tfvars" ]; then
    echo "⚠️  terraform.tfvars não encontrado"
    echo "📝 Criando a partir do exemplo..."
    cp terraform.tfvars.example terraform.tfvars
    echo ""
    echo "⚠️  IMPORTANTE: Edite o arquivo terraform.tfvars e altere:"
    echo "   - db_password (use uma senha forte!)"
    echo "   - Outras configurações conforme necessário"
    echo ""
    read -p "Pressione ENTER após editar o arquivo terraform.tfvars..."
fi

echo "🔧 Inicializando Terraform..."
terraform init
echo ""

echo "📋 Planejando infraestrutura..."
terraform plan -out=tfplan
echo ""

read -p "Deseja criar a infraestrutura? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "❌ Operação cancelada"
    rm -f tfplan
    exit 0
fi

echo ""
echo "🚀 Criando infraestrutura..."
terraform apply tfplan
rm -f tfplan
echo ""

echo "✅ Infraestrutura criada com sucesso!"
echo ""
echo "==================================="
echo "Próximos passos:"
echo "==================================="
echo ""
echo "1. Fazer login no ECR:"
echo "   make ecr-login"
echo ""
echo "2. Build e push das imagens Docker"
echo ""
echo "3. Criar database 'receiver' no RDS"
echo ""
echo "4. Forçar deployment dos serviços:"
echo "   make deploy-marketplace"
echo "   make deploy-receiver"
echo ""
echo "URLs dos serviços:"
terraform output marketplace_alb_dns
terraform output receiver_alb_dns
echo ""