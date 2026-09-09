# Algorithme de Validation des Scans Médicaux par Spécialité

## Vue d'ensemble
Ce système valide les uploads d'images médicales (IRM, scanner) en fonction de la spécialité médicale du médecin. Il utilise des algorithmes de détection d'organes et des règles de validation basées sur les dimensions, la forme et le type de fichier.

## Spécialités Supportées

### 1. CERVEAU (Tumeurs Cérébrales)
- **Types de fichiers acceptés**: DICOM, NIfTI, PNG, JPEG
- **Taille maximale**: 50 MB
- **Dimensions**: 256x256 à 2048x2048 pixels
- **Organes attendus**: brain, cerebrum, cerebellum, cortex
- **Confiance minimale**: 0.7
- **Ratio d'aspect**: 0.5 - 1.0 (images plutôt carrées)

### 2. ALZHEIMER
- **Types de fichiers acceptés**: DICOM, NIfTI, PNG, JPEG
- **Taille maximale**: 100 MB
- **Dimensions**: 256x256 à 2048x2048 pixels
- **Organes attendus**: brain, cerebrum, hippocampus, cortex
- **Confiance minimale**: 0.75 (plus élevée pour Alzheimer)
- **Ratio d'aspect**: 0.5 - 1.0

### 3. POUMON (Cancer du Poumon)
- **Types de fichiers acceptés**: DICOM, NIfTI, PNG, JPEG
- **Taille maximale**: 50 MB
- **Dimensions**: 256x256 à 2048x2048 pixels
- **Organes attendus**: lung, pulmonary, chest, thorax
- **Confiance minimale**: 0.7
- **Ratio d'aspect**: 0.6 - 1.5 (poumons peuvent être plus larges)

### 4. SEIN (Cancer du Sein)
- **Types de fichiers acceptés**: DICOM, PNG, JPEG
- **Taille maximale**: 30 MB
- **Dimensions**: 512x512 à 4096x4096 pixels
- **Organes attendus**: breast, mammography, mammary
- **Confiance minimale**: 0.7
- **Ratio d'aspect**: 0.8 - 1.2

### 5. PEAU (Cancer de la Peau)
- **Types de fichiers acceptés**: PNG, JPEG, JPG
- **Taille maximale**: 10 MB
- **Dimensions**: 224x224 à 1024x1024 pixels
- **Organes attendus**: skin, dermis, epidermis, lesion
- **Confiance minimale**: 0.6
- **Ratio d'aspect**: 0.8 - 1.0

### 6. OEIL (Rétinopathie Diabétique)
- **Types de fichiers acceptés**: PNG, JPEG, DICOM
- **Taille maximale**: 15 MB
- **Dimensions**: 224x224 à 1024x1024 pixels
- **Organes attendus**: eye, retina, fundus, optic
- **Confiance minimale**: 0.7
- **Ratio d'aspect**: 0.8 - 1.0

## Algorithme de Validation

### Étape 1: Validation du Type de Fichier
Vérifie que l'extension du fichier correspond aux types acceptés pour la spécialité.

### Étape 2: Validation de la Taille
Vérifie que la taille du fichier ne dépasse pas la limite maximale pour la spécialité.

### Étape 3: Validation des Dimensions
Pour les images, vérifie:
- Largeur et hauteur dans les limites acceptées
- Ratio d'aspect (largeur/hauteur) dans la plage acceptable

