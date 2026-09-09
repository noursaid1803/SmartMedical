# ================================================================
# SmartMedical - Script de démarrage Docker
# ================================================================

param(
    [switch]$Build,    # Force le rebuild des images
    [switch]$Stop,     # Arrête tous les services
    [switch]$Logs,     # Affiche les logs
    [switch]$Clean     # Supprime tout (images, volumes, conteneurs)
)

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ComposeFile = Join-Path $ProjectDir "docker-compose.yml"

function Write-Header {
    param([string]$Title)
    Write-Host ""
    Write-Host "=" * 60 -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "=" * 60 -ForegroundColor Cyan
}

function Check-Docker {
    try {
        $info = docker info 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Docker Desktop n'est pas démarré !" -ForegroundColor Red
            Write-Host "   Veuillez démarrer Docker Desktop et réessayer." -ForegroundColor Yellow
            exit 1
        }
        Write-Host "✅ Docker Desktop est en marche" -ForegroundColor Green
    } catch {
        Write-Host "❌ Docker n'est pas installé ou accessible" -ForegroundColor Red
        exit 1
    }
}

# Arrêt des services
if ($Stop) {
    Write-Header "Arrêt des services SmartMedical"
    docker compose -f $ComposeFile down
    Write-Host "✅ Tous les services ont été arrêtés" -ForegroundColor Green
    exit 0
}

# Nettoyage complet
if ($Clean) {
    Write-Header "Nettoyage complet SmartMedical"
    Write-Host "⚠️  Cela va supprimer tous les conteneurs, images et volumes !" -ForegroundColor Yellow
    $confirm = Read-Host "Continuer ? (oui/non)"
    if ($confirm -eq "oui") {
        docker compose -f $ComposeFile down -v --rmi all
        Write-Host "✅ Nettoyage terminé" -ForegroundColor Green
    }
    exit 0
}

# Affichage des logs
if ($Logs) {
    docker compose -f $ComposeFile logs -f
    exit 0
}

# Démarrage principal
Write-Header "SmartMedical - Docker Compose"
Check-Docker

Write-Host ""
Write-Host "📦 Services qui vont être lancés :" -ForegroundColor White
Write-Host "   🗄️  MongoDB          -> localhost:27017" -ForegroundColor Gray
Write-Host "   📨  Kafka            -> localhost:9092" -ForegroundColor Gray
Write-Host "   🔐  Auth-Service     -> localhost:8081" -ForegroundColor Gray
Write-Host "   🏥  Patient-Service  -> localhost:8082" -ForegroundColor Gray
Write-Host "   💊  Medical-Service  -> localhost:8083" -ForegroundColor Gray
Write-Host "   🔬  Scan-Service     -> localhost:8084" -ForegroundColor Gray
Write-Host "   👤  User-Service     -> localhost:8085" -ForegroundColor Gray
Write-Host "   🤖  AI-Service       -> localhost:8086" -ForegroundColor Gray
Write-Host "   🌐  Gateway-Service  -> localhost:8088" -ForegroundColor Gray
Write-Host "   💻  Frontend Angular -> http://localhost:4200" -ForegroundColor Cyan
Write-Host ""

if ($Build) {
    Write-Host "🔨 Construction des images Docker..." -ForegroundColor Yellow
    docker compose -f $ComposeFile build --no-cache
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Échec de la construction des images" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Images construites avec succès" -ForegroundColor Green
}

Write-Host "🚀 Démarrage des services..." -ForegroundColor Yellow
docker compose -f $ComposeFile up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Tous les services sont en cours de démarrage !" -ForegroundColor Green
    Write-Host ""
    Write-Host "🌐 Application disponible dans ~2 minutes sur :" -ForegroundColor White
    Write-Host "   http://localhost:4200" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📊 Vérifier les logs : .\start-docker.ps1 -Logs" -ForegroundColor Gray
    Write-Host "⏹️  Arrêter           : .\start-docker.ps1 -Stop" -ForegroundColor Gray
    Write-Host "🔨 Rebuild & Start   : .\start-docker.ps1 -Build" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🐳 Ou ouvrez Docker Desktop pour surveiller les conteneurs" -ForegroundColor Gray
} else {
    Write-Host "❌ Erreur lors du démarrage" -ForegroundColor Red
    Write-Host "   Vérifiez les logs : docker compose logs" -ForegroundColor Yellow
    exit 1
}
