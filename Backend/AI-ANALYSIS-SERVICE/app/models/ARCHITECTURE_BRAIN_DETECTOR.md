# Architecture du Détecteur d'IRM Cérébrale

## 🎯 Vue d'Ensemble

**Nom du système** : `BrainAnatomicalFinal`  
**Fichier** : `brain_anatomical_final.py`  
**Type** : Détecteur anatomique biomédical basé sur l'analyse d'images  
**Seuil de décision** : 4 critères positifs sur 7 (score ≥ 4/7)

---

## 🏗️ Architecture Complète

```
┌─────────────────────────────────────────────────────────────────────┐
│                         IMAGE D'ENTRÉE (Bytes)                       │
│                    Conversion PIL → NumPy Array (Grayscale)          │
└────────────────────────────────┬──────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│                     ÉTAPE 1: SEGMENTATION DU CERVEAU                 │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │ • Gaussian Blur (5×5)                                           ││
│  │ • Double seuillage : Otsu + Seuil adaptatif (0.7×médiane)       ││
│  │ • Combinaison bitwise OR                                         ││
│  │ • Morphologie : CLOSE (7×7) puis OPEN (5×5)                     ││
│  │ • Connected Components Analysis                                  ││
│  └─────────────────────────────────────────────────────────────────┘│
│                                                                      │
│  Sélection : Plus grande composante centrale (aire 15%-85%)           │
└────────────────────────────────┬──────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    ÉTAPE 2: ANALYSE ANATOMIQUE (7 critères)          │
│                                                                      │
│  Critère 1: Taille cerveau         │ 25%-75% de l'image              │
│  Critère 2: Forme ovale           │ Aspect ratio [0.7, 1.3]         │
│  Critère 3: Position centrale     │ Distance centre < 25%           │
│  Critère 4: Fond noir             │ Bordures < 40 intensité         │
│  Critère 5: Ventricules           │ Zones sombres bilatérales       │
│  Critère 6: Symétrie bilatérale   │ Corrélation > 0.6               │
│  Critère 7: Texture corticale     │ Variance + Gradient             │
└────────────────────────────────┬──────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│                     ÉTAPE 3: DÉCISION & CONFIANCE                    │
│                                                                      │
│  IF score ≥ 4:                                                       │
│     → CERVEAU ✅                                                     │
│     → Confiance = 55% + (score-4)×8% + area×15%                     │
│                                                                      │
│  ELSE:                                                               │
│     → REJET ❌                                                       │
│     → Raison = critères manquants                                    │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🔬 Description Scientifique des Choix

### 1. Segmentation Multi-Seuils

**Pourquoi Otsu + Seuil adaptatif ?**

```python
# Otsu : séparation automatique histogramme
_, thresh1 = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)

# Seuil adaptatif : capture tissus différents
thresh_val = int(median * 0.7)
_, thresh2 = cv2.threshold(blurred, thresh_val, 255, cv2.THRESH_BINARY_INV)

# Combinaison
combined = cv2.bitwise_or(thresh1, thresh2)
```

**Justification biomédicale** :
- L'IRM cerveau contient 3 tissus principaux : **matière grise**, **matière blanche**, **liquide céphalorachidien (LCR)**
- Chaque tissu a une intensité différente sur l'échelle de gris
- Un seul seuil ne capture pas tous les tissus cérébraux
- La combinaison permet de segmenter l'ensemble du volume cérébral

**Référence** : Otsu N. (1979). "A threshold selection method from gray-level histograms". IEEE Trans. SMC.

---

### 2. Morphologie Mathématique

**Pourquoi CLOSE puis OPEN ?**

```python
kernel_close = np.ones((7, 7), np.uint8)
kernel_open = np.ones((5, 5), np.uint8)

