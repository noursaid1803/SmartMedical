@echo off
echo ============================================
echo AI Analysis Service - Setup
echo ============================================
echo.

REM Créer l'environnement virtuel Python
echo Creating virtual environment...
python -m venv venv

REM Activer l'environnement
echo Activating virtual environment...
call venv\Scripts\activate.bat

REM Installer les dépendances
echo Installing dependencies...
pip install --upgrade pip
pip install -r requirements.txt

echo.
echo ============================================
echo Setup complete!
echo.
echo To start the service, run: start.bat
echo.
echo API will be available at: http://localhost:8084
echo Documentation at: http://localhost:8084/docs
echo ============================================
pause
