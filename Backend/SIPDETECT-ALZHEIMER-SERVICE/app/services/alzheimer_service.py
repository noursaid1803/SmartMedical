"""Alzheimer analysis service."""

from typing import Any, Dict

from app.models.config import MODELS_DIR, get_checkpoint_path, load_model_card
from app.models.inference import get_alzheimer_detector


class AlzheimerAnalysisService:
    def validate_mri(self, image_bytes: bytes) -> Dict[str, Any]:
        detector = get_alzheimer_detector()
        result = detector.validate_mri(image_bytes)
        return {"success": True, **result}

    def analyze(self, image_bytes: bytes) -> Dict[str, Any]:
        detector = get_alzheimer_detector()
        return detector.analyze(image_bytes)

    def get_status(self) -> Dict[str, Any]:
        detector = get_alzheimer_detector()
        path = get_checkpoint_path()
        card = load_model_card()
        return {
            "service": "SipDetect Alzheimer MS-2",
            "model_dir": str(MODELS_DIR),
            "checkpoint_path": str(path),
            "checkpoint_exists": path.exists(),
            "model_loaded": detector.is_loaded,
            "architecture": card.get("architecture", {}),
            "classes": card.get("classes", {}),
            "load_error": detector.load_error,
        }


_service = None


def get_alzheimer_service() -> AlzheimerAnalysisService:
    global _service
    if _service is None:
        _service = AlzheimerAnalysisService()
    return _service