closed = cv2.morphologyEx(combined, cv2.MORPH_CLOSE, kernel_close)  # Comble trous
opened = cv2.morphologyEx(closed, cv2.MORPH_OPEN, kernel_open)      # Supprime bruit
```

**Justification anatomique** :
- **CLOSE (dilatation + érosion)** : Reconnecte les régions fragmentées (sulci coupés par le seuillage)
- **OPEN (érosion + dilatation)** : Élimine les petits artefacts (vaisseaux, bruit)
- Le cerveau est une structure **connexe unique** malgré les sillons (sulci)

---

### 3. Sélection par Connected Components

**Pourquoi connected components et pas contours ?**

```python
num_labels, labels, stats, centroids = cv2.connectedComponentsWithStats(opened)
```

**Justification scientifique** :
- Les IRM cérébrales n'ont pas de **contour externe net** (gradients faibles crâne/cerveau)
- L'analyse de **régions** est plus robuste que l'analyse de **contours** pour les images médicales
- Connected components capture les **vraies régions anatomiques**, pas les artefacts de contour

---

### 4. Les 7 Critères Anatomiques

#### Critère 1 : Taille du Cerveau (25%-75%)
**Base anatomique** : En IRM axiale, le cerveau occupe typiquement 30-60% de l'image. Un zoom excessif ou insuffisant indique une acquisition non-standard.

#### Critère 2 : Forme Ovale (Aspect 0.7-1.3)
**Base anatomique** : Le crâne humain est **ovoïde**, jamais parfaitement circulaire (aspect = 1.0) ni fortement elliptique. Le ratio largeur/hauteur du cerveau est typiquement 0.8-1.2.

#### Critère 3 : Position Centrale
**Base anatomique** : Le cerveau est centré dans le champ de vue IRM. Une position excentrée suggère une coupe non-axiale ou un artefact.

#### Critère 4 : Fond Noir
**Base physique** : Les IRM utilisent des séquences qui rendent le **fond très sombre** (absence de signal). Si le fond est clair, ce n'est pas une IRM standard.

#### Critère 5 : Ventricules Latéraux
**Algorithme** :
```python
# Zones sombres au centre (LCR dans ventricules)
dark_threshold = mean_center * 0.65
left_dark = np.sum(left_half < dark_threshold) / left_half.size
right_dark = np.sum(right_half < dark_threshold) / right_half.size

# Présence bilatérale = ventricules caractéristiques
if left_dark > 0.03 and right_dark > 0.03:
    ventricule_score = 0.5 + symétrie_bonus
```

**Base anatomique** : Les **ventricules latéraux** contiennent du LCR (liquide) qui apparaît **sombre** en T1 et **clair** en T2. Leur présence bilatérale symétrique est **pathognomonique** d'une IRM cérébrale axiale.

#### Critère 6 : Symétrie Bilatérale
**Algorithme** :
```python
# Corrélation entre hémisphères gauche et droite
left_norm = (left - mean_left) / std_left
right_flipped = np.fliplr((right - mean_right) / std_right)
similarity = 1 - mean(|left_norm - right_flipped|) / 2
```

**Base anatomique** : Le cerveau humain présente une **symétrie bilatérale** (hémisphères gauche/droite). Cette symétrie est un marqueur fort d'authenticité cérébrale.

#### Critère 7 : Texture Corticale
**Algorithme** :
```python
# Variance locale (gyri = varié, sulci = varié)
std_dev = np.std(brain_pixels)

# Gradient de Sobel (détecte bordures gyri/sulci)
grad_x = cv2.Sobel(img, cv2.CV_64F, 1, 0, ksize=3)
grad_y = cv2.Sobel(img, cv2.CV_64F, 0, 1, ksize=3)
texture_score = (std_dev/40)*0.5 + (mean_grad/5)*0.5
```

**Base anatomique** : Le cortex cérébral présente un motif **caractéristique de plis** (gyri et sulci) créant une texture haute-fréquence unique. Ce motif n'existe dans aucun autre organe.

---

## 📊 Pipeline Complet Détaillé

### Phase 1 : Prétraitement (O(1))
```
Entrée: image_bytes (PNG/JPG)
  ↓
