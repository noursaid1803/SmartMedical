@echo off
chcp 65001 >nul
title SmartMedical - Démarrage des services

echo ==========================================
echo   DEMARRAGE SMARTMEDICAL
echo ==========================================
echo.

REM Arrêter les processus Java existants
echo [1/6] Arret des services existants...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM httpd.exe 2>nul
timeout /t 3 /nobreak >nul

REM Définir le répertoire de base
set "BASEDIR=c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main"

REM Démarrer Gateway (8088)
echo [2/6] Demarrage Gateway sur port 8088...
start "Gateway 8088" cmd /k "cd /d "%BASEDIR%\Backend\GATEWAY-SERVICE" && mvn spring-boot:run -DskipTests -q"
timeout /t 15 /nobreak >nul

REM Démarrer Auth (8081)
echo [3/6] Demarrage Auth sur port 8081...
start "Auth 8081" cmd /k "cd /d "%BASEDIR%\Backend\AUTH-SERVICE" && mvn spring-boot:run -DskipTests -q"
timeout /t 15 /nobreak >nul

REM Démarrer Patient (8082)
echo [4/6] Demarrage Patient sur port 8082...
start "Patient 8082" cmd /k "cd /d "%BASEDIR%\Backend\PATIENT-SERVICE" && mvn spring-boot:run -DskipTests -q"
timeout /t 15 /nobreak >nul

REM Démarrer Medical (8083)
echo [5/6] Demarrage Medical sur port 8083...
start "Medical 8083" cmd /k "cd /d "%BASEDIR%\Backend\MEDICAL-SERVICE" && mvn spring-boot:run -DskipTests -q"
timeout /t 30 /nobreak >nul

REM Démarrer Angular
echo [6/6] Demarrage Angular sur port 4200...
start "Angular 4200" cmd /k "cd /d "%BASEDIR%\SmartMedical-Angular" && ng serve"

echo.
echo ==========================================
echo   TOUS LES SERVICES DEMARRES !
echo ==========================================
echo.
echo - Gateway:  http://localhost:8088
echo - Auth:     http://localhost:8081
echo - Patient:  http://localhost:8082
echo - Medical:  http://localhost:8083
echo - Angular:  http://localhost:4200
echo.
echo Appuyez sur une touche pour fermer...
pause >nul
