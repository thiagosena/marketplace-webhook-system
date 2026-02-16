@echo off
setlocal enabledelayedexpansion

echo ===================================
echo Destruir Infraestrutura AWS
echo ===================================
echo.
echo ! ATENCAO: Esta acao ira DESTRUIR toda a infraestrutura!
echo ! Isso inclui:
echo    - VPC e todos os recursos de rede
echo    - RDS PostgreSQL ^(e todos os dados!^)
echo    - ECR e todas as imagens Docker
echo    - ECS Cluster e servicos
echo    - Application Load Balancers
echo    - CloudWatch Logs
echo.

set /p confirm="Tem certeza que deseja continuar? Digite 'destroy' para confirmar: "
if not "%confirm%"=="destroy" (
    echo X Operacao cancelada
    exit /b 0
)

echo.
echo Planejando destruicao...
terraform plan -destroy -out=tfplan-destroy
echo.

set /p confirm2="Confirma a destruicao? (yes/no): "
if not "%confirm2%"=="yes" (
    echo X Operacao cancelada
    del tfplan-destroy 2>nul
    exit /b 0
)

echo.
echo Destruindo infraestrutura...
terraform apply tfplan-destroy
del tfplan-destroy 2>nul
echo.

echo OK Infraestrutura destruida com sucesso!
pause
