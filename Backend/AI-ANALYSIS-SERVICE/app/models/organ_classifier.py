"""
Modèle CNN pour la classification d'organes médicaux à partir d'images IRM/CT
Utilise PyTorch avec un modèle ResNet50 pré-entraîné fine-tuné pour les images médicales
ET un classificateur HuggingFace pour une meilleure précision
"""

import torch
import torch.nn as nn
import torchvision.transforms as transforms
from torchvision.models import resnet50, ResNet50_Weights
from PIL import Image
import io
import os

# Importer le détecteur ANATOMIQUE FINAL
from app.models.brain_anatomical_final import BrainAnatomicalFinal, get_brain_anatomical_final
from app.models.lung_opencv_detector import LungOpenCVDetector
from app.models.organ_geometry_detectors import get_organ_geometry_validator, MIN_ACCEPT_CONFIDENCE

# Classes d'organes médicaux
ORGAN_CLASSES = [
    'cerveau',      # Brain
    'sein',         # Breast
    'peau',         # Skin
    'oeil',         # Eye/Retina
    'poumon',       # Lung
    'foie',         # Liver
    'coeur',        # Heart
]

ORGAN_NAMES = {
    'cerveau': 'Cerveau',
    'sein': 'Sein',
    'peau': 'Peau',
    'oeil': 'Œil',
    'poumon': 'Poumon',
    'foie': 'Foie',
    'coeur': 'Cœur'
}


