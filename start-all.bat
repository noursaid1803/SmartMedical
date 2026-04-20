@echo off
echo ==========================================
echo  SMART MEDICAL - Démarrage des Services
echo ==========================================
echo.

:: Arrêter les processus existants
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul

echo [1/4] Démarrage Gateway (8080)...
start "Gateway 8080" cmd /k "cd /d C:\Users\USER\Desktop\SmartMedical\Backend\GATEWAY-SERVICE && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo [2/4] Démarrage Auth (8081)...
start "Auth 8081" cmd /k "cd /d C:\Users\USER\Desktop\SmartMedical\Backend\AUTH-SERVICE && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo [3/4] Démarrage Patient (8082)...
start "Patient 8082" cmd /k "cd /d C:\Users\USER\Desktop\SmartMedical\Backend\PATIENT-SERVICE && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo [4/4] Démarrage Medical (8083)...
start "Medical 8083" cmd /k "cd /d C:\Users\USER\Desktop\SmartMedical\Backend\MEDICAL-SERVICE && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo.
echo ==========================================
echo  TOUS LES SERVICES DEMARRES !
echo ==========================================
echo.
echo - Gateway:  http://localhost:8080
echo - Auth:     http://localhost:8081
echo - Patient:  http://localhost:8082
echo - Medical:  http://localhost:8083
echo.
echo Angular: http://localhost:4200
echo.
echo Appuyez sur une touche pour fermer...
pause >nul
