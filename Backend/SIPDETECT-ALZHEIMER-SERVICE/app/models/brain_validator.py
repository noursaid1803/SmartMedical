"""Validation IRM cerveau — algorithme anatomique 7 points."""

import io
from typing import Any, Dict, List, Optional, Tuple

import cv2
import numpy as np
from PIL import Image


MIN_SCORE = 3
MIN_CRITERIA = 7


def _load_gray(image_bytes: bytes) -> np.ndarray:
    img = Image.open(io.BytesIO(image_bytes)).convert("L")
    return np.array(img, dtype=np.uint8)


def _segment_brain(img_array: np.ndarray) -> Tuple[np.ndarray, Dict[str, float]]:
    h, w = img_array.shape
    blurred = cv2.GaussianBlur(img_array, (5, 5), 0)
    _, thresh1 = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
    thresh_val = int(np.median(blurred) * 0.7)
    _, thresh2 = cv2.threshold(blurred, thresh_val, 255, cv2.THRESH_BINARY_INV)
    combined = cv2.bitwise_or(thresh1, thresh2)

    kernel_close = np.ones((7, 7), np.uint8)
    kernel_open = np.ones((5, 5), np.uint8)
    closed = cv2.morphologyEx(combined, cv2.MORPH_CLOSE, kernel_close)
    opened = cv2.morphologyEx(closed, cv2.MORPH_OPEN, kernel_open)

    num_labels, labels, stats, centroids = cv2.connectedComponentsWithStats(opened)
    if num_labels <= 1:
        return np.zeros_like(img_array), {"area_ratio": 0.0, "aspect": 1.0, "cx": w / 2, "cy": h / 2}

    img_area = h * w
    best_label = 1
    best_score = -1.0
    for label in range(1, num_labels):
        area = stats[label, cv2.CC_STAT_AREA]
        area_ratio = area / img_area
        if area_ratio < 0.15 or area_ratio > 0.85:
            continue
        cx, cy = centroids[label]
        dist = np.sqrt((cx - w / 2) ** 2 + (cy - h / 2) ** 2)
        centrality = 1.0 - min(dist / (0.5 * min(h, w)), 1.0)
        score = area_ratio * centrality
        if score > best_score:
            best_score = score
            best_label = label

    mask = (labels == best_label).astype(np.uint8) * 255
    x, y, bw, bh, _ = stats[best_label]
    aspect = bw / max(bh, 1)
    return mask, {
        "area_ratio": float(np.sum(mask > 0) / img_area),
        "aspect": float(aspect),
        "cx": float(centroids[best_label][0]),
        "cy": float(centroids[best_label][1]),
    }