### Étape 4: Détection de l'Organe
Utilise le service AI (http://localhost:8086) pour détecter l'organe présent dans l'image.
- Si le service AI n'est pas disponible, utilise un fallback basé sur la spécialité

### Étape 5: Validation de l'Organe
Vérifie que l'organe détecté correspond aux organes attendus pour la spécialité.
- Exemple: Pour ALZHEIMER, seul "brain" ou "cerebrum" est accepté

### Étape 6: Calcul de la Confiance
Calcule un score de confiance basé sur:
- Correspondance de l'organe (40%)
- Taille du fichier optimale (30%)
- Proximité des dimensions optimales (30%)

### Étape 7: Validation Finale
Le scan est accepté si:
- Toutes les validations précédentes sont passées
- Le score de confiance est supérieur au minimum requis

## API Endpoints

### POST /scan/upload
Upload un scan avec validation automatique.

**Paramètres:**
- `file`: MultipartFile - Le fichier image
- `patientId`: String - ID du patient
- `doctorId`: String - ID du médecin
- `specialtyCode`: String - Code de la spécialité (CERVEAU, POUMON, etc.)
- `scanType`: String (optionnel) - Type de scan

**Réponse:**
```json
{
  "valid": true,
  "message": "Scan uploadé et validé avec succès",
  "scan": {...},
  "validation": {
    "valid": true,
    "specialtyCode": "CERVEAU",
    "validationMessage": "Scan validé avec succès pour la spécialité CERVEAU",
    "detectedOrgan": "brain",
    "confidence": 0.85,
    "width": 512,
    "height": 512,
    "aspectRatio": 1.0
  }
}
```

### GET /scan/validation-rules/{specialtyCode}
Récupère les règles de validation pour une spécialité.

**Réponse:**
```json
{
  "allowedFileTypes": ["DICOM", "NIfTI", "PNG", "JPEG"],
  "maxFileSize": 52428800,
  "minWidth": 256,
  "minHeight": 256,
  "maxWidth": 2048,
  "maxHeight": 2048,
  "expectedOrgans": ["brain", "cerebrum", "cerebellum", "cortex"],
  "minConfidence": 0.7,
  "maxAspectRatio": 1.0,
  "minAspectRatio": 0.5
}
```

### GET /scan/doctor/{doctorId}/specialty/{specialtyCode}
Récupère tous les scans d'un médecin pour une spécialité spécifique.

## Intégration avec le Service AI

Le système de validation s'intègre avec le service AI-ANALYSIS-SERVICE pour:
1. La détection d'organes dans les images
2. L'analyse de confiance
3. La validation automatique

**Endpoint AI utilisé:**
- POST http://localhost:8086/predict/organ

## Exemples d'Utilisation

### Exemple 1: Upload d'un IRM Cérébral pour Alzheimer
```bash
curl -X POST http://localhost:8084/scan/upload \
  -F "file=@brain_mri.dicom" \
  -F "patientId=patient123" \
  -F "doctorId=doctor456" \
  -F "specialtyCode=ALZHEIMER" \
  -F "scanType=BRAIN_MRI"
```

**Résultat attendu:** Validé si l'image contient un cerveau avec confiance > 0.75

### Exemple 2: Upload d'un Scanner Pulmonaire
```bash
curl -X POST http://localhost:8084/scan/upload \
  -F "file=@lung_scan.dicom" \
  -F "patientId=patient123" \
  -F "doctorId=doctor456" \
  -F "specialtyCode=POUMON"
```

**Résultat attendu:** Validé si l'image contient des poumons avec confiance > 0.7

### Exemple 3: Upload d'une Image Incorrecte
```bash
curl -X POST http://localhost:8084/scan/upload \
  -F "file=@skin_lesion.jpg" \
  -F "patientId=patient123" \
  -F "doctorId=doctor456" \
  -F "specialtyCode=ALZHEIMER"
```

**Résultat attendu:** Rejeté - organe détecté (skin) ne correspond pas à la spécialité (ALZHEIMER)

## Configuration

### ScanValidationService
Le service est configuré avec des règles prédéfinies pour chaque spécialité. Pour modifier les règles:

1. Ouvrir `ScanValidationService.java`
2. Modifier les règles dans le bloc `static`
3. Redémarrer le SCAN-SERVICE

### Integration AI Service
Assurez-vous que le service AI-ANALYSIS-SERVICE est démarré sur le port 8086:
```bash
cd Backend/AI-ANALYSIS-SERVICE
python -m uvicorn app.main:app --host 0.0.0.0 --port 8086
```

## Tests

Pour tester le système de validation:

1. **Vérifier que tous les services sont démarrés:**
   - SCAN-SERVICE: http://localhost:8084
   - AI-ANALYSIS-SERVICE: http://localhost:8086

2. **Tester les règles de validation:**
   ```bash
   curl http://localhost:8084/scan/validation-rules/CERVEAU
   ```

3. **Tester l'upload avec un fichier valide:**
   ```bash
   curl -X POST http://localhost:8084/scan/upload \
     -F "file=@test_brain.png" \
     -F "patientId=test" \
     -F "doctorId=test" \
     -F "specialtyCode=CERVEAU"
   ```

4. **Tester l'upload avec un fichier invalide:**
   ```bash
   curl -X POST http://localhost:8084/scan/upload \
     -F "file=@test_skin.jpg" \
     -F "patientId=test" \
     -F "doctorId=test" \
     -F "specialtyCode=CERVEAU"
   ```

## Sécurité

- Validation stricte des types de fichiers
- Limites de taille pour prévenir les attaques
- Vérification des dimensions pour éviter les images malformées
- Détection d'organes pour s'assurer de la pertinence médicale
- Score de confiance pour évaluer la qualité de l'image

## Performance

- Validation locale pour les types de fichiers et dimensions (rapide)
- Appel au service AI uniquement pour la détection d'organes
- Caching possible pour les règles de validation
- Support des formats médicaux standards (DICOM, NIfTI)

## Maintenance

Pour ajouter une nouvelle spécialité:

1. Ajouter les règles dans `SPECIALTY_VALIDATION_RULES` dans `ScanValidationService.java`
2. Ajouter l'organe attendu dans `getExpectedOrganForSpecialty()`
3. Redémarrer le SCAN-SERVICE
4. Tester avec des images de la nouvelle spécialité
