"""SipDetect v6 configuration."""

import json
import os
from pathlib import Path
from typing import Any, Dict, List

SERVICE_ROOT = Path(__file__).resolve().parents[2]
MODELS_DIR = Path(os.getenv("SIPDETECT_MODEL_DIR", SERVICE_ROOT / "models"))
DEFAULT_CHECKPOINT = os.getenv("SIPDETECT_MODEL_PATH", str(MODELS_DIR / "model.pth"))
MODEL_CARD_PATH = MODELS_DIR / "model_card.json"

INPUT_SIZE = 300
N_SEGMENTS = 100
K_NN = 7
PH_CHANNELS = 8
PH_FEATURES = 12
NODE_FEATURES = 1550

CLASS_NAMES = [
    "Non Demented (CN)",
    "Very Mild (MCI)",
    "Mild (EMCI)",
    "Moderate (AD)",
]

CLASS_SHORT = ["CN", "VMCI", "EMCI", "AD"]

CLASS_NAMES_FR = {
    0: "Non Demented (CN)",
    1: "Very Mild (MCI)",
    2: "Mild (EMCI)",
    3: "Moderate (AD)",
}

DIAGNOSIS_FR = {
    0: "Aucun signe de démence détecté — profil cognitif normal.",
    1: "Stade Very Mild (MCI) — surveillance cognitive recommandée.",
    2: "Stade Mild (EMCI) — consultation neurologique recommandée.",
    3: "Stade Moderate (AD) — prise en charge spécialisée urgente.",
}

RECOMMENDATIONS_FR = {
    0: "Contrôle de routine. Maintenir une activité cognitive et physique régulière.",
    1: "Bilan neuropsychologique et IRM de suivi dans 6 à 12 mois.",
    2: "Stade détecté — consultation neurologique recommandée. Examens complémentaires (biomarqueurs, neuropsychologie).",
    3: "Stade modéré détecté — consultation neurologique urgente. Évaluation thérapeutique et suivi rapproché.",
}

F1_PRECISION_BY_CLASS = {
    0: 0.95,
    1: 0.78,
    2: 0.95,
    3: 1.0,
}

ANATOMICAL_ZONES = [
    "Cortex frontal",
    "Cortex pariétal",
    "Cortex temporal",
    "Cortex occipital",
    "Région hippocampique",
    "Ventricules latéraux",
]


def load_model_card() -> Dict[str, Any]:
    if MODEL_CARD_PATH.exists():
        with open(MODEL_CARD_PATH, encoding="utf-8") as handle:
            return json.load(handle)
    return {}


def get_checkpoint_path() -> Path:
    return Path(DEFAULT_CHECKPOINT)