class BrainMRIValidator:
    """Détecteur IRM cérébrale basé sur 7 critères anatomiques."""

    def validate(self, image_bytes: bytes) -> Dict[str, Any]:
        try:
            img_array = _load_gray(image_bytes)
            h, w = img_array.shape
            center_x, center_y = w // 2, h // 2

            mean_val = float(np.mean(img_array))
            std_val = float(np.std(img_array))
            if not (20 < mean_val < 230 and 5 < std_val < 80):
                return self._reject(
                    "Image non médicale ou qualité insuffisante",
                    {},
                    0,
                    [],
                )

            mask, meta = _segment_brain(img_array)
            brain_pixels = img_array[mask > 0]
            if brain_pixels.size == 0:
                return self._reject("Structure cérébrale non détectée", meta, 0, [])

            checks: Dict[str, Dict[str, Any]] = {}

            area_ratio = meta["area_ratio"]
            checks["taille_cerveau"] = {
                "passed": bool(0.18 <= area_ratio <= 0.82),
                "value": round(area_ratio, 2),
            }

            aspect = meta["aspect"]
            checks["forme_ovale"] = {
                "passed": bool(0.55 <= aspect <= 1.45),
                "value": round(aspect, 2),
            }

            dist_center = np.sqrt((meta["cx"] - center_x) ** 2 + (meta["cy"] - center_y) ** 2)
            center_ratio = dist_center / max(min(h, w) / 2, 1)
            checks["centre_ok"] = {
                "passed": bool(center_ratio < 0.35),
                "value": round(float(center_ratio), 2),
            }

            border_size = max(min(h, w) // 12, 4)
            border = np.concatenate([
                img_array[:border_size, :].flatten(),
                img_array[-border_size:, :].flatten(),
                img_array[:, :border_size].flatten(),
                img_array[:, -border_size:].flatten(),
            ])
            border_mean = float(np.mean(border))
            checks["fond_noir"] = {
                "passed": bool(border_mean < 55),
                "value": round(border_mean, 1),
            }

            center_region = img_array[
                max(0, center_y - h // 4): min(h, center_y + h // 4),
                max(0, center_x - w // 4): min(w, center_x + w // 4),
            ]
            left_half = center_region[:, : center_region.shape[1] // 2]
            right_half = center_region[:, center_region.shape[1] // 2:]
            mean_center = float(np.mean(center_region))
            dark_threshold = mean_center * 0.65
            left_dark = float(np.mean(left_half < dark_threshold)) if left_half.size else 0.0
            right_dark = float(np.mean(right_half < dark_threshold)) if right_half.size else 0.0
            ventricule_score = min(left_dark, right_dark)
            if left_dark > 0.03 and right_dark > 0.03:
                sym_bonus = 1.0 - abs(left_dark - right_dark) / max(left_dark, right_dark, 1e-6)
                ventricule_score = 0.5 + 0.5 * sym_bonus
            checks["ventricules_0.3"] = {
                "passed": bool(ventricule_score >= 0.12),
                "value": round(float(ventricule_score), 2),
            }

            left = img_array[:, :center_x]
            right = np.fliplr(img_array[:, w - center_x:])
            min_w = min(left.shape[1], right.shape[1])
            if min_w > 10:
                corr = np.corrcoef(left[:, :min_w].flatten(), right[:, :min_w].flatten())[0, 1]
                symmetry = float(max(0.0, corr if not np.isnan(corr) else 0.0))
            else:
                symmetry = 0.0
            checks["symétrie_0.60"] = {
                "passed": bool(symmetry >= 0.45),
                "value": round(symmetry, 2),
            }

            std_dev = float(np.std(brain_pixels))
            grad_x = cv2.Sobel(img_array, cv2.CV_64F, 1, 0, ksize=3)
            grad_y = cv2.Sobel(img_array, cv2.CV_64F, 0, 1, ksize=3)
            mean_grad = float(np.mean(np.sqrt(grad_x ** 2 + grad_y ** 2)[mask > 0]))
            texture_score = min(1.0, (std_dev / 40.0) * 0.5 + (mean_grad / 5.0) * 0.5)
            checks["texture_1.0"] = {
                "passed": bool(texture_score >= 0.35),
                "value": round(texture_score, 2),
            }

            score = sum(1 for c in checks.values() if c["passed"])
            criteria_tags = [
                f"{'✓' if c['passed'] else '✗'} {name}" for name, c in checks.items()
            ]

            if score >= MIN_SCORE:
                confidence = min(55.0 + (score - MIN_SCORE) * 8.0 + area_ratio * 15.0, 96.0)
                return {
                    "valid": True,
                    "is_brain": True,
                    "organ": "cerveau",
                    "organ_name": "Cerveau",
                    "confidence": round(confidence, 1),
                    "score": score,
                    "total_checks": MIN_CRITERIA,
                    "criteria": checks,
                    "criteria_tags": criteria_tags,
                    "message": "L'image correspond bien au modèle sélectionné.",
                    "method": "anatomical_7points",
                }

            failed = [name for name, c in checks.items() if not c["passed"]]
            return self._reject(
                f"IRM cérébrale non confirmée ({score}/{MIN_CRITERIA} critères)",
                meta,
                score,
                criteria_tags,
                failed=failed,
                checks=checks,
            )

        except Exception as exc:
            return self._reject(str(exc), {}, 0, [])

    def _reject(
        self,
        reason: str,
        meta: Dict[str, Any],
        score: int,
        criteria_tags: List[str],
        failed: Optional[List[str]] = None,
        checks: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        return {
            "valid": False,
            "is_brain": False,
            "organ": "unknown",
            "organ_name": "Inconnu",
            "confidence": max(score * 10.0, 10.0),
            "score": score,
            "total_checks": MIN_CRITERIA,
            "criteria": checks or {},
            "criteria_tags": criteria_tags,
            "failed_criteria": failed or [],
            "message": reason,
            "rejection_reason": reason,
            "method": "anatomical_7points",
            "details": meta,
        }


_validator = None


def get_brain_validator() -> BrainMRIValidator:
    global _validator
    if _validator is None:
        _validator = BrainMRIValidator()
    return _validator
