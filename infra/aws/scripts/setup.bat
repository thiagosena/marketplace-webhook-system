@echo off
setlocal enabledelayedexpansion

echo ===================================
echo Setup da Infraestrutura AWS
echo ===================================
echo.

REM Verificar Terraform
where terraform >nul 2>nul
if %errorlevel% neq 0 (
    echo X Terraform nao encontrado. Instale em: https://www.terraform.io/downloads
    exit /b 1
)

REM Verificar AWS CLI
where aws >nul 2>nul
if %errorlevel% neq 0 (
    echo X AWS CLI nao encontrado. Instale em: https://aws.amazon.com/cli/
    exit /b 1
)

REM Verificar credenciais AWS
aws sts get-caller-identity >nul 2>nul
if %errorlevel% neq 0 (
    echo X Credenciais AWS nao configuradas. Execute: aws configure
    exit /b 1
)

echo OK Pre-requisitos verificados
echo.

REM Verificar terraform.tfvars
if not exist "terraform.tfvars" (
    echo ! terraform.tfvars nao encontrado
    echo Criando a partir do exemplo...
    copy terraform.tfvars.example terraform.tfvars
    echo.
    echo ! IMPORTANTE: Edite o arquivo terraform.tfvars e altere:
    echo    - db_password ^(use uma senha forte!^)
    echo    - Outras configuracoes conforme necessario
    echo.
    pause
)

echo Inicializando Terraform...
terraform init
echo.

echo Planejando infraestrutura...
terraform plan -out=tfplan
echo.

set /p confirm="Deseja criar a infraestrutura? (yes/no): "
if not "%confirm%"=="yes" (
    echo X Operacao cancelada
    del tfplan 2>nul
    exit /b 0
)

echo.
echo Criando infraestrutura...
terraform apply tfplan
del tfplan 2>nul
echo.

echo OK Infraestrutura criada com sucesso!
echo.
echo ===================================
echo Proximos passos:
echo ===================================
echo.
echo 1. Fazer login no ECR
echo 2. Build e push das imagens Docker
echo 3. Criar database 'receiver' no RDS
echo 4. Forcar deployment dos servicos
echo.
echo URLs dos servicos:
terraform output marketplace_alb_dns
terraform output receiver_alb_dns
echo.
pause
