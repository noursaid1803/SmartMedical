# PowerShell script to start all SmartMedical services
$baseDir = Split-Path -Parent $PSScriptRoot
$projectDir = Join-Path $baseDir "SmartMedical-main"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  SMART MEDICAL - Demarrage des Services" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Kill existing Java processes
Write-Host "[INFO] Arret des processus Java existants..." -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 3

# Function to start a service
function Start-ServiceCmd {
    param(
        [string]$Name,
        [string]$Path,
        [int]$Port
    )
    Write-Host "[$Name] Demarrage sur port $Port..." -ForegroundColor Green
    $servicePath = Join-Path $projectDir $Path
    Start-Process cmd -ArgumentList "/k cd /d `"$servicePath`" && mvn spring-boot:run -DskipTests -q" -WindowTitle "$Name $Port"
}

# Start Gateway
Start-ServiceCmd -Name "Gateway" -Path "Backend\GATEWAY-SERVICE" -Port 8080
Start-Sleep -Seconds 30

# Start Auth
Start-ServiceCmd -Name "Auth" -Path "Backend\AUTH-SERVICE" -Port 8081
Start-Sleep -Seconds 30

# Start Patient
Start-ServiceCmd -Name "Patient" -Path "Backend\PATIENT-SERVICE" -Port 8082
Start-Sleep -Seconds 30

# Start Medical
Start-ServiceCmd -Name "Medical" -Path "Backend\MEDICAL-SERVICE" -Port 8083
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "  TOUS LES SERVICES DEMARRES !" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "- Gateway:  http://localhost:8080" -ForegroundColor White
Write-Host "- Auth:     http://localhost:8081" -ForegroundColor White
Write-Host "- Patient:  http://localhost:8082" -ForegroundColor White
Write-Host "- Medical:  http://localhost:8083" -ForegroundColor White
Write-Host ""
Write-Host "Angular: http://localhost:4200" -ForegroundColor Yellow
Write-Host ""
Write-Host "Appuyez sur une touche pour fermer..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
