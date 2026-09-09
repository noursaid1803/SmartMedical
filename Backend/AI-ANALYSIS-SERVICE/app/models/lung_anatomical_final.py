"""
Détecteur de poumon SIMPLIFIÉ
Vérification basique basée sur structure globale
"""

import io
import numpy as np
from PIL import Image
from scipy import ndimage

class LungAnatomicalFinal:
    """
    Détecteur minimaliste pour IRM poumon
    Basé sur: bilateral dark zones + symétrie simple
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
            
            print(f"\n[LUNG SIMPLE] Analyse {w}x{h}")
            
            # === 1. FILTRE IMAGE MÉDICALE ===
            mean_val = np.mean(img_array)
            std_val = np.std(img_array)
            aspect_ratio = w / h
            
            print(f"  mean={mean_val:.1f}, std={std_val:.1f}, aspect={aspect_ratio:.2f}")
            
            # Vérifier si image médicale
            if not (20 < mean_val < 230 and 5 < std_val < 80):
                print(f"  [REJET] Pas une image médicale")
                return {
                    'is_lung': False,
                    'confidence': 15.0,
                    'method': 'lung_simple',
                    'rejection_reason': f'Image non médicale (mean={mean_val:.1f}, std={std_val:.1f})',
                    'details': {'mean': mean_val, 'std': std_val}
                }
            
            # Vérifier ratio d'aspect (poumon = typiquement 0.65 - 1.55)
            if not (0.65 < aspect_ratio < 1.55):
                print(f"  [REJET] Ratio d'aspect anormal pour poumon: {aspect_ratio:.2f}")
                return {
                    'is_lung': False,
                    'confidence': 20.0,
                    'method': 'lung_simple',
                    'rejection_reason': f'Forme anormale (aspect={aspect_ratio:.2f}, attendu 0.65-1.55)',
                    'details': {'aspect_ratio': aspect_ratio}
                }
            
            # === 2. DÉTECTION STRUCTURE POUMON ===
            center_x = w // 2
            
            # Zones gauche / droite (zone centrale verticale)
            top = int(h * 0.20)  # Étendre un peu plus
            bottom = int(h * 0.80)
            
            left_zone = img_array[top:bottom, :center_x]
            right_zone = img_array[top:bottom, center_x:]
            
            # Ratio de pixels sombres (percentile 40)
            left_dark = np.sum(left_zone < np.percentile(left_zone, 40)) / left_zone.size
            right_dark = np.sum(right_zone < np.percentile(right_zone, 40)) / right_zone.size
            
            print(f"  left_dark={left_dark:.2f}, right_dark={right_dark:.2f}")
            
            # === 2b. VÉRIFICATION ANTI-CERVEAU & ANTI-SEINS ===
            # Un cerveau a: bord sombre + centre gris clair + structure unique
            border_size = min(h, w) // 15
            border_mask = np.zeros_like(img_array, dtype=np.uint8)
            border_mask[:border_size, :] = 1
            border_mask[-border_size:, :] = 1
            border_mask[:, :border_size] = 1
            border_mask[:, -border_size:] = 1
            
            border_mean = np.mean(img_array[border_mask == 1])
            
            # Vérifier la zone centrale
            center_zone = img_array[h//3:2*h//3, w//3:2*w//3]
            center_mean = np.mean(center_zone)
            center_std = np.std(center_zone)
            
            # === DÉTECTION CERVEAU ===
            # Cerveau: bord très sombre (< 40) + centre CLAIR (> 50) + texture modérée
            has_dark_border = border_mean < 40
            has_bright_center = center_mean > 50  # Cerveau = centre clair
            has_brain_texture = 10 < center_std < 100  # Plage plus large
            
            # Calculer le ratio de pixels sombres dans le centre
            center_dark_ratio = np.sum(center_zone < 60) / center_zone.size
            
            # Poumon: zones sombres bilatérales avec air (>15%)
            # Cerveau: centre uniforme avec PEU d'air (<10%)
            has_lung_air_pattern = center_dark_ratio > 0.15  # >15% d'ombre au centre (plus permissif)
            
            # Cerveau si: bord très sombre + centre clair + PEU d'air au centre
            looks_like_brain = has_dark_border and has_bright_center and has_brain_texture and center_dark_ratio < 0.02
            
            # Vérifier s'il y a des "yeux" (deux cercles très clairs en haut)
            top_zone = img_array[:h//4, :]  # Zone plus petite (haut)
            top_mean = np.mean(top_zone)
            # Yeux = zone supérieure BEAUCOUP plus claire que le centre
            has_eyes = top_mean > center_mean * 1.4 and top_mean > 120
            
            print(f"  border_mean={border_mean:.1f}, center_mean={center_mean:.1f}, center_std={center_std:.1f}")
            print(f"  center_dark={center_dark_ratio:.2f}, has_lung_air={has_lung_air_pattern}")
            print(f"  has_eyes={has_eyes}, brain_like={looks_like_brain}")
            
            # === VÉRIFICATION ANTI-SEINS ===
            # Les seins ont deux masses compactes (pas d'air), pas de structure bronchique
            # Les poumons ont BEAUCOUP d'air (trous) dans les zones sombres
            
            # Calculer la variance dans les zones sombres gauche/droite
            left_variance = np.std(left_zone)
            right_variance = np.std(right_zone)
            
            # Les poumons ont une variance élevée (mélange air/vaisseaux), les seins sont plus homogènes
            # Les seins ont une variance plus faible car c'est du tissu glandulaire compact
            low_variance_threshold = 15  # Seuil TRÈS STRICT pour détecter les seins
            
            # Vérifier le ratio hauteur/largeur des zones sombres
            # Seins: forme ronde/ovale, Poumons: forme plus diffuse et irrégulière
            
            # Vérifier s'il y a un "sillon médian" clair entre les deux zones (caractéristique seins)
            mid_line = img_array[:, center_x-5:center_x+5]
            mid_mean = np.mean(mid_line)
            has_cleavage = mid_mean < (left_dark + right_dark) / 2 * 0.8  # Sillon sombre = seins
            
            # Seins si: variance faible + sillon médian + deux masses compactes
            looks_like_breast = (left_variance < low_variance_threshold and 
                                 right_variance < low_variance_threshold and
                                 has_cleavage)
            
            print(f"  border_mean={border_mean:.1f}, center_mean={center_mean:.1f}, center_std={center_std:.1f}")
            print(f"  has_eyes={has_eyes}, brain_like={looks_like_brain}")
            print(f"  left_var={left_variance:.1f}, right_var={right_variance:.1f}, cleavage={has_cleavage}")
            print(f"  breast_like={looks_like_breast}")
            
            # ANTI-SEINS: Si ressemble à des seins → REJET IMMÉDIAT
            if looks_like_breast:
                print(f"  [ANTI-BREAST] SEINS détectés - REJET IMMÉDIAT")
                return {
                    'is_lung': False,
                    'confidence': 10.0,
                    'method': 'lung_simple',
                    'rejection_reason': 'Seins détectés - structure mammaire, pas pulmonaire',
                    'details': {
                        'left_variance': round(left_variance, 1),
                        'right_variance': round(right_variance, 1),
                        'has_cleavage': has_cleavage,
                        'reason': 'breast_detected'
                    }
                }
            
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
            
            print(f"  symmetry={corr:.2f}")
            
            # === 4. SYSTÈME DE SCORE ===
            score = 0
            checks = {}
            score_penalty = 0
            
            # VÉRIFICATION CRITIQUE: Centre CLAIR (médiastin) pour poumon
            # Les poumons ont un centre CLAIR (coeur/mediastin) entre les deux poumons sombres
            # Cerveau = centre plus sombre ou uniforme, pas de médiastin clair
            has_bright_center_mediastinum = center_mean > 90  # Centre clair typique poumon (TRÈS STRICT)
            
            if not has_bright_center_mediastinum:
                print(f"  [REJET] Centre pas assez clair ({center_mean:.1f} <= 90) - Pas un poumon")
                return {
                    'is_lung': False,
                    'confidence': 20.0,
                    'method': 'lung_simple',
                    'rejection_reason': f'Centre pas assez clair ({center_mean:.1f}) - caractéristique médiastin absente',
                    'details': {
                        'center_mean': round(center_mean, 1),
                        'threshold': 90,
                        'reason': 'no_mediastinum'
                    }
                }
            
            # VÉRIFICATION CONTRASTE: Le poumon a un fort contraste entre centre clair et zones sombres
            # Calculer le ratio centre/zones sombres (poumon = fort contraste)
            side_mean = (np.mean(left_zone) + np.mean(right_zone)) / 2
            contrast_ratio = center_mean / max(side_mean, 1)  # Éviter division par 0
            
            print(f"  Contraste: centre={center_mean:.1f}, côtés={side_mean:.1f}, ratio={contrast_ratio:.2f}")
            
            # Poumon: centre clair, côtés sombres → ratio > 1.1
            # Seins/cerveau: plus uniforme → ratio < 1.1
            if contrast_ratio < 1.1:
                print(f"  [REJET] Contraste insuffisant ({contrast_ratio:.2f} < 1.1) - Pas un poumon")
                return {
                    'is_lung': False,
                    'confidence': 25.0,
                    'method': 'lung_simple',
                    'rejection_reason': f'Contraste insuffisant ({contrast_ratio:.2f}) - structure trop uniforme',
                    'details': {
                        'contrast_ratio': round(contrast_ratio, 2),
                        'threshold': 1.1,
                        'center_mean': round(center_mean, 1),
                        'side_mean': round(side_mean, 1),
                        'reason': 'low_contrast'
                    }
                }
            
            # ANTI-CERVEAU: Si ressemble à un cerveau → REJET IMMÉDIAT
            if looks_like_brain:
                print(f"  [ANTI-BRAIN] CERVEAU détecté - REJET")
                return {
                    'is_lung': False,
                    'confidence': 15.0,
                    'method': 'lung_simple',
                    'rejection_reason': 'Cerveau détecté - structure cérébrale, pas pulmonaire',
                    'details': {
                        'border_mean': round(border_mean, 1),
                        'center_mean': round(center_mean, 1),
                        'center_air': round(center_dark_ratio, 2),
                        'reason': 'brain_detected'
                    }
                }
            
            # Si a des yeux (orbites), pénalité supplémentaire
            if has_eyes:
                print(f"  [ANTI-EYES] Orbitales/yeux détectés")
                score_penalty += 3  # Forte pénalité pour yeux
            
            # ANTI-SEINS: Si ressemble à des seins
            if looks_like_breast:
                print(f"  [ANTI-BREAST] Structure mammaire détectée")
                score_penalty += 3  # Forte pénalité pour seins
            
            # left_dark >= 0.38 → +1 (PLUS STRICT)
            if left_dark >= 0.38:
                score += 1
                checks['left_dark'] = True
            else:
                checks['left_dark'] = False
            
            # right_dark >= 0.38 → +1 (PLUS STRICT)
            if right_dark >= 0.38:
                score += 1
                checks['right_dark'] = True
            else:
                checks['right_dark'] = False
            
            # équilibre gauche/droite → +1
            if abs(left_dark - right_dark) < 0.15:
                score += 1
                checks['balance'] = True
            else:
                checks['balance'] = False
            
            # symétrie > 0.5 → +1
            if corr > 0.5:
                score += 1
                checks['symmetry'] = True
            else:
                checks['symmetry'] = False
            
            # VÉRIFICATION: Zones sombres au centre (caractéristique poumon)
            # Les poumons ont des zones d'ombre bilatérales au centre
            if not has_lung_air_pattern:
                print(f"  [WARNING] Peu d'ombre au centre ({center_dark_ratio:.2f} < 0.15)")
                # Pénalité modérée
                score_penalty += 1
            
            # BONUS: Beaucoup d'air dans toute l'image
            air_ratio = np.sum(img_array < np.percentile(img_array, 35)) / (h * w)
            if air_ratio > 0.30:  # Poumons ont beaucoup d'air
                score += 1
                checks['lots_of_air'] = True
            else:
                checks['lots_of_air'] = False
            
            # Appliquer la pénalité
            final_score = max(0, score - score_penalty)
            
            print(f"  Score brut: {score}/5, Pénalité: -{score_penalty}, Final: {final_score}")
            print(f"  Checks: {checks}")
            
            # Utiliser final_score pour la décision
            score = final_score
            
            # === 5. DÉCISION ===
            # Seuil STRICT: 4/5 minimum (accepter SEULEMENT les vrais poumons)
            if score >= 4:
                confidence = 80 + score * 4  # 80-100%
                return {
                    'is_lung': True,
                    'confidence': confidence,
                    'method': 'lung_simple',
                    'details': {
                        'left_dark': round(left_dark, 3),
                        'right_dark': round(right_dark, 3),
                        'symmetry': round(corr, 3),
                        'score': score,
                        'checks': checks
                    }
                }
            else:
                # Tout score < 4 = rejet
                return {
                    'is_lung': False,
                    'confidence': score * 20,
                    'method': 'lung_simple',
                    'rejection_reason': f'Critères poumon non satisfaits ({score}/5, besoin 4/5)',
                    'details': {
                        'left_dark': round(left_dark, 3),
                        'right_dark': round(right_dark, 3),
                        'symmetry': round(corr, 3),
                        'score': score,
                        'checks': checks
                    }
                }
        
        except Exception as e:
            print(f"[LUNG SIMPLE] ERREUR: {str(e)}")
            return {
                'is_lung': False,
                'confidence': 0,
                'method': 'error',
                'rejection_reason': str(e)
            }
    
    def validate(self, image_bytes: bytes) -> dict:
        """Alias pour detect"""
        return self.detect(image_bytes)


# Instance singleton
_lung_anatomical_final = None

def get_lung_anatomical_final():
    """
    Retourne une instance unique du détecteur de poumons.
    Pattern singleton pour éviter de recharger le modèle.
    """
    global _lung_anatomical_final
    if _lung_anatomical_final is None:
        _lung_anatomical_final = LungAnatomicalFinal()
    return _lung_anatomical_final
