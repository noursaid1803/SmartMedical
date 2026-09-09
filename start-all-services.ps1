# Script PowerShell pour démarrer tous les services SmartMedical

$BASEDIR = 'c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main'

Write-Host "==========================================" -ForegroundColor Green
Write-Host "  DEMARRAGE SMARTMEDICAL" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

# Fonction pour démarrer un service
function Start-Service {
    param(
        [string]$Name,
        [string]$Port,
        [string]$Path,
        [int]$DelaySeconds = 0
    )
    
    if ($DelaySeconds -gt 0) {
        Write-Host "Attente de ${DelaySeconds}s avant de demarrer $Name..." -ForegroundColor Yellow
        Start-Sleep -Seconds $DelaySeconds
    }
    
    Write-Host "Demarrage de $Name sur le port $Port..." -ForegroundColor Cyan
    
    $process = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-Command", "cd `"$Path`" -and mvn spring-boot:run -DskipTests -q" -WindowStyle Normal -PassThru
    
    Write-Host "$Name demarre (PID: $($process.Id))" -ForegroundColor Green
    return $process
}

# Arreter les processus Java existants
Write-Host "Arret des services existants..." -ForegroundColor Yellow
taskkill /F /IM java.exe 2>$null
Start-Sleep -Seconds 3

# Demarrer les services backend
$gateway = Start-Service -Name "Gateway" -Port "8088" -Path "$BASEDIR\Backend\GATEWAY-SERVICE" -DelaySeconds 0
Start-Service -Name "Auth" -Port "8081" -Path "$BASEDIR\Backend\AUTH-SERVICE" -DelaySeconds 15
Start-Service -Name "Patient" -Port "8082" -Path "$BASEDIR\Backend\PATIENT-SERVICE" -DelaySeconds 30
Start-Service -Name "Medical" -Port "8083" -Path "$BASEDIR\Backend\MEDICAL-SERVICE" -DelaySeconds 45

# Demarrer Angular
Write-Host "Demarrage d'Angular sur le port 4200..." -ForegroundColor Cyan
Start-Sleep -Seconds 60
Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-Command", "cd `"$BASEDIR\SmartMedical-Angular`"; ng serve" -WindowStyle Normal

Write-Host "" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "  TOUS LES SERVICES DEMARRES !" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "- Gateway:  http://localhost:8088" -ForegroundColor White
Write-Host "- Auth:     http://localhost:8081" -ForegroundColor White
Write-Host "- Patient:  http://localhost:8082" -ForegroundColor White
Write-Host "- Medical:  http://localhost:8083" -ForegroundColor White
Write-Host "- Angular:  http://localhost:4200" -ForegroundColor White
Write-Host "==========================================" -ForegroundColor Green
