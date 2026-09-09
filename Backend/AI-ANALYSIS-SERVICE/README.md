# AI Analysis Service

Microservice Python pour l'analyse d'images médicales par intelligence artificielle.

## Architecture

- **Framework**: FastAPI
- **Modèle IA**: ResNet50 (PyTorch) fine-tuné pour la classification d'organes médicaux
- **Port**: 8086
- **Protocole**: HTTP/REST API

## Organes supportés

1. 🧠 Cerveau (Brain)
2. 🎀 Sein (Breast)
3. 🔬 Peau (Skin)
4. 👁️ Œil (Eye)
5. 🫁 Poumon (Lung)
6. 🫘 Foie (Liver)
7. ❤️ Cœur (Heart)

## Installation

### Prérequis
- Python 3.10+
- pip

### Setup Windows
```bash
setup.bat
```

### Setup Linux/Mac
```bash
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Démarrage

### Windows
```bash
start.bat
```

### Linux/Mac
```bash
./start.sh
```

### Docker
```bash
docker build -t ai-analysis-service .
docker run -p 8084:8084 ai-analysis-service
```

## API Endpoints

### Health Check
```
GET /
```

### Liste des organes supportés
```
GET /organs
```

### Prédiction d'organe
```
POST /predict
Content-Type: multipart/form-data

file: <image_file>
```

### Validation d'image
```
POST /validate
Content-Type: multipart/form-data

file: <image_file>
expected_organ: <organ_id>
```

### Analyse complète
```
POST /analyze
Content-Type: multipart/form-data

file: <image_file>
model_type: <optional>
```

### Analyse cancer pulmonaire (SipDetect V20)
```
POST /ai/analyze/lung-cancer
Content-Type: multipart/form-data

file: <lung_scan_image>
```

Statut du modèle :
```
GET /ai/analyze/lung-cancer/status
```

**Poids requis** : placez `best_hybrid_v20.pt` (export Kaggle) dans `models/lung_cancer/`.
Voir `models/lung_cancer/README.md`.

### Informations du modèle
```
GET /model/info
```

## Exemple de réponse

```json
{
  "success": true,
  "valid": true,
  "organ": "cerveau",
  "organ_name": "Cerveau",
  "confidence": 94.5,
  "expected_organ": "cerveau",
  "expected_organ_name": "Cerveau",
  "alternative_organs": [
    {"organ": "coeur", "confidence": 3.2},
    {"organ": "foie", "confidence": 1.8}
  ],
  "all_scores": {
    "cerveau": 94.5,
    "coeur": 3.2,
    "foie": 1.8,
    ...
  }
}
```

## Documentation

Une fois le service démarré, accédez à l'API à l'adresse : http://localhost:8086
echo Documentation à l'adresse : http://localhost:8086/docs

## Intégration avec Gateway

Le service doit être enregistré dans le Gateway Service pour être accessible via le port 8080.

Configuration dans le Gateway:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ai-analysis-service
          uri: http://localhost:8084
          predicates:
            - Path=/ai/**
```

## Notes

- Le modèle utilise ResNet50 pré-entraîné sur ImageNet
- Pour un entraînement spécifique, fournir un dataset d'images médicales étiquetées
- Les images sont redimensionnées à 224x224 pixels pour l'analyse
