#!/bin/bash

echo "Starting AI Analysis Service..."
echo "Port: 8086"
echo ""

# Activer l'environnement virtuel si existant
if [ -d "venv" ]; then
    source venv/bin/activate
fi

# Démarrer le serveur
python -m uvicorn app.main:app --host 0.0.0.0 --port 8086 --reload
