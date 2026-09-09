@echo off
REM Demarrage complet SmartMedical sur Docker Desktop
cd /d "%~dp0"

echo === Preparation reseau Docker ===
docker network inspect smartmedical-network >nul 2>&1 || docker network create smartmedical-network

echo === Nettoyage anciens conteneurs SmartMedical ===
for %%c in (
  smartmedical-mongodb
  smartmedical-zookeeper
  smartmedical-kafka
  smartmedical-auth
  smartmedical-patient
  smartmedical-medical
  smartmedical-scan
  smartmedical-user
  smartmedical-ai
  smartmedical-sipdetect-alzheimer
  smartmedical-gateway
  smartmedical-frontend
) do docker rm -f %%c >nul 2>&1

echo === Tag images de secours si necessaire ===
docker image inspect smartmedicalfinal-main-ai-service:latest >nul 2>&1 || (
  docker tag smartmedical-ai:latest smartmedicalfinal-main-ai-service:latest 2>nul
  docker tag smartmedical/ai-service:latest smartmedicalfinal-main-ai-service:latest 2>nul
)
docker image inspect smartmedical/gateway-service:latest >nul 2>&1 && (
  docker tag smartmedical/gateway-service:latest smartmedical/gateway-service:latest >nul 2>&1
)

echo === Demarrage de tous les services ===
docker compose up -d --no-build --force-recreate
if errorlevel 1 (
  echo Build manquant, lancement avec build...
  docker compose up -d --build
)

echo.
echo === Application SmartMedical demarree ===
echo Frontend Angular : http://localhost:4200
echo API Gateway      : http://localhost:8088
echo Auth Service     : http://localhost:8081
echo AI Service       : http://localhost:8086
echo Alzheimer IA     : http://localhost:8087
echo MongoDB          : localhost:27017
echo.
docker compose ps
