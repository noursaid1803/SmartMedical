$base = "c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main"

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "   SMARTMEDICAL - Lancement des services" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan

# Gateway
Write-Host "[1/5] Demarrage Gateway (8080)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/k cd /d `"$base\Backend\GATEWAY-SERVICE`" && mvn spring-boot:run -DskipTests" -WindowStyle Normal

Start-Sleep -Seconds 5

# Auth
Write-Host "[2/5] Demarrage Auth (8081)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/k cd /d `"$base\Backend\AUTH-SERVICE`" && mvn spring-boot:run -DskipTests" -WindowStyle Normal

Start-Sleep -Seconds 3

# Patient
Write-Host "[3/5] Demarrage Patient (8082)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/k cd /d `"$base\Backend\PATIENT-SERVICE`" && mvn spring-boot:run -DskipTests" -WindowStyle Normal

Start-Sleep -Seconds 3

# Medical
Write-Host "[4/5] Demarrage Medical (8083)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/k cd /d `"$base\Backend\MEDICAL-SERVICE`" && mvn spring-boot:run -DskipTests" -WindowStyle Normal

Start-Sleep -Seconds 3

# Angular Frontend
Write-Host "[5/5] Demarrage Angular Frontend (4200)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/k cd /d `"$base\SmartMedical-Angular`" && npx ng serve --proxy-config proxy.conf.json --port 4200" -WindowStyle Normal

Write-Host ""
Write-Host "===========================================" -ForegroundColor Green
Write-Host "   TOUS LES SERVICES LANCES !" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Gateway:   http://localhost:8080" -ForegroundColor White
Write-Host "Auth:      http://localhost:8081" -ForegroundColor White
Write-Host "Patient:   http://localhost:8082" -ForegroundColor White
Write-Host "Medical:   http://localhost:8083" -ForegroundColor White
Write-Host "Angular:   http://localhost:4200  <-- ouvrir dans navigateur" -ForegroundColor Green
Write-Host ""
Write-Host "Attendez 2-3 minutes que les services Spring Boot demarrent..." -ForegroundColor DarkYellow
