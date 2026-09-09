"""
Détecteur de cerveau SIMPLIFIÉ
Vérification basique basée sur structure globale
"""

import io
import numpy as np
from PIL import Image
from scipy import ndimage

class BrainAnatomicalFinal:
    """
    Détecteur minimaliste pour IRM cerveau
    Basé sur: forme ovale + structure centrale unique
    """
    
    def detect(self, image_bytes: bytes) -> dict:
        """
        Détection simplifiée
        Score max: 4, Seuil: 3
        """
        try:
            img = Image.open(io.BytesIO(image_bytes)).convert('L')
            img_array = np.array(img, dtype=np.uint8)
            h, w = img_array.shape
            
            print(f"\n[BRAIN SIMPLE] Analyse {w}x{h}")
            
            # === 1. FILTRE IMAGE MÉDICALE ===
            mean_val = np.mean(img_array)
            std_val = np.std(img_array)
            
            print(f"  mean={mean_val:.1f}, std={std_val:.1f}")
            
            # Vérifier si image médicale
            if not (20 < mean_val < 230 and 5 < std_val < 80):
                print(f"  [REJET] Pas une image médicale")
                return {
                    'is_brain': False,
                    'confidence': 15.0,
                    'method': 'brain_simple',
                    'rejection_reason': f'Image non médicale (mean={mean_val:.1f}, std={std_val:.1f})',
                    'details': {'mean': mean_val, 'std': std_val}
                }
            
            # === 2. DÉTECTION STRUCTURE CERVEAU ===
            center_x, center_y = w // 2, h // 2
            
            # Région centrale
            center_region = img_array[
                max(0, center_y - h//4):min(h, center_y + h//4),
                max(0, center_x - w//4):min(w, center_x + w//4)
            ]
            
            # Zone périphérique (bord)
            border_mask = np.zeros_like(img_array, dtype=np.uint8)
            border_size = min(h, w) // 15
            border_mask[:border_size, :] = 1
            border_mask[-border_size:, :] = 1
            border_mask[:, :border_size] = 1
            border_mask[:, -border_size:] = 1
            
            border_mean = np.mean(img_array[border_mask == 1])
            center_mean = np.mean(center_region)
            
            print(f"  border={border_mean:.1f}, center={center_mean:.1f}")
            
            # ANTI-POUMON: cavités latérales sombres + médiastin central clair (sans filtrer les zones sombres)
            cy, cx = h // 2, w // 2
            y_grid, x_grid = np.ogrid[:h, :w]
            thorax_mask = (
                ((x_grid - cx) / max(1, w * 0.38)) ** 2
                + ((y_grid - cy) / max(1, h * 0.42)) ** 2
            ) <= 1
            left_zone = thorax_mask & (x_grid < cx - w // 14)
            right_zone = thorax_mask & (x_grid > cx + w // 14)
            center_zone = thorax_mask & (np.abs(x_grid - cx) <= w // 12)
            if left_zone.sum() > 40 and right_zone.sum() > 40 and center_zone.sum() > 15:
                li = float(np.mean(img_array[left_zone]))
                ri = float(np.mean(img_array[right_zone]))
                ci = float(np.mean(img_array[center_zone]))
                left_dark_frac = float(np.mean(img_array[left_zone] < 85))
                right_dark_frac = float(np.mean(img_array[right_zone] < 85))
                # Poumon: cavités latérales sombres + médiastin central plus clair (pas ventricules)
                looks_like_lung = (
                    li < 95 and ri < 95
                    and ci > max(li, ri) * 1.12
                    and abs(li - ri) / max(li, ri, 1.0) < 0.25
                    and left_dark_frac > 0.25
                    and right_dark_frac > 0.25
                )
            else:
                looks_like_lung = False
                li = ri = ci = 0.0

            print(f"  inside L/R/C={li:.1f}/{ri:.1f}/{ci:.1f}, lung_like={looks_like_lung}")

            # Ratio large/haut (cerveau = ovale)
            aspect = w / h
            
            # === 3. SYMÉTRIE ===
            left_half = img_array[:, :center_x]
            right_half = np.fliplr(img_array[:, center_x:])
            min_w = min(left_half.shape[1], right_half.shape[1])
            
            if min_w > 10:
                corr = np.corrcoef(
                    left_half[:, :min_w].flatten(),
                    right_half[:, :min_w].flatten()
                )[0, 1]
                corr = max(0, corr)
            else:
                corr = 0.0
            
            print(f"  symmetry={corr:.2f}, aspect={aspect:.2f}")
            
            # === 4. SYSTÈME DE SCORE ===
            score = 0
            checks = {}
            
            # ANTI-POUMON: Si ressemble à un poumon → score = 0
            if looks_like_lung:
                print(f"  [ANTI-LUNG] Structure pulmonaire détectée")
                return {
                    'is_brain': False,
                    'confidence': 20.0,
                    'method': 'brain_simple',
                    'rejection_reason': 'Structure pulmonaire détectée (pas un cerveau)',
                    'details': {
                        'inside_left': round(li, 1),
                        'inside_right': round(ri, 1),
                        'inside_center': round(ci, 1),
                        'reason': 'lung_like_structure'
                    }
                }
            
            # Bord sombre (fond IRM, noir ou gris foncé) → +1
            if border_mean < 65:
                score += 1
                checks['dark_border'] = True
            else:
                checks['dark_border'] = False
            
            # Centre plus clair que bord (tissu cérébral vs fond) → +1
            if center_mean > border_mean * 1.10:
                score += 1
                checks['center_brighter'] = True
            else:
                checks['center_brighter'] = False
            
            # Forme ovale/circulaire typique IRM axiale → +1
            if 0.75 <= aspect <= 1.35:
                score += 1
                checks['oval_shape'] = True
            else:
                checks['oval_shape'] = False
            
            # Symétrie bilatérale (hémisphères) → +1
            if corr > 0.40:
                score += 1
                checks['symmetry'] = True
            else:
                checks['symmetry'] = False
            
            # Peu de zones très sombres type cavité pulmonaire → +1
            air_ratio = np.sum(img_array < np.percentile(img_array, 35)) / (h * w)
            if air_ratio < 0.35:
                score += 1
                checks['low_air'] = True
            else:
                checks['low_air'] = False
            
            core_checks = sum([
                checks['dark_border'],
                checks['center_brighter'],
                checks['symmetry'],
            ])
            
            print(f"  Score: {score}/5 (core={core_checks}/3)")
            print(f"  Checks: {checks}")
            
            # === 5. DÉCISION ===
            details = {
                'border_mean': round(border_mean, 1),
                'center_mean': round(center_mean, 1),
                'aspect': round(aspect, 2),
                'symmetry': round(corr, 3),
                'score': score,
                'core_checks': core_checks,
                'checks': checks,
            }
            
            if score >= 4 and core_checks >= 2:
                confidence = 62.0 + score * 7.0
                return {
                    'is_brain': True,
                    'confidence': min(confidence, 96.0),
                    'method': 'brain_simple',
                    'details': details,
                }
            if score >= 3 and core_checks >= 2:
                confidence = 55.0 + score * 4.0
                return {
                    'is_brain': True,
                    'confidence': min(confidence, 72.0),
                    'method': 'brain_simple',
                    'details': details,
                }
            return {
                'is_brain': False,
                'confidence': max(score * 12.0, 10.0),
                'method': 'brain_simple',
                'rejection_reason': f'Structure IRM cérébrale non confirmée ({score}/5)',
                'details': details,
            }
        
        except Exception as e:
            print(f"[BRAIN SIMPLE] ERREUR: {str(e)}")
            return {
                'is_brain': False,
                'confidence': 0,
                'method': 'error',
                'rejection_reason': str(e)
            }
    
    def validate(self, image_bytes: bytes) -> dict:
        """Alias pour detect"""
        result = self.detect(image_bytes)
        return {
            'valid': result.get('is_brain', False),
            'confidence': result.get('confidence', 0),
            'reason': result.get('rejection_reason', 'ok'),
            'details': result.get('details', {})
        }


# Instance singleton
_brain_anatomical_final = None

def get_brain_anatomical_final():
    """
    Retourne une instance unique du détecteur de cerveau.
    Pattern singleton pour éviter de recharger le modèle.
    """
    global _brain_anatomical_final
    if _brain_anatomical_final is None:
        _brain_anatomical_final = BrainAnatomicalFinal()
    return _brain_anatomical_final
