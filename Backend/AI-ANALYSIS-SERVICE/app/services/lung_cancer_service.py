"""Lung cancer analysis service."""

from typing import Any, Dict

from app.models.lung_cancer.config import DEFAULT_CONFIG, get_model_path, get_models_dir
from app.models.lung_cancer.inference import ModelNotFoundError, get_lung_cancer_detector


class LungCancerAnalysisService:
    def analyze_image(self, image_bytes: bytes) -> Dict[str, Any]:
        detector = get_lung_cancer_detector()
        return detector.analyze(image_bytes)

    def get_status(self) -> Dict[str, Any]:
        path = get_model_path()
        detector = get_lung_cancer_detector()
        return {
            "model_dir": str(get_models_dir()),
            "expected_checkpoint": DEFAULT_CONFIG["recommended_checkpoint"],
            "checkpoint_path": str(path),
            "checkpoint_exists": path.exists(),
            "model_loaded": detector.is_loaded,
            "architecture": DEFAULT_CONFIG["architecture"],
            "classes": DEFAULT_CONFIG["class_codes"],
            "load_error": detector.load_error,
        }


_service = None


def get_lung_cancer_service() -> LungCancerAnalysisService:
    global _service
    if _service is None:
        _service = LungCancerAnalysisService()
    return _service
