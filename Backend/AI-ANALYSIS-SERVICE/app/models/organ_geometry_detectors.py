"""
Validateurs géométriques par organe pour images médicales (IRM/CT/scanner).
Chaque détecteur retourne un score de confiance 0-100 et une raison de rejet éventuelle.
"""

import io
from typing import Optional, Tuple
import numpy as np
from PIL import Image
from scipy import ndimage

from app.models.brain_anatomical_final import get_brain_anatomical_final
from app.models.lung_opencv_detector import LungOpenCVDetector

ORGAN_NAMES = {
    'cerveau': 'Cerveau',
    'sein': 'Sein',
    'peau': 'Peau',
    'oeil': 'Œil',
    'poumon': 'Poumon',
    'foie': 'Foie',
    'coeur': 'Cœur',
}

MIN_ACCEPT_CONFIDENCE = 55.0
MIN_ACCEPT_CONFIDENCE_BY_ORGAN = {
    'cerveau': 50.0,
}

# Seuil plus élevé pour rejeter via organe concurrent (évite faux positifs cerveau↔poumon)
CROSS_ORGAN_MIN_CONFIDENCE = {
    ('cerveau', 'poumon'): 68.0,
    ('poumon', 'cerveau'): 60.0,
}

# Organes à vérifier en exclusion croisée (confusions fréquentes)
CROSS_ORGAN_CHECKS = {
    'poumon': ['cerveau'],
    'cerveau': ['poumon'],
    'coeur': ['poumon', 'cerveau'],
    'foie': ['poumon'],
    'sein': ['poumon'],
}


def _load_gray(image_bytes: bytes) -> Tuple[np.ndarray, Image.Image]:
    img = Image.open(io.BytesIO(image_bytes))
    gray = img.convert('L')
    return np.array(gray, dtype=np.float32), img


def _is_grayscale_medical(img: Image.Image) -> bool:
    if img.mode != 'RGB':
        return True
    rgb = np.array(img.convert('RGB'), dtype=np.float32)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    diff = (np.mean(np.abs(r - g)) + np.mean(np.abs(g - b))) / 2
    return diff < 8.0


def _colorfulness(img: Image.Image) -> float:
    if img.mode != 'RGB':
        rgb = np.array(img.convert('RGB'), dtype=np.float32)
    else:
        rgb = np.array(img, dtype=np.float32)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    rg = np.abs(r - g)
    yb = np.abs(0.5 * (r + g) - b)
    return float(np.mean(rg) + np.mean(yb))


def _bilateral_symmetry(img_array: np.ndarray) -> float:
    h, w = img_array.shape
    cx = w // 2
    left = img_array[:, :cx]
    right = np.fliplr(img_array[:, w - cx:])
    min_w = min(left.shape[1], right.shape[1])
    if min_w < 10:
        return 0.0
    left_flat = left[:, -min_w:].flatten()
    right_flat = right[:, :min_w].flatten()
    corr = np.corrcoef(left_flat, right_flat)[0, 1]
    return float(max(0.0, corr if not np.isnan(corr) else 0.0))


