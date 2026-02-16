#!/bin/bash

# Script para gerar token de serviço seguro
# Uso: ./generate-token.sh

echo "=================================="
echo "  Token Generator - Receiver Service"
echo "=================================="
echo ""

# Gera token aleatório de 32 bytes em Base64
TOKEN=$(openssl rand -base64 32)

echo "Token gerado com sucesso!"
echo ""
echo -e "\033[1;33mTOKEN:\033[0m"
echo -e "\033[1;37m$TOKEN\033[0m"
echo ""
echo -e "\033[1;36mConfiguração para Linux/Mac (Bash):\033[0m"
echo -e "\033[0;37mexport SERVICE_SHARED_SECRET='$TOKEN'\033[0m"
echo ""
echo -e "\033[1;36mConfiguração para Windows (PowerShell):\033[0m"
echo -e "\033[0;37m\$env:SERVICE_SHARED_SECRET='$TOKEN'\033[0m"
echo ""
echo -e "\033[1;36mConfiguração no .env:\033[0m"
echo -e "\033[0;37mSERVICE_SHARED_SECRET=$TOKEN\033[0m"
echo ""
echo -e "\033[1;31mIMPORTANTE:\033[0m"
echo -e "\033[1;33m- Guarde este token em um local seguro\033[0m"
echo -e "\033[1;33m- Use o mesmo token no Marketplace Service\033[0m"
echo -e "\033[1;33m- Nunca commite o token no Git\033[0m"
echo ""
