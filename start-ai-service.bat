@echo off
echo ============================================
echo AI Analysis Service - Demarrage
echo ============================================
echo.
echo Ce service necessite Python 3.10+
echo.
echo Si Python n'est pas installe:
echo 1. Telechargez Python depuis https://python.org
echo 2. Cochez "Add Python to PATH" lors de l'installation
echo 3. Relancez ce script
echo.

cd /d "%~dp0\Backend\AI-ANALYSIS-SERVICE"

REM Verifier Python
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERREUR] Python n'est pas installe ou pas dans le PATH
    echo.
    pause
    exit /b 1
)

REM Creer venv si necessaire
if not exist venv (
    echo Creation de l'environnement virtuel...
    python -m venv venv
)

REM Activer venv
call venv\Scripts\activate.bat

REM Verifier/installer deps
echo Verification des dependances...
pip show fastapi >nul 2>&1
if %errorlevel% neq 0 (
    echo Installation des dependances...
    pip install -r requirements.txt
)

echo.
echo ============================================
echo Demarrage du AI Analysis Service...
echo Port: 8086
echo API: http://localhost:8086
echo Docs: http://localhost:8086/docs
echo ============================================
echo.

python -m uvicorn app.main:app --host 0.0.0.0 --port 8086 --reload

pause
