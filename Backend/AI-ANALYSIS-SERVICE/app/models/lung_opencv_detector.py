"""
Détecteur de poumon basé sur OpenCV (rule-based, no ML)
Détection par thresholding, contours et analyse morphologique
"""

import io
import numpy as np
from PIL import Image
import cv2

class LungOpenCVDetector:
    """
    Détecteur de poumon utilisant OpenCV et traitement d'image
    Basé sur: thresholding + détection de contours + analyse de forme
    """
    
    def __init__(self):
        self.min_contour_area = 500  # Aire minimale d'un poumon
        self.max_contour_area = 50000  # Aire maximale
        self.symmetry_threshold = 0.7  # Seuil de symétrie
        
    def detect(self, image_bytes: bytes) -> dict:
        """
        Détection par analyse de contours et régions sombres
        """
        try:
            # === 1. CHARGEMENT ET PRÉTRAITEMENT ===
            img = Image.open(io.BytesIO(image_bytes)).convert('L')
            img_array = np.array(img, dtype=np.uint8)
            h, w = img_array.shape
            
            print(f"\n[LUNG OPENCV] Analyse {w}x{h}")
            
            # Redimensionner si trop grand (optimisation)
            if max(h, w) > 800:
                scale = 800 / max(h, w)
                new_w, new_h = int(w * scale), int(h * scale)
                img_array = cv2.resize(img_array, (new_w, new_h))
                h, w = img_array.shape
                print(f"  Redimensionné à {w}x{h}")
            
            # === 2. NORMALISATION ET THRESHOLDING ===
            # Normaliser l'intensité
            img_normalized = cv2.normalize(img_array, None, 0, 255, cv2.NORM_MINMAX)
            
            # Appliquer un flou pour réduire le bruit
            img_blurred = cv2.GaussianBlur(img_normalized, (5, 5), 0)
            
            # Thresholding pour détecter les zones sombres (poumons = air = sombre)
            # Méthode 1: Otsu automatique
            _, thresh_otsu = cv2.threshold(img_blurred, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
            
            # Méthode 2: Seuil fixe pour zones très sombres (air dans poumons)
            # Les poumons ont des valeurs < 80-100 en niveaux de gris normalisés
            _, thresh_fixed = cv2.threshold(img_blurred, 80, 255, cv2.THRESH_BINARY_INV)
            
            # Combiner les deux: prendre l'intersection (zones sombres dans les deux)
            thresh = cv2.bitwise_or(thresh_otsu, thresh_fixed)
            
            print(f"  Seuil Otsu + fixe (80) appliqué")
            
            # === 3. OPÉRATIONS MORPHOLOGIQUES ===
            # Nettoyer le masque (enlever le bruit petit)
            kernel_small = np.ones((3, 3), np.uint8)
            kernel_large = np.ones((7, 7), np.uint8)
            
            # Ouverture: érosion puis dilatation (enlève le bruit)
            morph = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel_small)
            
            # Fermeture: dilatation puis érosion (remplit les trous)
            morph = cv2.morphologyEx(morph, cv2.MORPH_CLOSE, kernel_large)
            
            # === 4. DÉTECTION DE CONTOURS ===
            contours, _ = cv2.findContours(morph, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            print(f"  Contours détectés: {len(contours)}")
            
            # Filtrer les contours par taille (garder les grands)
            valid_contours = []
            for cnt in contours:
                area = cv2.contourArea(cnt)
                if self.min_contour_area < area < self.max_contour_area:
                    valid_contours.append(cnt)
            
            print(f"  Contours valides (taille): {len(valid_contours)}")
            
            if len(valid_contours) < 2:
                print(f"  [REJET] Pas assez de régions pulmonaires ({len(valid_contours)} < 2)")
                return {
                    'is_lung': False,
                    'confidence': 30.0,
                    'method': 'opencv',
                    'rejection_reason': f'Pas assez de régions sombres ({len(valid_contours)} trouvées, besoin 2+)',
                    'details': {
                        'contours_found': len(contours),
                        'valid_contours': len(valid_contours),
                        'reason': 'insufficient_regions'
                    }
                }
            
            # === 5. ANALYSE DES CONTOURS ===
            # D'abord calculer les centres de tous les contours valides
            candidate_regions = []
            for i, cnt in enumerate(valid_contours):
                M = cv2.moments(cnt)
                if M["m00"] != 0:
                    cx = int(M["m10"] / M["m00"])
                    cy = int(M["m01"] / M["m00"])
                else:
                    continue
                
                area = cv2.contourArea(cnt)
                x, y, bw, bh = cv2.boundingRect(cnt)
                aspect_ratio = float(bw) / bh if bh > 0 else 0
                
                # Filtrer par position thoracique (0.15 - 0.85)
                y_ratio = cy / h
                if 0.15 < y_ratio < 0.85:
                    candidate_regions.append({
                        'center': (cx, cy),
                        'area': area,
                        'bbox': (x, y, bw, bh),
                        'aspect_ratio': aspect_ratio,
                        'y_ratio': y_ratio,
                        'contour': cnt
                    })
            
            if len(candidate_regions) < 2:
                print(f"  [REJET] Pas assez de régions dans la zone thoracique ({len(candidate_regions)} < 2)")
                return {
                    'is_lung': False,
                    'confidence': 32.0,
                    'method': 'opencv',
                    'rejection_reason': f'Pas assez de régions thoraciques ({len(candidate_regions)} trouvées)',
                    'details': {
                        'candidates': len(candidate_regions),
                        'reason': 'insufficient_thoracic_regions'
                    }
                }
            
            # Trier par aire et prendre les 2 plus grands dans la zone thoracique
            candidate_regions = sorted(candidate_regions, key=lambda x: x['area'], reverse=True)
            lung_regions = candidate_regions[:2]
            
            # Afficher les régions sélectionnées
            for i, region in enumerate(lung_regions):
                print(f"    Région {i+1}: centre={region['center']}, aire={region['area']:.0f}, ratio={region['aspect_ratio']:.2f}, y={region['y_ratio']:.2f}")
            
            # === 6. ASSIGNATION GAUCHE/DROITE ===
            # Déterminer quelle région est à gauche et à droite
            if lung_regions[0]['center'][0] < lung_regions[1]['center'][0]:
                left_region = lung_regions[0]
                right_region = lung_regions[1]
            else:
                left_region = lung_regions[1]
                right_region = lung_regions[0]
            
            # Afficher positions
            print(f"  Position Y: gauche={left_region['y_ratio']:.2f}, droite={right_region['y_ratio']:.2f}")
            
            # === 7. VÉRIFICATION SYMÉTRIE (gauche/droite) ===
            # Calculer la distance horizontale par rapport au centre
            center_x = w // 2
            left_dist = abs(left_region['center'][0] - center_x)
            right_dist = abs(right_region['center'][0] - center_x)
            
            # Distance verticale entre les centres (doit être proche)
            y_diff = abs(left_region['center'][1] - right_region['center'][1])
            y_diff_ratio = y_diff / h
            
            # Ratio des aires (doivent être similaires)
            area_ratio = min(left_region['area'], right_region['area']) / max(left_region['area'], right_region['area'])
            
            print(f"  Symétrie: dist_g={left_dist}, dist_d={right_dist}, dy={y_diff_ratio:.2f}, aire_ratio={area_ratio:.2f}")
            
            # Vérifier symétrie horizontale (deux côtés du centre)
            left_is_left = left_region['center'][0] < center_x
            right_is_right = right_region['center'][0] > center_x
            
            if not (left_is_left and right_is_right):
                print(f"  [REJET] Régions pas symétriques (pas de chaque côté du centre)")
                return {
                    'is_lung': False,
                    'confidence': 40.0,
                    'method': 'opencv',
                    'rejection_reason': 'Régions non symétriques',
                    'details': {
                        'left_x': left_region['center'][0],
                        'right_x': right_region['center'][0],
                        'center_x': center_x,
                        'reason': 'not_symmetric'
                    }
                }
            
            # === 8. VÉRIFICATION FORME ===
            # Les poumons ont un ratio hauteur/largeur typique
            left_ar = left_region['aspect_ratio']
            right_ar = right_region['aspect_ratio']
            
            print(f"  Forme: ratio_g={left_ar:.2f}, ratio_d={right_ar:.2f}")
            
            # Vérifier que les ratios sont raisonnables (0.5 - 2.0)
            if not (0.4 < left_ar < 2.5 and 0.4 < right_ar < 2.5):
                print(f"  [REJET] Forme anormale des régions")
                return {
                    'is_lung': False,
                    'confidence': 35.0,
                    'method': 'opencv',
                    'rejection_reason': 'Forme des régions non pulmonaire',
                    'details': {
                        'left_ratio': left_ar,
                        'right_ratio': right_ar,
                        'reason': 'bad_shape'
                    }
                }
            
            # === 9. CALCUL CONFIANCE ===
            confidence = 0.7  # Base
            
            # Bonus pour bonne symétrie
            if y_diff_ratio < 0.1:
                confidence += 0.15
            if area_ratio > 0.7:
                confidence += 0.10
            if 0.5 < left_ar < 1.5 and 0.5 < right_ar < 1.5:
                confidence += 0.05
            
            confidence = min(confidence, 0.95)
            
            print(f"  [ACCEPTÉ] Poumon détecté (confiance={confidence:.1%})")
            
            return {
                'is_lung': True,
                'confidence': confidence * 100,
                'method': 'opencv',
                'details': {
                    'contours_found': len(contours),
                    'regions': [
                        {
                            'center': left_region['center'],
                            'area': int(left_region['area']),
                            'aspect_ratio': round(left_ar, 2)
                        },
                        {
                            'center': right_region['center'],
                            'area': int(right_region['area']),
                            'aspect_ratio': round(right_ar, 2)
                        }
                    ],
                    'symmetry': {
                        'y_diff_ratio': round(y_diff_ratio, 2),
                        'area_ratio': round(area_ratio, 2)
                    }
                }
            }
            
        except Exception as e:
            print(f"  [ERREUR] {str(e)}")
            return {
                'is_lung': False,
                'confidence': 0.0,
                'method': 'opencv',
                'rejection_reason': f'Erreur: {str(e)}',
                'details': {'error': str(e)}
            }
    
    def visualize(self, image_bytes: bytes, output_path: str = None):
        """
        Visualise les contours détectés (pour debug)
        """
        try:
            img = Image.open(io.BytesIO(image_bytes)).convert('L')
            img_array = np.array(img, dtype=np.uint8)
            
            # Convertir en BGR pour dessiner en couleur
            img_color = cv2.cvtColor(img_array, cv2.COLOR_GRAY2BGR)
            
            h, w = img_array.shape
            if max(h, w) > 800:
                scale = 800 / max(h, w)
                new_w, new_h = int(w * scale), int(h * scale)
                img_array = cv2.resize(img_array, (new_w, new_h))
                img_color = cv2.resize(img_color, (new_w, new_h))
                h, w = img_array.shape
            
            # Même traitement que detect()
            img_normalized = cv2.normalize(img_array, None, 0, 255, cv2.NORM_MINMAX)
            img_blurred = cv2.GaussianBlur(img_normalized, (5, 5), 0)
            _, thresh = cv2.threshold(img_blurred, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
            
            kernel_small = np.ones((3, 3), np.uint8)
            kernel_large = np.ones((7, 7), np.uint8)
            morph = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel_small)
            morph = cv2.morphologyEx(morph, cv2.MORPH_CLOSE, kernel_large)
            
            contours, _ = cv2.findContours(morph, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            # Dessiner les contours
            cv2.drawContours(img_color, contours, -1, (0, 255, 0), 2)
            
            # Dessiner les centres
            for cnt in contours:
                area = cv2.contourArea(cnt)
                if 500 < area < 50000:
                    M = cv2.moments(cnt)
                    if M["m00"] != 0:
                        cx = int(M["m10"] / M["m00"])
                        cy = int(M["m01"] / M["m00"])
                        cv2.circle(img_color, (cx, cy), 5, (0, 0, 255), -1)
                        cv2.putText(img_color, f"{int(area)}", (cx+10, cy), 
                                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 1)
            
            if output_path:
                cv2.imwrite(output_path, img_color)
                print(f"  Image sauvegardée: {output_path}")
            
            return img_color
            
        except Exception as e:
            print(f"  [ERREUR VISUALISATION] {str(e)}")
            return None
