#!/bin/bash

# Script para gerar chave SSH para o bastion temporário

KEY_DIR="$(dirname "$0")/../.ssh"
KEY_FILE="$KEY_DIR/bastion_key"

# Criar diretório se não existir
mkdir -p "$KEY_DIR"

# Gerar chave SSH
if [ ! -f "$KEY_FILE" ]; then
    echo "Gerando chave SSH para bastion..."
    ssh-keygen -t rsa -b 2048 -f "$KEY_FILE" -N "" -C "bastion-temp-key"
    echo "Chave gerada em: $KEY_FILE"
else
    echo "Chave já existe em: $KEY_FILE"
fi

# Adicionar ao .gitignore
GITIGNORE_FILE="$(dirname "$0")/../.gitignore"
if ! grep -q ".ssh/" "$GITIGNORE_FILE" 2>/dev/null; then
    echo ".ssh/" >> "$GITIGNORE_FILE"
    echo "Adicionado .ssh/ ao .gitignore"
fi

echo ""
echo "=========================================="
echo "Para usar no terraform.tfvars, adicione:"
echo "=========================================="
echo ""
echo "# Chave pública"
echo "bastion_public_key = \"$(cat ${KEY_FILE}.pub)\""
echo ""
echo "# Chave privada"
echo "bastion_private_key = <<-EOT"
cat "$KEY_FILE"
echo "EOT"