class OrganGeometryValidator:
    """Orchestrateur de validation multi-organe basé sur forme et géométrie."""

    def __init__(self):
        self.brain_detector = get_brain_anatomical_final()
        self.lung_detector = LungOpenCVDetector()

    def score_organ(self, image_bytes: bytes, organ: str) -> dict:
        scorers = {
            'cerveau': self._score_brain,
            'poumon': self._score_lung,
            'coeur': self._score_heart,
            'foie': self._score_liver,
            'sein': self._score_breast,
            'peau': self._score_skin,
            'oeil': self._score_eye,
        }
        scorer = scorers.get(organ)
        if not scorer:
            return {'confidence': 0.0, 'matched': False, 'reason': 'Organe non supporté'}
        return scorer(image_bytes)

    def score_all_organs(self, image_bytes: bytes) -> dict:
        return {organ: self.score_organ(image_bytes, organ) for organ in ORGAN_NAMES}

    def validate(self, image_bytes: bytes, expected_organ: str) -> dict:
        if expected_organ not in ORGAN_NAMES:
            return self._result(
                valid=False,
                expected_organ=expected_organ,
                detected_organ='unknown',
                confidence=0.0,
                reason=f"Organe '{expected_organ}' non supporté",
                all_scores={},
                method='geometry',
            )

        all_scores = self.score_all_organs(image_bytes)
        primary = all_scores[expected_organ]
        expected_name = ORGAN_NAMES[expected_organ]
        min_confidence = MIN_ACCEPT_CONFIDENCE_BY_ORGAN.get(expected_organ, MIN_ACCEPT_CONFIDENCE)

        # Rejet croisé : un autre organe incompatible est détecté avec confiance élevée
        for competing in CROSS_ORGAN_CHECKS.get(expected_organ, []):
            comp = all_scores[competing]
            cross_min = CROSS_ORGAN_MIN_CONFIDENCE.get(
                (expected_organ, competing), MIN_ACCEPT_CONFIDENCE
            )
            if comp.get('matched') and comp['confidence'] >= cross_min:
                comp_name = ORGAN_NAMES[competing]
                reason = (
                    f"Image incompatible : {comp_name} détecté ({comp['confidence']:.0f}%) "
                    f"au lieu de {expected_name}. "
                    f"Veuillez importer une image de {expected_name.lower()}."
                )
                return self._result(
                    valid=False,
                    expected_organ=expected_organ,
                    detected_organ=competing,
                    confidence=comp['confidence'],
                    reason=reason,
                    all_scores=all_scores,
                    method='geometry',
                )

        # Acceptation : le détecteur primaire confirme l'organe attendu
        if primary.get('matched') and primary['confidence'] >= min_confidence:
            return self._result(
                valid=True,
                expected_organ=expected_organ,
                detected_organ=expected_organ,
                confidence=primary['confidence'],
                reason=None,
                all_scores=all_scores,
                method='geometry',
            )

        # Meilleur organe détecté (pour message d'erreur)
        ranked = sorted(all_scores.items(), key=lambda x: x[1]['confidence'], reverse=True)
        best_organ, best_data = ranked[0]
        best_conf = best_data['confidence']

        if best_organ != expected_organ and best_data.get('matched') and best_conf >= min_confidence:
            reason = (
                f"Image incompatible : {ORGAN_NAMES[best_organ]} détecté ({best_conf:.0f}%) "
                f"au lieu de {expected_name} ({primary['confidence']:.0f}%). "
                f"Veuillez importer une image de {expected_name.lower()}."
            )
            return self._result(
                valid=False,
                expected_organ=expected_organ,
                detected_organ=best_organ,
                confidence=best_conf,
                reason=reason,
                all_scores=all_scores,
                method='geometry',
            )

        detail = primary.get('reason') or 'Anatomie non confirmée pour cet organe'
        reason = (
            f"Image refusée pour {expected_name} : {detail} "
            f"(confiance {primary['confidence']:.0f}%, minimum {min_confidence:.0f}%)."
        )
        return self._result(
            valid=False,
            expected_organ=expected_organ,
            detected_organ='unknown',
            confidence=primary['confidence'],
            reason=reason,
            all_scores=all_scores,
            method='geometry',
        )

    def _result(
        self,
        valid: bool,
        expected_organ: str,
        detected_organ: str,
        confidence: float,
        reason: Optional[str],
        all_scores: dict,
        method: str,
    ) -> dict:
        all_score_map = {k: round(v['confidence'], 2) for k, v in all_scores.items()}
        alternatives = sorted(
            [
                {'organ': k, 'confidence': round(v['confidence'], 2)}
                for k, v in all_scores.items()
                if k != detected_organ
            ],
            key=lambda x: x['confidence'],
            reverse=True,
        )[:3]

        detected_name = ORGAN_NAMES.get(detected_organ, 'Inconnu') if detected_organ != 'unknown' else 'Inconnu'

        return {
            'valid': valid,
            'expected_organ': expected_organ,
            'expected_organ_name': ORGAN_NAMES.get(expected_organ, expected_organ),
            'detected_organ': detected_organ,
            'detected_organ_name': detected_name,
            'organ': detected_organ if detected_organ != 'unknown' else expected_organ,
            'organ_name': detected_name if detected_organ != 'unknown' else ORGAN_NAMES.get(expected_organ, expected_organ),
            'confidence': round(confidence, 2),
            'reason': reason,
            'rejection_reason': reason,
            'all_scores': all_score_map,
            'alternative_organs': alternatives,
            'method': method,
            'anatomical_detector': True,
        }

    def _score_brain(self, image_bytes: bytes) -> dict:
        result = self.brain_detector.detect(image_bytes)
        return {
            'confidence': float(result.get('confidence', 0)),
            'matched': bool(result.get('is_brain', False)),
            'reason': result.get('rejection_reason'),
        }

    def _score_lung(self, image_bytes: bytes) -> dict:
        result = self.lung_detector.detect(image_bytes)
        conf = float(result.get('confidence', 0))
        matched = bool(result.get('is_lung', False))

        if matched:
            return {
                'confidence': conf,
                'matched': matched,
                'reason': result.get('rejection_reason'),
            }

        # Heuristique scanner thoracique (cavités latérales sombres + médiastin clair)
        try:
            img_array, img = _load_gray(image_bytes)
            if not _is_grayscale_medical(img):
                return {'confidence': conf, 'matched': False, 'reason': result.get('rejection_reason')}

            h, w = img_array.shape
            top, bottom = h // 5, 4 * h // 5
            left_region = img_array[top:bottom, w // 8:w // 2 - w // 12]
            right_region = img_array[top:bottom, w // 2 + w // 12:7 * w // 8]
            center_region = img_array[top:bottom, w // 2 - w // 14:w // 2 + w // 14]

            if left_region.size == 0 or right_region.size == 0:
                return {'confidence': conf, 'matched': False, 'reason': result.get('rejection_reason')}

            left_mean = float(np.mean(left_region))
            right_mean = float(np.mean(right_region))
            center_mean = float(np.mean(center_region))
            left_std = float(np.std(left_region))
            right_std = float(np.std(right_region))

            side_diff = abs(left_mean - right_mean) / max(left_mean, right_mean, 1.0)
            left_dark_frac = float(np.mean(left_region < 85))
            right_dark_frac = float(np.mean(right_region < 85))
            uniform_cavities = left_std < 22 and right_std < 22
            dark_lungs = (
                left_mean < center_mean * 0.78
                and right_mean < center_mean * 0.78
                and left_dark_frac > 0.35
                and right_dark_frac > 0.35
            )
            bright_mediastinum = center_mean > max(left_mean, right_mean) * 1.15
            wide_chest = (w / h if h else 1.0) >= 1.15

            # Exclure les coupes cérébrales (ventricules centraux sombres, pas médiastin clair)
            aspect = w / h if h else 1.0
            symmetry = _bilateral_symmetry(img_array)
            border_dark = float(np.mean(img_array[: h // 8, :])) < 55
            brain_ventricles = center_mean < max(left_mean, right_mean) * 0.90
            looks_brain = (
                0.75 <= aspect <= 1.35
                and symmetry > 0.72
                and border_dark
                and brain_ventricles
            )

            if looks_brain:
                return {'confidence': conf, 'matched': False, 'reason': 'Structure cérébrale détectée (pas un poumon)'}

            score = int(uniform_cavities) + int(dark_lungs) + int(bright_mediastinum) + int(side_diff < 0.2) + int(wide_chest)
            if dark_lungs and bright_mediastinum and side_diff < 0.25 and score >= 3:
                return {
                    'confidence': max(conf, min(58 + score * 8, 92.0)),
                    'matched': True,
                    'reason': None,
                }
        except Exception:
            pass

        return {
            'confidence': conf,
            'matched': matched,
            'reason': result.get('rejection_reason'),
        }

    def _score_heart(self, image_bytes: bytes) -> dict:
        try:
            img_array, img = _load_gray(image_bytes)
            if not _is_grayscale_medical(img):
                return {'confidence': 8.0, 'matched': False, 'reason': 'Image couleur (attendu IRM cardiaque grayscale)'}

            h, w = img_array.shape
            aspect = w / h if h else 1.0
            if not (0.75 <= aspect <= 1.35):
                return {'confidence': 18.0, 'matched': False, 'reason': 'Format non compatible IRM cardiaque'}

            center = img_array[h // 4:3 * h // 4, w // 4:3 * w // 4]
            border = np.concatenate([
                img_array[: h // 8, :].flatten(),
                img_array[-h // 8:, :].flatten(),
                img_array[:, : w // 8].flatten(),
                img_array[:, -w // 8:].flatten(),
            ])
            center_mean = float(np.mean(center))
            border_mean = float(np.mean(border))
            ring_contrast = center_mean > border_mean * 1.08

            # Cavité centrale sombre entourée de myocarde plus clair
            inner = img_array[h // 3:2 * h // 3, w // 3:2 * w // 3]
            inner_dark = float(np.percentile(inner, 25)) < float(np.percentile(center, 50))

            symmetry = _bilateral_symmetry(img_array)
            score = 0
            if ring_contrast:
                score += 1
            if inner_dark:
                score += 1
            if symmetry > 0.65:
                score += 1
            if 0.85 <= aspect <= 1.15:
                score += 1
            if float(np.std(img_array)) > 18:
                score += 1

            confidence = 28 + score * 11
            matched = score >= 5
            reason = None if matched else 'Structure cardiaque (anneau myocardique) non confirmée'
            return {'confidence': min(confidence, 88.0), 'matched': matched, 'reason': reason}
        except Exception as exc:
            return {'confidence': 0.0, 'matched': False, 'reason': str(exc)}

    def _score_liver(self, image_bytes: bytes) -> dict:
        try:
            img_array, img = _load_gray(image_bytes)
            if not _is_grayscale_medical(img):
                return {'confidence': 10.0, 'matched': False, 'reason': 'Image couleur (attendu scanner/IRM foie)'}

            h, w = img_array.shape
            right = img_array[:, w // 2:]
            left = img_array[:, : w // 2]
            right_std = float(np.std(right))
            left_std = float(np.std(left))
            right_mean = float(np.mean(right))
            left_mean = float(np.mean(left))

            # Foie: masse homogène droite, texture lisse
            homogeneity = right_std < left_std * 0.85 and right_std < 35
            dominance = right_mean > left_mean * 0.95
            smooth = float(np.mean(ndimage.sobel(right))) < float(np.mean(ndimage.sobel(left))) * 1.1

            score = int(homogeneity) + int(dominance) + int(smooth)
            if float(np.std(img_array)) < 12:
                return {'confidence': 12.0, 'matched': False, 'reason': 'Contraste insuffisant'}

            confidence = 25 + score * 16
            matched = score >= 3
            reason = None if matched else 'Morphologie hépatique (lobe droit homogène) non détectée'
            return {'confidence': min(confidence, 90.0), 'matched': matched, 'reason': reason}
        except Exception as exc:
            return {'confidence': 0.0, 'matched': False, 'reason': str(exc)}

    def _score_breast(self, image_bytes: bytes) -> dict:
        try:
            img_array, img = _load_gray(image_bytes)
            if not _is_grayscale_medical(img):
                return {'confidence': 12.0, 'matched': False, 'reason': 'Image couleur (attendu mammographie grayscale)'}

            h, w = img_array.shape
            if not (0.9 <= (w / h if h else 1) <= 1.6):
                return {'confidence': 15.0, 'matched': False, 'reason': 'Format non compatible mammographie'}

            cx = w // 2
            left = img_array[h // 5:4 * h // 5, w // 8:cx - w // 12]
            right = img_array[h // 5:4 * h // 5, cx + w // 12:7 * w // 8]
            if left.size == 0 or right.size == 0:
                return {'confidence': 12.0, 'matched': False, 'reason': 'Zone mammaire non détectée'}

            left_density = float(np.mean(left > np.percentile(left, 65)))
            right_density = float(np.mean(right > np.percentile(right, 65)))

            bilateral = left_density > 0.08 and right_density > 0.08
            symmetry = _bilateral_symmetry(img_array)
            rounded = 0.05 < left_density < 0.45 and 0.05 < right_density < 0.45

            score = int(bilateral) + int(symmetry > 0.62) + int(rounded)
            confidence = 22 + score * 16
            matched = score >= 3 and bilateral and left_density > 0.12
            reason = None if matched else 'Densités mammaires bilatérales non détectées'
            return {'confidence': min(confidence, 91.0), 'matched': matched, 'reason': reason}
        except Exception as exc:
            return {'confidence': 0.0, 'matched': False, 'reason': str(exc)}

    def _score_skin(self, image_bytes: bytes) -> dict:
        try:
            img = Image.open(io.BytesIO(image_bytes))
            color = _colorfulness(img)
            gray_score = 100.0 if _is_grayscale_medical(img) else 0.0

            # Peau / dermoscopie: image couleur avec motifs locaux
            img_rgb = np.array(img.convert('RGB'), dtype=np.float32)
            local_std = float(np.std(ndimage.generic_filter(img_rgb.mean(axis=2), np.std, size=15)))

            if gray_score > 50:
                return {
                    'confidence': max(5.0, 25.0 - gray_score * 0.2),
                    'matched': False,
                    'reason': 'Image grayscale médicale (attendu photo dermatologique couleur)',
                }

            score = 0
            if color > 18:
                score += 2
            if local_std > 8:
                score += 1
            if img_rgb.shape[0] > 100 and img_rgb.shape[1] > 100:
                score += 1

            confidence = 25 + score * 18
            matched = score >= 3 and color > 15
            reason = None if matched else 'Motifs cutanés couleur non détectés'
            return {'confidence': min(confidence, 93.0), 'matched': matched, 'reason': reason}
        except Exception as exc:
            return {'confidence': 0.0, 'matched': False, 'reason': str(exc)}

    def _score_eye(self, image_bytes: bytes) -> dict:
        try:
            img = Image.open(io.BytesIO(image_bytes)).convert('RGB')
            rgb = np.array(img, dtype=np.float32)
            r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]

            # Fond d'œil: dominante rouge/orange, souvent circulaire
            red_dom = float(np.mean(r)) > float(np.mean(g)) * 1.05
            orange_tone = float(np.mean(r - b)) > 12 and float(np.mean(g)) > float(np.mean(b))

            h, w = rgb.shape[:2]
            cy, cx = h // 2, w // 2
            y_grid, x_grid = np.ogrid[:h, :w]
            circle = ((x_grid - cx) ** 2 + (y_grid - cy) ** 2) <= (min(h, w) * 0.42) ** 2
            inside = rgb[circle]
            outside = rgb[~circle]
            if inside.size == 0 or outside.size == 0:
                return {'confidence': 10.0, 'matched': False, 'reason': 'Structure rétinienne circulaire absente'}

            inside_red = float(np.mean(inside[:, 0]))
            outside_mean = float(np.mean(outside))
            disc_bright = float(np.max(inside[:, 0])) > inside_red * 1.25

            score = int(red_dom) + int(orange_tone) + int(disc_bright) + int(inside_red > outside_mean * 0.8)
            confidence = 22 + score * 16
            matched = score >= 3
            reason = None if matched else 'Fond d\'œil (rétine colorée circulaire) non détecté'
            return {'confidence': min(confidence, 94.0), 'matched': matched, 'reason': reason}
        except Exception as exc:
            return {'confidence': 0.0, 'matched': False, 'reason': str(exc)}


_validator = None


def get_organ_geometry_validator() -> OrganGeometryValidator:
    global _validator
    if _validator is None:
        _validator = OrganGeometryValidator()
    return _validator
