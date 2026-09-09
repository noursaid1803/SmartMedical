"""SipDetect V20 inference — stage prediction, TTA, Grad-CAM localization."""

import base64
import io
from typing import Any, Dict, List, Optional

import numpy as np
import torch
import torch.nn.functional as F
import torchvision.transforms.functional as TF
from PIL import Image

from app.models.lung_cancer.architecture import CNNBaselineV20, HybridV20
from app.models.lung_cancer.config import (
    CLASS_NAMES,
    CLASS_NAMES_FR,
    CLASS_SHORT,
    DEFAULT_CONFIG,
    DIAGNOSIS_FR,
    RECOMMENDATIONS_FR,
    SEVERITY_BY_GRADE,
    get_model_path,
    load_runtime_config,
)
from app.models.lung_cancer.gradcam import GradCAMPlusPlus, get_bbox, overlay_gradcam
from app.models.lung_cancer.preprocessing import (
    bbox_to_location_label,
    denorm_axial,
    image_bytes_to_patch,
)


class ModelNotFoundError(FileNotFoundError):
    """Raised when SipDetect V20 weights are missing."""


class LungCancerDetector:
    """Lazy-loaded SipDetect V20 hybrid detector with Grad-CAM++."""

    def __init__(self):
        self.cfg = load_runtime_config()
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model: Optional[torch.nn.Module] = None
        self.is_hybrid = True
        self.model_path: Optional[str] = None
        self._load_error: Optional[str] = None

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

        path = get_model_path()
        if not path.exists():
            models_dir = path.parent
            self._load_error = (
                f"Poids du modèle introuvables. Placez best_hybrid_v20.pt dans "
                f"{models_dir} (ou définissez LUNG_CANCER_MODEL_PATH)."
            )
            raise ModelNotFoundError(self._load_error)

        self.cfg = load_runtime_config(path.parent)
        state = torch.load(path, map_location=self.device)

        if isinstance(state, dict) and "model_state_dict" in state:
            state_dict = state["model_state_dict"]
            if "ct_mean" in state:
                self.cfg["ct_mean"] = state["ct_mean"]
            if "ct_std" in state:
                self.cfg["ct_std"] = state["ct_std"]
            if "thresholds_s2" in state:
                self.cfg["thresholds"] = state["thresholds_s2"]
        else:
            state_dict = state

        hybrid_keys = [key for key in state_dict.keys() if key.startswith("cnn.") or key.startswith("vit.")]
        if hybrid_keys:
            model = HybridV20()
            self.is_hybrid = True
        else:
            model = CNNBaselineV20()
            self.is_hybrid = False

        model.load_state_dict(state_dict, strict=False)
        model.to(self.device)
        model.eval()

        self.model = model
        self.model_path = str(path)

    def _preprocess_patch(self, patch_np: np.ndarray) -> torch.Tensor:
        tensor = torch.tensor(patch_np, dtype=torch.float32)
        img_size = int(self.cfg["img_size"])
        if tensor.shape[-1] != img_size or tensor.shape[-2] != img_size:
            tensor = F.interpolate(
                tensor.unsqueeze(0),
                (img_size, img_size),
                mode="bilinear",
                align_corners=False,
            ).squeeze(0)

        mean = torch.tensor(self.cfg["ct_mean"]).view(3, 1, 1)
        std = torch.tensor(self.cfg["ct_std"]).view(3, 1, 1)
        tensor = (tensor - mean) / (std + 1e-8)
        return tensor.clamp(-5.0, 5.0).unsqueeze(0).to(self.device)

    def _apply_thresholds(self, probs: np.ndarray) -> int:
        thresholds = np.array(self.cfg["thresholds"], dtype=np.float32)
        scores = probs / (thresholds + 1e-6)
        return int(scores.argmax())

    def _forward_probs(self, tensor: torch.Tensor) -> np.ndarray:
        assert self.model is not None
        with torch.no_grad():
            logits = self.model(tensor)
        return F.softmax(logits, dim=1).squeeze(0).cpu().numpy()

    def _tta_predict(self, tensor: torch.Tensor) -> np.ndarray:
        probs_list: List[np.ndarray] = []
        for flip_h in (False, True):
            for flip_v in (False, True):
                for angle in (0, 10):
                    augmented = tensor.clone()
                    if flip_h:
                        augmented = TF.hflip(augmented)
                    if flip_v:
                        augmented = TF.vflip(augmented)
                    if angle:
                        augmented = TF.rotate(augmented, angle)
                    probs_list.append(self._forward_probs(augmented))
        return np.stack(probs_list).mean(0)

    def _encode_png_base64(self, rgb_array: np.ndarray) -> str:
        image = Image.fromarray(rgb_array)
        buffer = io.BytesIO()
        image.save(buffer, format="PNG")
        encoded = base64.b64encode(buffer.getvalue()).decode("ascii")
        return f"data:image/png;base64,{encoded}"

    def analyze(self, image_bytes: bytes, use_tta: bool = True) -> Dict[str, Any]:
        self.ensure_loaded()
        assert self.model is not None

        patch_size = int(self.cfg.get("patch_size", DEFAULT_CONFIG["patch_size"]))
        patch_np = image_bytes_to_patch(image_bytes, patch_size=patch_size)
        tensor = self._preprocess_patch(patch_np)

        probs = self._tta_predict(tensor) if use_tta else self._forward_probs(tensor)
        grade = self._apply_thresholds(probs)
        confidence_pct = round(float(probs[grade]) * 100, 1)

        gradcam = GradCAMPlusPlus(self.model, is_hybrid=self.is_hybrid)
        try:
            cam, _ = gradcam.generate(tensor.cpu(), target_class=grade, img_size=int(self.cfg["img_size"]))
            axial = denorm_axial(tensor.squeeze(0).cpu(), self.cfg["ct_mean"], self.cfg["ct_std"])
            overlay = overlay_gradcam(axial, cam)
            bbox, bbox_conf = get_bbox(cam)
        finally:
            gradcam.remove_hooks()

        detections: List[Dict[str, Any]] = []
        if grade > 0:
            location = "Zone nodulaire non localisée précisément"
            detection_conf = confidence_pct
            if bbox is not None:
                location = bbox_to_location_label(bbox, int(self.cfg["img_size"]))
                detection_conf = round(bbox_conf * 100, 1)

            detections.append(
                {
                    "type": f"Nodule pulmonaire — {CLASS_NAMES_FR[grade]}",
                    "location": location,
                    "confidence": detection_conf,
                    "severity": SEVERITY_BY_GRADE[grade],
                }
            )

        return {
            "success": True,
            "stage": grade,
            "stage_label": CLASS_SHORT[grade],
            "stage_name": CLASS_NAMES[grade],
            "confidence": confidence_pct,
            "isNormal": grade == 0,
            "diagnosis": DIAGNOSIS_FR[grade],
            "detections": detections,
            "recommendations": RECOMMENDATIONS_FR[grade],
            "gradcam_image_base64": self._encode_png_base64(overlay),
            "probabilities": {
                CLASS_SHORT[i]: round(float(probs[i]) * 100, 2) for i in range(len(CLASS_SHORT))
            },
            "model_loaded": True,
            "architecture": self.cfg.get("architecture", DEFAULT_CONFIG["architecture"]),
            "checkpoint": self.model_path,
            "alert": grade >= int(self.cfg.get("alert_grade", 2)),
        }


_detector: Optional[LungCancerDetector] = None


def get_lung_cancer_detector() -> LungCancerDetector:
    global _detector
    if _detector is None:
        _detector = LungCancerDetector()
    return _detector
