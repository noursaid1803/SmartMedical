"""SipDetect V20 configuration — EfficientNet-B4 + Swin-Small hybrid."""

import json
import os
from pathlib import Path
from typing import Any, Dict, List, Optional

N_CLASSES = 4
PATCH_SIZE = 96
IMG_SIZE = 224

CLASS_SHORT: List[str] = ["G0", "G1", "G2", "G3+"]
CLASS_NAMES: List[str] = ["Bénin", "Prob.Bénin", "Indéterminé", "Malin"]
CLASS_NAMES_FR: List[str] = [
    "Bénin (G0)",
    "Probablement bénin (G1)",
    "Indéterminé (G2)",
    "Malin (G3+)",
]

DEFAULT_CONFIG: Dict[str, Any] = {
    "ct_mean": [0.2789, 0.3018, 0.2942],
    "ct_std": [0.2760, 0.2865, 0.2832],
    "img_size": IMG_SIZE,
    "patch_size": PATCH_SIZE,
    "thresholds": [0.30, 0.20, 0.18, 0.16],
    "class_codes": CLASS_SHORT,
    "class_names": CLASS_NAMES,
    "alert_grade": 2,
    "architecture": "EfficientNet-B4 + Swin-Small + Gate",
    "recommended_checkpoint": "best_hybrid_v20.pt",
}

SEVERITY_BY_GRADE = ["low", "low", "medium", "high"]

RECOMMENDATIONS_FR: List[str] = [
    "Aucun nodule suspect détecté. Surveillance de routine recommandée. Contrôle annuel si facteurs de risque tabagique.",
    "Nodule probablement bénin (G1). Scanner thoracique de contrôle recommandé dans 6 à 12 mois. Surveillance clinique.",
    "Nodule indéterminé (G2). PET-scan ou biopsie à discuter. Avis pneumologique recommandé sous 3 mois.",
    "Nodule malin suspect (G3+). Consultation pneumo-oncologie urgente. Bilan d'extension et prise en charge spécialisée recommandés.",
]

DIAGNOSIS_FR: List[str] = [
    "Scanner pulmonaire normal — Grade G0 (Bénin)",
    "Nodule pulmonaire probablement bénin — Grade G1",
    "Nodule pulmonaire indéterminé — Grade G2",
    "Nodule pulmonaire malin suspect — Grade G3+ (Malin)",
]


def get_models_dir() -> Path:
    env_path = os.getenv("LUNG_CANCER_MODEL_DIR")
    if env_path:
        return Path(env_path)
    service_root = Path(__file__).resolve().parents[3]
    return service_root / "models" / "lung_cancer"


def get_model_path() -> Path:
    env_file = os.getenv("LUNG_CANCER_MODEL_PATH")
    if env_file:
        return Path(env_file)
    models_dir = get_models_dir()
    hybrid = models_dir / "best_hybrid_v20.pt"
    if hybrid.exists():
        return hybrid
    cnn = models_dir / "best_cnn_v20.pt"
    if cnn.exists():
        return cnn
    return hybrid


def load_runtime_config(models_dir: Optional[Path] = None) -> Dict[str, Any]:
    cfg = dict(DEFAULT_CONFIG)
    base = models_dir or get_models_dir()

    for name in ("config_v20.json", "sprint2_v20_final.json"):
        path = base / name
        if not path.exists():
            continue
        try:
            with open(path, encoding="utf-8") as handle:
                data = json.load(handle)
            if "ct_mean" in data:
                cfg["ct_mean"] = data["ct_mean"]
            if "ct_std" in data:
                cfg["ct_std"] = data["ct_std"]
            if "thresholds_s2" in data:
                cfg["thresholds"] = data["thresholds_s2"]
            elif "thresholds" in data:
                cfg["thresholds"] = data["thresholds"]
            if "optimal_thresholds" in data and isinstance(data["optimal_thresholds"], dict):
                cfg["thresholds"] = [
                    data["optimal_thresholds"].get(code, cfg["thresholds"][i])
                    for i, code in enumerate(CLASS_SHORT)
                ]
            break
        except (json.JSONDecodeError, OSError, KeyError, TypeError):
            continue

    return cfg
