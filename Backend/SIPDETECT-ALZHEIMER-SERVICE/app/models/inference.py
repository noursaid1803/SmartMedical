"""SipDetect v6 inference — classification + Grad-CAM."""

from typing import Any, Dict, Optional

import numpy as np
import torch
import torch.nn.functional as F

from app.models.architecture import CNNGATHybrid
from app.models.brain_validator import get_brain_validator
from app.models.config import (
    CLASS_NAMES,
    CLASS_NAMES_FR,
    CLASS_SHORT,
    DIAGNOSIS_FR,
    F1_PRECISION_BY_CLASS,
    RECOMMENDATIONS_FR,
    get_checkpoint_path,
    load_model_card,
)
from app.models.gradcam import encode_image_base64, generate_gradcam, locate_affected_zone
from app.models.graph_builder import batch_graph, build_graph_from_tensor, preprocess_image


class ModelNotFoundError(FileNotFoundError):
    pass


class InvalidBrainMRIError(ValueError):
    pass


class AlzheimerDetector:
    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model: Optional[CNNGATHybrid] = None
        self.temperature: float = 1.0
        self.model_path: Optional[str] = None
        self._load_error: Optional[str] = None
        self.metrics: Dict[str, Any] = {}
        self.validator = get_brain_validator()

    @property
    def is_loaded(self) -> bool:
        return self.model is not None

    @property
    def load_error(self) -> Optional[str]:
        return self._load_error

    def ensure_loaded(self) -> None:
        if self.model is not None:
            return
        if self._load_error:
            raise ModelNotFoundError(self._load_error)

        path = get_checkpoint_path()
        if not path.exists():
            self._load_error = (
                f"Poids du modèle introuvables. Placez model.pth dans {path.parent} "
                f"(ou définissez SIPDETECT_MODEL_PATH)."
            )
            raise ModelNotFoundError(self._load_error)

        checkpoint = torch.load(path, map_location=self.device, weights_only=False)
        edge_dim = int(checkpoint.get("gat_edge_channels", 1))
        self.temperature = float(checkpoint.get("temperature", 1.0))
        self.metrics = checkpoint.get("metrics", {})

        model = CNNGATHybrid(edge_dim=edge_dim)
        model.load_state_dict(checkpoint["model_state_dict"], strict=True)
        model.to(self.device)
        model.eval()

        self.model = model
        self.model_path = str(path)

    def validate_mri(self, image_bytes: bytes) -> Dict[str, Any]:
        return self.validator.validate(image_bytes)

    def analyze(self, image_bytes: bytes, skip_validation: bool = False) -> Dict[str, Any]:
        validation = None if skip_validation else self.validate_mri(image_bytes)
        if validation and not validation.get("valid"):
            raise InvalidBrainMRIError(validation.get("rejection_reason") or validation.get("message"))

        self.ensure_loaded()
        assert self.model is not None

        img_tensor, _ = preprocess_image(image_bytes)
        img_tensor = img_tensor.to(self.device)

        graph, _ = build_graph_from_tensor(img_tensor, self.model.cnn)
        graph = graph.to(self.device)
        graph_batch = batch_graph(graph).to(self.device)

        with torch.no_grad():
            logits = self.model(img_tensor, graph_batch)
            probs = F.softmax(logits / self.temperature, dim=1).squeeze(0).cpu().numpy()

        stage = int(np.argmax(probs))
        confidence_pct = round(float(probs[stage]) * 100, 1)
        precision_pct = round(F1_PRECISION_BY_CLASS.get(stage, 0.9) * 100, 1)

        original, overlay, cam, cam_intensity = generate_gradcam(
            self.model, img_tensor, graph_batch, stage
        )
        zone, zone_conf = locate_affected_zone(cam, cam.shape[0])

        model_card = load_model_card()
        affected_zones = []
        if stage > 0:
            activation = round(float(zone_conf), 1)
            affected_zones.append({
                "region": zone,
                "label": "Zone d'activation principale",
                "activation_pct": activation,
                "confidence_pct": activation,
                "intensity": "Élevée" if activation >= 60 else "Modérée",
                "severity": "high" if activation >= 60 else "medium",
            })

        metrics_test = model_card.get("metrics_test", {})
        return {
            "success": True,
            "validation": validation,
            "stage": stage,
            "stage_label": CLASS_SHORT[stage],
            "stage_name": CLASS_NAMES[stage],
            "stage_name_fr": CLASS_NAMES_FR[stage],
            "confidence": confidence_pct,
            "stage_f1_precision_pct": precision_pct,
            "precision": confidence_pct,
            "is_normal": bool(stage == 0),
            "diagnosis": DIAGNOSIS_FR[stage],
            "recommendations": RECOMMENDATIONS_FR[stage],
            "probabilities": {
                CLASS_SHORT[i]: round(float(probs[i]) * 100, 2) for i in range(len(CLASS_SHORT))
            },
            "probabilities_detail": [
                {
                    "stage": CLASS_NAMES[i],
                    "short": CLASS_SHORT[i],
                    "probability_pct": round(float(probs[i]) * 100, 2),
                }
                for i in range(len(CLASS_NAMES))
            ],
            "gradcam": {
                "original_image_base64": encode_image_base64(original),
                "heatmap_image_base64": encode_image_base64(overlay),
                "global_intensity_pct": round(cam_intensity, 1),
                "legend": {
                    "blue": "Activation faible",
                    "orange": "Activation modérée",
                    "red": "Zones suspectes (forte activation)",
                },
            },
            "gradcam_image_base64": encode_image_base64(overlay),
            "affected_zones": affected_zones,
            "model": {
                "name": model_card.get("model_name", "SipDetect v6"),
                "version": model_card.get("version", "v6-optimized-final"),
                "architecture": "EfficientNet-B3 + GATv2Conv + Persistent Homology (TDA)",
                "metrics": {
                    "global_accuracy_pct": round(metrics_test.get("Accuracy", 0.9047) * 100, 2),
                    "f1_macro_pct": round(metrics_test.get("F1_macro", 0.9126) * 100, 2),
                    "auc_roc_pct": round(metrics_test.get("AUC", 0.9869) * 100, 2),
                    "qwk_pct": round(metrics_test.get("QWK", 0.9461) * 100, 2),
                    "f1_detected_stage_pct": precision_pct,
                },
                "checkpoint": self.model_path,
                "loaded": True,
            },
            "alert": bool(stage >= 2),
        }


_detector: Optional[AlzheimerDetector] = None


def get_alzheimer_detector() -> AlzheimerDetector:
    global _detector
    if _detector is None:
        _detector = AlzheimerDetector()
    return _detector