PIL.Image.open() → convert('L') → np.array()
  ↓
Sortie: img_array[H×W], uint8
```

### Phase 2 : Segmentation (O(H×W))
```
GaussianBlur(5×5)
  ↓
├─→ Otsu Threshold
├─→ Adaptive Threshold (0.7×median)
↓
Bitwise OR
  ↓
Morphology: CLOSE(7×7) → OPEN(5×5)
  ↓
ConnectedComponentsWithStats()
  ↓
Sélection: argmax(area × centrality)
  ↓
Sortie: brain_mask, {cx, cy, area_ratio, aspect_ratio}
```

### Phase 3 : Extraction de Features (O(H×W))
```
Pour chaque critère anatomique:
  ├─→ Extraction ROI
  ├─→ Calcul métrique
  └─→ Binarisation (0 ou 1 point)
  ↓
Sortie: score [0-7], details[]
```

### Phase 4 : Décision (O(1))
```
IF score ≥ 4:
   confidence = f(score, area_ratio)
   RETURN {is_brain: True, confidence, method}
ELSE:
   RETURN {is_brain: False, rejection_reason}
```

---

## 🎓 Complexité Algorithmique

| Phase | Complexité | Temps typique (512×512) |
|-------|-----------|------------------------|
| Prétraitement | O(1) | < 1 ms |
| Segmentation | O(H×W) | ~10 ms |
| Features | O(H×W) | ~15 ms |
| Décision | O(1) | < 1 ms |
| **TOTAL** | **O(H×W)** | **~30 ms** |

---

## 📚 Références Scientifiques

1. **Otsu N.** (1979). "A threshold selection method from gray-level histograms". IEEE Trans. Systems, Man, and Cybernetics.

2. **Gonzalez & Woods** (2018). "Digital Image Processing" (4th ed.). Pearson. Chapitres 9 (Morphologie) et 10 (Segmentation).

3. **Huang et al.** (2015). "Brain extraction from magnetic resonance images using hierarchical segmentation". Medical Physics.

4. **Smith S.M.** (2002). "Fast robust automated brain extraction". Human Brain Mapping.

5. **Ségonne et al.** (2004). "A hybrid approach to the skull stripping problem in MRI". NeuroImage.

---

## 🏥 Validation Clinique (Recommandée)

Pour valider ce détecteur dans un contexte médical :

1. **Dataset de validation** :
   - 200 IRM cerveau réelles (multiples séquences T1/T2/FLAIR)
   - 100 images non-cerveau (poumons, abdomen, diagrammes, objets)

2. **Métriques d'évaluation** :
   - Sensitivity (True Positive Rate)
   - Specificity (True Negative Rate)
   - F1-Score
   - AUC-ROC

3. **Seuil optimal** :
   - Actuel : score ≥ 4/7
   - À ajuster selon ROC curve sur dataset de validation

---

## 💡 Extensions Futures

1. **CNN Légér** : Remplacer les règles 5-7 par un mini-CNN entraîné sur patches cérébraux
2. **Multi-séquence** : Intégrer T1, T2, FLAIR pour plus de robustesse
3. **3D** : Analyser les coupes adjacentes pour validation temporelle

---

## ✍️ Auteur & Contexte

**Projet** : SmartMedical - PFE  
**Module** : AI-Analysis-Service  
**Version** : 1.0.0  
**Date** : Avril 2026

---

## 🔧 Utilisation

```python
from app.models.brain_anatomical_final import get_brain_anatomical_final

detector = get_brain_anatomical_final()
result = detector.detect(image_bytes)

# Résultat
{
    'is_brain': True/False,
    'confidence': 65.5,  # %
    'method': 'anatomical_final',
    'details': {
        'score': 5,
        'total_checks': 7,
        'checks': ['✓ taille_cerveau', '✓ forme_ovale', ...],
        'ventricles': 0.6,
        'symmetry': 0.75,
        'texture': 0.8
    }
}
```
