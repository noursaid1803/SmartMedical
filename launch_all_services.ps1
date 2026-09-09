# Script PowerShell pour lancer tous les microservices SmartMedical
# Lance dans l'ordre: Gateway, Auth, User, Patient, Medical, Scan, AI, Frontend

$basePath = "c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main"

Write-Host "`n[NETTOYAGE] Fermeture des anciens processus..." -ForegroundColor Yellow
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "node" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "python" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

# Fonction pour lancer un service dans une nouvelle fenêtre
function Start-ServiceWindow {
    param(
        [string]$Name,
        [string]$Path,
        [string]$Command,
        [int]$DelaySeconds = 10
    )
    
    Write-Host "`n[LANCEMENT] $Name..." -ForegroundColor Green
    $psCommand = "Set-Location -LiteralPath '$Path'; Write-Host '*** $Name ***' -ForegroundColor Cyan; $Command"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $psCommand
    Write-Host "Attente de ${DelaySeconds}s pour le démarrage..." -ForegroundColor Yellow
    Start-Sleep -Seconds $DelaySeconds
}

# 1. GATEWAY-SERVICE (Port 8088)
Start-ServiceWindow -Name "GATEWAY-SERVICE (Port 8088)" `
    -Path "$basePath\Backend\GATEWAY-SERVICE" `
    -Command "mvn spring-boot:run" `
    -DelaySeconds 15

# 2. AUTH-SERVICE (Port 8081)
Start-ServiceWindow -Name "AUTH-SERVICE (Port 8081)" `
    -Path "$basePath\Backend\AUTH-SERVICE" `
    -Command "mvn spring-boot:run" `
    -DelaySeconds 10

# 3. PATIENT-SERVICE (Port 8082)
Start-ServiceWindow -Name "PATIENT-SERVICE (Port 8082)" `
    -Path "$basePath\Backend\PATIENT-SERVICE" `
    -Command "mvn spring-boot:run" `
    -DelaySeconds 10

# 4. MEDICAL-SERVICE (Port 8083)
Start-ServiceWindow -Name "MEDICAL-SERVICE (Port 8083)" `
    -Path "$basePath\Backend\MEDICAL-SERVICE" `
    -Command "mvn spring-boot:run" `
    -DelaySeconds 10

# 5. SCAN-SERVICE (Port 8084)
Start-ServiceWindow -Name "SCAN-SERVICE (Port 8084)" `
    -Path "$basePath\Backend\SCAN-SERVICE" `
    -Command "mvn spring-boot:run" `
    -DelaySeconds 10

# 6. USER-SERVICE (Port 8085)
Start-ServiceWindow -Name "USER-SERVICE (Port 8085)" `
    -Path "$basePath\Backend\USER-SERVICE" `
    -Command "mvn spring-boot:run" `
    -DelaySeconds 10

# 7. AI-ANALYSIS-SERVICE (Port 8086)
Start-ServiceWindow -Name "AI-ANALYSIS-SERVICE (Port 8086)" `
    -Path "$basePath\Backend\AI-ANALYSIS-SERVICE" `
    -Command ".\venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8086" `
    -DelaySeconds 15

# 8. Frontend Angular (Port 4200)
Start-ServiceWindow -Name "Frontend Angular (Port 4200)" `
    -Path "$basePath\SmartMedical-Angular" `
    -Command "ng serve" `
    -DelaySeconds 5

Write-Host "`n=========================================" -ForegroundColor Green
Write-Host "Tous les services sont lancés !" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host "URLs disponibles:" -ForegroundColor Cyan
Write-Host "  - Frontend: http://localhost:4200" -ForegroundColor White
Write-Host "  - Gateway:  http://localhost:8088" -ForegroundColor White
Write-Host "  - Auth:     http://localhost:8081" -ForegroundColor White
Write-Host "  - Patient:  http://localhost:8082" -ForegroundColor White
Write-Host "  - Medical:  http://localhost:8083" -ForegroundColor White
Write-Host "  - Scan:     http://localhost:8084" -ForegroundColor White
Write-Host "  - User:     http://localhost:8085" -ForegroundColor White
Write-Host "  - AI:       http://localhost:8086" -ForegroundColor White
Write-Host "`nAppuyez sur une touche pour fermer ce script..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
