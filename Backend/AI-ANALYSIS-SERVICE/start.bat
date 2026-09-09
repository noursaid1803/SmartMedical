@echo off
echo Starting AI Analysis Service...
echo Port: 8086
echo.
cd /d "%~dp0"
python -m uvicorn app.main:app --host 0.0.0.0 --port 8086 --reload
