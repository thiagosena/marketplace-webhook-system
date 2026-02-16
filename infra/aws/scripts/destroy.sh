#!/bin/bash
set -e

echo "==================================="
echo "Destruir Infraestrutura AWS"
echo "==================================="
echo ""
echo "⚠️  ATENÇÃO: Esta ação irá DESTRUIR toda a infraestrutura!"
echo "⚠️  Isso inclui:"
echo "   - VPC e todos os recursos de rede"
echo "   - RDS PostgreSQL (e todos os dados!)"
echo "   - ECR e todas as imagens Docker"
echo "   - ECS Cluster e serviços"
echo "   - Application Load Balancers"
echo "   - CloudWatch Logs"
echo ""
read -p "Tem certeza que deseja continuar? Digite 'destroy' para confirmar: " confirm

if [ "$confirm" != "destroy" ]; then
    echo "❌ Operação cancelada"
    exit 0
fi

echo ""
echo "📋 Planejando destruição..."
terraform plan -destroy -out=tfplan-destroy
echo ""

read -p "Confirma a destruição? (yes/no): " confirm2
if [ "$confirm2" != "yes" ]; then
    echo "❌ Operação cancelada"
    rm -f tfplan-destroy
    exit 0
fi

echo ""
echo "💣 Destruindo infraestrutura..."
terraform apply tfplan-destroy
rm -f tfplan-destroy
echo ""

echo "✅ Infraestrutura destruída com sucesso!"
