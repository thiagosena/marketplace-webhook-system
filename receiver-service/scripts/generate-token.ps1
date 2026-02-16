# Script para gerar token de serviço seguro
# Uso: .\generate-token.ps1

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "  Token Generator - Receiver Service" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Gera token aleatório de 32 bytes em Base64
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$token = [Convert]::ToBase64String($bytes)

Write-Host "Token gerado com sucesso!" -ForegroundColor Green
Write-Host ""
Write-Host "TOKEN:" -ForegroundColor Yellow
Write-Host $token -ForegroundColor White
Write-Host ""
Write-Host "Configuração para Windows (PowerShell):" -ForegroundColor Cyan
Write-Host "`$env:SERVICE_SHARED_SECRET='$token'" -ForegroundColor White
Write-Host ""
Write-Host "Configuração para Linux/Mac (Bash):" -ForegroundColor Cyan
Write-Host "export SERVICE_SHARED_SECRET='$token'" -ForegroundColor White
Write-Host ""
Write-Host "Configuração no .env:" -ForegroundColor Cyan
Write-Host "SERVICE_SHARED_SECRET=$token" -ForegroundColor White
Write-Host ""
Write-Host "IMPORTANTE:" -ForegroundColor Red
Write-Host "- Guarde este token em um local seguro" -ForegroundColor Yellow
Write-Host "- Use o mesmo token no Marketplace Service" -ForegroundColor Yellow
Write-Host "- Nunca commite o token no Git" -ForegroundColor Yellow
Write-Host ""
