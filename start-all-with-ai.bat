@echo off
echo ============================================
echo SmartMedical - Démarrage de tous les services
echo ============================================
echo.
echo Services à démarrer :
echo - Gateway Service (Port 8080)
echo - Auth Service (Port 8081)
echo - Patient Service (Port 8082)
echo - Medical Service (Port 8083)
echo - AI Analysis Service (Port 8086) - NOUVEAU
echo.
pause

cd /d "%~dp0"

REM Vérifier si Python est installé pour le AI Service
echo Verification de Python...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERREUR] Python n'est pas installe ou n'est pas dans le PATH
    echo Veuillez installer Python 3.10+ pour utiliser le AI Analysis Service
    pause
    exit /b 1
)

REM Vérifier si le venv existe pour le AI Service
if not exist "Backend\AI-ANALYSIS-SERVICE\venv" (
    echo.
    echo [INFO] Configuration du AI Analysis Service...
    echo Creation de l'environnement virtuel Python...
    cd Backend\AI-ANALYSIS-SERVICE
    python -m venv venv
    call venv\Scripts\activate.bat
    echo Installation des dependances...
    pip install -r requirements.txt
    cd ..\..
)

REM 1. Démarrer le Gateway Service
echo.
echo [1/5] Demarrage du Gateway Service (Port 8080)...
start "Gateway Service" cmd /k "cd \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\GATEWAY-SERVICE\" && mvn spring-boot:run -DskipTests -q"

timeout /t 5 /nobreak >nul

REM 2. Démarrer le Auth Service
echo [2/5] Demarrage du Auth Service (Port 8081)...
start "Auth Service" cmd /k "cd \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\AUTH-SERVICE\" && mvn spring-boot:run -DskipTests -q"

timeout /t 5 /nobreak >nul

REM 3. Démarrer le Patient Service
echo [3/5] Demarrage du Patient Service (Port 8082)...
start "Patient Service" cmd /k "cd \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\PATIENT-SERVICE\" && mvn spring-boot:run -DskipTests -q"

timeout /t 5 /nobreak >nul

REM 4. Démarrer le Medical Service
echo [4/5] Demarrage du Medical Service (Port 8083)...
start "Medical Service" cmd /k "cd \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\MEDICAL-SERVICE\" && mvn spring-boot:run -DskipTests -q"

timeout /t 5 /nobreak >nul

REM 5. Démarrer le AI Analysis Service
echo [5/5] Demarrage du AI Analysis Service (Port 8086)...
start "AI Analysis Service" cmd /k "cd \"c:\Users\arijh\Downloads\SmartMedical-main (1)\SmartMedical-main\Backend\AI-ANALYSIS-SERVICE\" && call venv\Scripts\activate.bat && python -m uvicorn app.main:app --host 0.0.0.0 --port 8086"

timeout /t 3 /nobreak >nul

echo.
echo ============================================
echo Tous les services ont ete demarres !
echo.
echo URLs disponibles :
echo - Gateway API: http://localhost:8080
echo - Auth Service: http://localhost:8081
echo - Patient Service: http://localhost:8082
echo - Medical Service: http://localhost:8083
echo - AI Analysis Service: http://localhost:8086
echo - AI API Docs: http://localhost:8086/docs
echo.
echo Frontend Angular:
echo - cd SmartMedical-Angular
echo - ng serve
echo.
echo ============================================
pause
