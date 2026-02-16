@echo off
REM Script para gerar chave SSH para o bastion temporário (Windows)

set KEY_DIR=%~dp0..\\.ssh
set KEY_FILE=%KEY_DIR%\\bastion_key

REM Criar diretório se não existir
if not exist "%KEY_DIR%" mkdir "%KEY_DIR%"

REM Verificar se ssh-keygen está disponível
where ssh-keygen >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: ssh-keygen não encontrado. Instale o OpenSSH ou Git Bash.
    echo Você pode instalar via: winget install Microsoft.OpenSSH.Beta
    exit /b 1
)

REM Gerar chave SSH
if not exist "%KEY_FILE%" (
    echo Gerando chave SSH para bastion...
    ssh-keygen -t rsa -b 2048 -f "%KEY_FILE%" -N "" -C "bastion-temp-key"
    echo Chave gerada em: %KEY_FILE%
) else (
    echo Chave já existe em: %KEY_FILE%
)

echo.
echo ==========================================
echo Para usar no terraform.tfvars, adicione:
echo ==========================================
echo.
echo # Chave pública
echo bastion_public_key = "
type "%KEY_FILE%.pub"
echo "
echo.
echo # Chave privada
echo bastion_private_key = ^<^<-EOT
type "%KEY_FILE%"
echo EOT

pause
