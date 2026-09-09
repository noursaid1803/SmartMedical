@echo off
chcp 65001 >nul
echo ==========================================
echo  SMART MEDICAL - Démarrage des Services
echo ==========================================
echo.

REM Définir le chemin de base
set "BASEDIR=%~dp0"

echo Chemin du projet: %BASEDIR%
echo.

REM Arrêter les processus existants
echo [INFO] Arrêt des processus Java existants...
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul

echo [1/4] Démarrage Gateway (8080)...
start "Gateway 8080" cmd /k "cd /d "%BASEDIR%Backend\GATEWAY-SERVICE" && mvn spring-boot:run -DskipTests -q"
timeout /t 30 /nobreak >nul

echo [2/4] Démarrage Auth (8081)...
start "Auth 8081" cmd /k "cd /d "%BASEDIR%Backend\AUTH-SERVICE" && mvn spring-boot:run -DskipTests -q"
timeout /t 30 /nobreak >nul

echo [3/4] Démarrage Patient (8082)...
start "Patient 8082" cmd /k "cd /d "%BASEDIR%Backend\PATIENT-SERVICE" && mvn spring-boot:run -DskipTests -q"
timeout /t 30 /nobreak >nul

echo [4/4] Démarrage Medical (8083)...
start "Medical 8083" cmd /k "cd /d "%BASEDIR%Backend\MEDICAL-SERVICE" && mvn spring-boot:run -DskipTests -q"
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
pause