class MedicalOrganClassifier:
    """
    Classificateur d'organes médicaux basé sur ResNet50
    """
    
    def __init__(self, model_path: str = None):
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        self.num_classes = len(ORGAN_CLASSES)
        
        # Transforms pour prétraitement des images
        self.transforms = transforms.Compose([
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
            transforms.Normalize(
                mean=[0.485, 0.456, 0.406],
                std=[0.229, 0.224, 0.225]
            )
        ])
        
        # Initialiser le modèle CNN (fallback)
        self.model = self._build_model()
        
        # Charger les poids si disponibles
        if model_path and os.path.exists(model_path):
            self.load_weights(model_path)
        else:
            # Utiliser le modèle pré-entraîné sur ImageNet comme base
            print("Using pretrained ResNet50 as base model")
        
        self.model.to(self.device)
        self.model.eval()
        
        # Initialiser le détecteur ANATOMIQUE FINAL
        print("[MedicalOrganClassifier] Chargement du détecteur ANATOMIQUE FINAL...")
        self.brain_anatomical = get_brain_anatomical_final()
        self.lung_anatomical = LungOpenCVDetector()
        print("[MedicalOrganClassifier] Détecteur ANATOMIQUE FINAL prêt!")
    
    def _build_model(self):
        """
        Construit le modèle ResNet50 avec la tête de classification adaptée
        """
        # Charger ResNet50 pré-entraîné
        model = resnet50(weights=ResNet50_Weights.DEFAULT)
        
        # Geler les couches de base (feature extraction)
        for param in model.parameters():
            param.requires_grad = False
        
        # Modifier la dernière couche pour notre nombre de classes
        num_features = model.fc.in_features
        model.fc = nn.Sequential(
            nn.Dropout(0.5),
            nn.Linear(num_features, 512),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(512, self.num_classes)
        )
        
        return model
    
    def load_weights(self, model_path: str):
        """
        Charge les poids entraînés du modèle
        """
        try:
            checkpoint = torch.load(model_path, map_location=self.device)
            self.model.load_state_dict(checkpoint['model_state_dict'])
            print(f"Loaded model weights from {model_path}")
        except Exception as e:
            print(f"Error loading weights: {e}")
            print("Using pretrained ImageNet weights")
    
    def save_weights(self, model_path: str):
        """
        Sauvegarde les poids du modèle
        """
        torch.save({
            'model_state_dict': self.model.state_dict(),
            'classes': ORGAN_CLASSES
        }, model_path)
        print(f"Saved model weights to {model_path}")
    
    def _detect_medical_image_type(self, image: Image.Image, expected_organ: str = None) -> tuple:
        """
        Détection multi-organe pour IRM médicales.
        Analyse spécifique selon l'organe attendu (cerveau, poumon, foie, etc.)
        
        Returns:
            (is_medical, organ_detected, confidence, details)
        """
        try:
            import numpy as np
            from scipy import ndimage
            
            # Convertir en niveaux de gris
            if image.mode != 'L':
                gray = image.convert('L')
            else:
                gray = image
            
            img_array = np.array(gray, dtype=np.float32)
            h, w = img_array.shape
            
            details = {}
            
            # === VÉRIFICATION 1: GRAYSCALE MÉDICAL ===
            if image.mode == 'RGB':
                rgb_array = np.array(image)
                r, g, b = rgb_array[:,:,0], rgb_array[:,:,1], rgb_array[:,:,2]
                rg_diff = np.abs(r.astype(float) - g.astype(float))
                gb_diff = np.abs(g.astype(float) - b.astype(float))
                mean_diff = (np.mean(rg_diff) + np.mean(gb_diff)) / 2
                is_grayscale = mean_diff < 5.0
                details['color_diff'] = mean_diff
            else:
                is_grayscale = True
                details['color_diff'] = 0
            
            if not is_grayscale:
                return (False, None, 0.0, {'reject': 'not_grayscale'})
            
            # === ANALYSE SELON L'ORGANE ATTENDU ===
            
            # --- DÉTECTION CERVEAU ---
            if expected_organ == 'cerveau':
                return self._detect_brain_mri(img_array, h, w, details)
            
            # --- DÉTECTION POUMON ---
            elif expected_organ == 'poumon':
                return self._detect_lung_mri(img_array, h, w, details)
            
            # --- DÉTECTION AUTRES ORGANES (fallback) ---
            else:
                # Détection générique basée sur la forme
                return self._detect_generic_medical(img_array, h, w, expected_organ, details)
                
        except Exception as e:
            print(f"Medical detection error: {e}")
            import traceback
            traceback.print_exc()
            return (False, None, 0.0, {'error': str(e)})
    
    def _detect_brain_mri(self, img_array, h, w, details):
        """Détection spécifique IRM cerveau"""
        import numpy as np
        from scipy import ndimage
        
        center_y, center_x = h // 2, w // 2
        
        # 1. FORME CRÂNIENNE: Ovale caractéristique
        skull_ratio = h / w if w > 0 else 1.0
        if not (0.9 <= skull_ratio <= 1.5):
            return (False, None, 0.0, {**details, 'reject': f'wrong_ratio_{skull_ratio:.2f}'})
        
        # Masque crânien
        y_grid, x_grid = np.ogrid[:h, :w]
        ellipse_mask = (((x_grid - center_x) / (w * 0.35)) ** 2 + 
                       ((y_grid - center_y) / (h * 0.40)) ** 2) <= 1
        
        ellipse_coverage = np.sum(ellipse_mask) / (h * w)
        if not (0.20 <= ellipse_coverage <= 0.55):
            return (False, None, 0.0, {**details, 'reject': f'coverage_{ellipse_coverage:.2%}'})
        
        inside = img_array[ellipse_mask]
        outside = img_array[~ellipse_mask]
        
        if len(inside) == 0 or len(outside) == 0:
            return (False, None, 0.0, {**details, 'reject': 'empty_regions'})
        
        inside_mean = np.mean(inside)
        outside_mean = np.mean(outside)
        
        # 2. FOND SOMBRE typique IRM
        if not (outside_mean < 45 and outside_mean < inside_mean * 0.5):
            return (False, None, 0.0, {**details, 'reject': f'background_{outside_mean:.1f}'})
        
        # 3. VENTRICULES CENTRAUX (caractéristique cerveau)
        v_h, v_w = max(25, h//6), max(35, w//5)
        v_y1 = max(0, center_y - v_h//2)
        v_y2 = min(h, v_y1 + v_h)
        v_x1 = max(0, center_x - v_w//2)
        v_x2 = min(w, v_x1 + v_w)
        
        ventricle_region = img_array[v_y1:v_y2, v_x1:v_x2]
        v_mean = np.mean(ventricle_region)
        v_min = np.min(ventricle_region)
        
        # Ventricules = zones sombres au centre
        has_ventricles = v_min < v_mean * 0.5 and v_min < inside_mean * 0.4
        
        if not has_ventricles:
            return (False, None, 0.0, {**details, 'reject': f'no_ventricles_{v_min:.1f}'})
        
        # 4. SYMMÉTRIE BILATÉRALE
        margin = min(h, w) // 12
        left = img_array[margin:h-margin, margin:center_x]
        right = img_array[margin:h-margin, center_x:w-margin]
        
        if left.size > 100 and right.size > 100:
            min_w = min(left.shape[1], right.shape[1])
            left_flat = left[:, -min_w:].flatten()
            right_flat = np.fliplr(right[:, :min_w]).flatten()
            correlation = np.corrcoef(left_flat, right_flat)[0,1]
            is_symmetric = correlation > 0.72
        else:
            is_symmetric = False
            correlation = 0
        
        if not is_symmetric:
            return (False, None, 0.0, {**details, 'reject': f'not_symmetric_{correlation:.3f}'})
        
        # 5. TEXTURE GYRI/SULCI
        sobel_h = ndimage.sobel(img_array, axis=0)
        sobel_v = ndimage.sobel(img_array, axis=1)
        edge_mag = np.sqrt(sobel_h**2 + sobel_v**2)
        if np.max(edge_mag) > 0:
            edge_mag = edge_mag / np.max(edge_mag)
        
        edges_ratio = np.sum((edge_mag > 0.12) & ellipse_mask) / np.sum(ellipse_mask)
        has_texture = 0.06 < edges_ratio < 0.32
        
        if not has_texture:
            return (False, None, 0.0, {**details, 'reject': f'bad_texture_{edges_ratio:.3f}'})
        
        # ✅ CERVEAU DÉTECTÉ
        confidence = 0.82
        print(f"[BRAIN MRI] Detected (conf={confidence:.2%}, corr={correlation:.3f}, ventricles={has_ventricles})")
        return (True, 'cerveau', confidence, {**details, 'correlation': correlation, 'ventricles': True})
    
    def _detect_lung_mri(self, img_array, h, w, details):
        """Détection spécifique IRM poumon"""
        import numpy as np
        from scipy import ndimage
        
        # IRM poumon: deux grandes zones sombres (poumons) de chaque côté
        # avec structure médiasginale au centre
        
        center_y, center_x = h // 2, w // 2
        
        # Ratio typique poumon: plus large que haut
        lung_ratio = h / w if w > 0 else 1.0
        if not (0.6 <= lung_ratio <= 1.3):
            return (False, None, 0.0, {**details, 'reject': f'wrong_ratio_{lung_ratio:.2f}'})
        
        # Diviser en gauche/droite
        left_side = img_array[:, :center_x]
        right_side = img_array[:, center_x:]
        
        # Les poumons apparaissent comme des zones relativement sombres et uniformes
        # avec des vaisseaux plus clairs
        left_mean = np.mean(left_side)
        right_mean = np.mean(right_side)
        
        # Vérifier la symétrie gauche-droite des poumons
        side_diff = abs(left_mean - right_mean) / max(left_mean, right_mean)
        sides_symmetric = side_diff < 0.25
        
        # Zone centrale (médiastin) doit être plus claire
        center_width = w // 5
        center_region = img_array[:, center_x - center_width//2 : center_x + center_width//2]
        center_mean = np.mean(center_region)
        
        # Médiastin plus clair que les poumons
        has_mediastinum = center_mean > max(left_mean, right_mean) * 1.1
        
        # Texture pulmonaire (moins dense que cerveau)
        sobel_h = ndimage.sobel(img_array, axis=0)
        sobel_v = ndimage.sobel(img_array, axis=1)
        edge_mag = np.sqrt(sobel_h**2 + sobel_v**2)
        
        lung_texture = np.mean(edge_mag) / np.max(edge_mag) if np.max(edge_mag) > 0 else 0
        has_lung_texture = 0.02 < lung_texture < 0.15  # Moins texturé que cerveau
        
        score = 0
        if sides_symmetric: score += 1
        if has_mediastinum: score += 2
        if has_lung_texture: score += 1
        
        if score >= 3:
            confidence = 0.75 + (score - 3) * 0.08
            print(f"[LUNG MRI] Detected (conf={confidence:.2%}, score={score})")
            return (True, 'poumon', min(confidence, 0.90), {**details, 'mediastinum': has_mediastinum})
        else:
            return (False, None, 0.0, {**details, 'reject': f'lung_score_{score}'})
    
    def _detect_generic_medical(self, img_array, h, w, expected_organ, details):
        """Détection générique pour autres organes"""
        import numpy as np
        
        # Vérifications de base
        mean_intensity = np.mean(img_array)
        std_intensity = np.std(img_array)
        
        # Image médicale typique: grayscale avec distribution spécifique
        is_medical = (30 <= mean_intensity <= 220) and (10 <= std_intensity <= 80)
        
        if is_medical and expected_organ:
            # Retourner l'organe attendu avec confiance modérée
            return (True, expected_organ, 0.60, {**details, 'generic': True})
        
        return (False, None, 0.0, {**details, 'reject': 'not_medical'})
    
    def predict(self, image_bytes: bytes) -> dict:
        """
        Prédit l'organe à partir d'une image
        
        Args:
            image_bytes: Image en bytes
            
        Returns:
            dict avec organe prédit, confiance et scores par classe
        """
        try:
            # === ÉTAPE 1: CNN PREDICTION (PRINCIPAL) ===
            print("[PREDICT] CNN ResNet50 (principal) + anatomical bonus...")
            image = Image.open(io.BytesIO(image_bytes)).convert('RGB')
            image_tensor = self.transforms(image).unsqueeze(0).to(self.device)
            
            with torch.no_grad():
                outputs = self.model(image_tensor)
                probs = torch.nn.functional.softmax(outputs, dim=1)[0].cpu().numpy()
            
            # Scores CNN
            cnn_scores = {ORGAN_CLASSES[i]: float(probs[i]) for i in range(len(ORGAN_CLASSES))}
            print(f"[PREDICT] CNN scores: {cnn_scores}")
            
            # === ÉTAPE 2: DÉTECTION ANATOMIQUE (COMME BONUS/PÉNALITÉ) ===
            lung_bonus = 0
            brain_penalty = 0
            
            if self.lung_anatomical:
                lung_result = self.lung_anatomical.detect(image_bytes)
                if lung_result.get('is_lung'):
                    lung_bonus = lung_result.get('confidence', 0) / 100 * 0.25  # boost max 0.25
                    print(f"[PREDICT] Lung bonus: +{lung_bonus:.3f}")
            
            if self.brain_anatomical:
                brain_result = self.brain_anatomical.detect(image_bytes)
                if brain_result.get('is_brain'):
                    brain_penalty = brain_result.get('confidence', 0) / 100 * 0.25  # penalty max 0.25
                    print(f"[PREDICT] Brain penalty: -{brain_penalty:.3f}")
            
            # === ÉTAPE 3: AJUSTER SCORES ===
            adjusted_scores = cnn_scores.copy()
            
            # Boost poumon si anatomique détecte poumon
            adjusted_scores['poumon'] += lung_bonus
            
            # Pénalité si cerveau détecté (pour la validation poumon)
            adjusted_scores['poumon'] -= brain_penalty
            
            # S'assurer que les scores restent positifs
            for k in adjusted_scores:
                adjusted_scores[k] = max(0.001, adjusted_scores[k])
            
            # Normalisation
            total = sum(adjusted_scores.values())
            adjusted_scores = {k: v / total for k, v in adjusted_scores.items()}
            
            print(f"[PREDICT] Adjusted scores: {adjusted_scores}")
            
            # === ÉTAPE 4: DÉCISION FINALE ===
            predicted_organ = max(adjusted_scores, key=adjusted_scores.get)
            confidence = adjusted_scores[predicted_organ] * 100
            
            # Alternatives
            sorted_scores = dict(sorted(adjusted_scores.items(), key=lambda x: x[1], reverse=True))
            alternatives = [
                {'organ': k, 'confidence': round(v * 100, 2)}
                for k, v in list(sorted_scores.items())[1:4]
            ]
            
            print(f"[PREDICT] Result: {predicted_organ} ({confidence:.1f}%)")
            
            return {
                'organ': predicted_organ,
                'organ_name': ORGAN_NAMES.get(predicted_organ, predicted_organ),
                'confidence': round(confidence, 2),
                'detected': True,
                'all_scores': {k: round(v * 100, 2) for k, v in sorted_scores.items()},
                'alternatives': alternatives,
                'method': 'cnn+anatomical_fusion',
                'cnn_raw_scores': {k: round(v * 100, 2) for k, v in cnn_scores.items()},
                'anatomical_adjustments': {'lung_bonus': lung_bonus, 'brain_penalty': brain_penalty}
            }
                
        except Exception as e:
            print(f"Prediction error: {e}")
            return {
                'organ': 'unknown',
                'organ_name': 'Inconnu',
                'confidence': 0.0,
                'detected': False,
                'error': str(e)
            }
    
    def validate_organ(self, image_bytes: bytes, expected_organ: str) -> dict:
        """
        Valide si l'image correspond à l'organe attendu via détecteurs géométriques multi-organe.
        Rejette strictement les images d'un autre organe (ex: cerveau pour modèle poumon).
        """
        if expected_organ not in ORGAN_CLASSES:
            return {
                'valid': False,
                'expected_organ': expected_organ,
                'expected_organ_name': expected_organ,
                'detected_organ': 'unknown',
                'detected_organ_name': 'Inconnu',
                'confidence': 0.0,
                'reason': f"Organe '{expected_organ}' non supporté",
                'rejection_reason': f"Organe '{expected_organ}' non supporté",
            }

        try:
            validator = get_organ_geometry_validator()
            result = validator.validate(image_bytes, expected_organ)
            print(
                f"[VALIDATE] {expected_organ}: valid={result['valid']}, "
                f"detected={result.get('detected_organ')}, conf={result.get('confidence')}"
            )
            return result
        except Exception as exc:
            print(f"[VALIDATE] Geometry validation error: {exc}, CNN fallback")

        prediction = self.predict(image_bytes)
        if 'error' in prediction:
            return {
                'valid': False,
                'error': prediction['error'],
                'expected_organ': expected_organ,
                'expected_organ_name': ORGAN_NAMES.get(expected_organ, expected_organ),
                'detected_organ': 'unknown',
                'detected_organ_name': 'Inconnu',
                'confidence': 0.0,
                'reason': prediction['error'],
                'rejection_reason': prediction['error'],
            }

        detected_organ = prediction['organ']
        confidence = float(prediction['confidence'])
        is_valid = (detected_organ == expected_organ) and (confidence >= MIN_ACCEPT_CONFIDENCE)
        reason = None
        if not is_valid:
            if detected_organ != expected_organ:
                reason = (
                    f"Organe détecté : {ORGAN_NAMES.get(detected_organ, detected_organ)} "
                    f"({confidence:.0f}%) — attendu : {ORGAN_NAMES.get(expected_organ, expected_organ)}"
                )
            else:
                reason = f"Confiance insuffisante ({confidence:.0f}%, minimum {MIN_ACCEPT_CONFIDENCE:.0f}%)"

        return {
            'valid': is_valid,
            'expected_organ': expected_organ,
            'expected_organ_name': ORGAN_NAMES.get(expected_organ, expected_organ),
            'detected_organ': detected_organ,
            'detected_organ_name': ORGAN_NAMES.get(detected_organ, detected_organ),
            'organ': detected_organ,
            'organ_name': ORGAN_NAMES.get(detected_organ, detected_organ),
            'confidence': confidence,
            'reason': reason,
            'rejection_reason': reason,
            'alternative_organs': prediction.get('alternatives', []),
            'all_scores': prediction.get('all_scores', {}),
            'method': 'cnn_fallback',
        }


# Instance singleton du classificateur
_classifier = None

def get_classifier(model_path: str = None) -> MedicalOrganClassifier:
    """
    Obtient l'instance singleton du classificateur
    """
    global _classifier
    if _classifier is None:
        _classifier = MedicalOrganClassifier(model_path)
    return _classifier
