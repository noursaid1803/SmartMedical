@echo off
echo ==========================================
echo  SMART MEDICAL - Demarrage des Services
echo ==========================================
echo.

:: Arrêter les processus existants
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul

echo [1/6] Demarrage Gateway (8080)...
start "Gateway 8080" cmd /k "cd /d \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\GATEWAY-SERVICE\" && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo [2/6] Demarrage Auth (8081)...
start "Auth 8081" cmd /k "cd /d \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\AUTH-SERVICE\" && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo [3/6] Demarrage Patient (8082)...
start "Patient 8082" cmd /k "cd /d \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\PATIENT-SERVICE\" && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo [4/6] Demarrage Medical (8083)...
start "Medical 8083" cmd /k "cd /d \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\MEDICAL-SERVICE\" && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo [5/6] Demarrage AI Analysis (8086)...
start "AI Analysis 8086" cmd /k "cd /d \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\AI-ANALYSIS-SERVICE\" && python -m uvicorn app.main:app --host 0.0.0.0 --port 8086"
timeout /t 15 /nobreak >nul

echo [6/6] Demarrage Frontend Angular (4200)...
start "Frontend 4200" cmd /k "cd /d \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Frontend\SmartMedical\" && ng serve --open=false --port=4200 --disable-host-check"

echo.
echo ==========================================
echo  SERVICES EN DEMARRAGE !
echo ==========================================
echo.
echo - Gateway:      http://localhost:8080
echo - Auth:         http://localhost:8081
echo - Patient:      http://localhost:8082
echo - Medical:      http://localhost:8083
echo - AI Analysis:  http://localhost:8086
echo - Frontend:     http://localhost:4200
echo.
echo Fermez cette fenetre pour arreter le lanceur.
echo Les services continueront a tourner.
echo.
echo Appuyez sur une touche pour fermer CE lanceur uniquement...
pause >nul
