"""Validation des organes autorisés par spécialité médicale."""

from typing import Dict, List, Optional, Set

SPECIALTY_ORGAN_MAP: Dict[str, Set[str]] = {
    "Pneumologie": {"poumon"},
    "Oncologie": {"poumon"},
    "Neurologie": {"cerveau"},
    "Cardiologie": {"coeur"},
    "Dermatologie": {"peau"},
    "Ophtalmologie": {"oeil"},
    "Gynécologie": {"sein"},
    "Gastroentérologie": {"foie"},
    "Radiologie": {"cerveau", "sein", "peau", "oeil", "poumon", "foie", "coeur"},
    "Médecine générale": set(),
}

SPECIALTY_ALIASES: Dict[str, str] = {
    "POUMON": "Pneumologie",
    "PNEUMOLOGIE": "Pneumologie",
    "ONCOLOGIE": "Oncologie",
    "CERVEAU": "Neurologie",
    "ALZHEIMER": "Neurologie",
    "NEUROLOGIE": "Neurologie",
    "SEIN": "Gynécologie",
    "GYNECOLOGIE": "Gynécologie",
    "GYNÉCOLOGIE": "Gynécologie",
    "PEAU": "Dermatologie",
    "DERMATOLOGIE": "Dermatologie",
    "OEIL": "Ophtalmologie",
    "OPHTALMOLOGIE": "Ophtalmologie",
    "COEUR": "Cardiologie",
    "CARDIOLOGIE": "Cardiologie",
    "FOIE": "Gastroentérologie",
    "GASTROENTEROLOGIE": "Gastroentérologie",
    "GASTROENTÉROLOGIE": "Gastroentérologie",
    "RADIOLOGIE": "Radiologie",
    "MEDECINE_GENERALE": "Médecine générale",
    "Poumon / Nodules": "Pneumologie",
    "Cerveau (2D/3D Tumeurs)": "Neurologie",
    "Alzheimer (IRM)": "Neurologie",
    "Sein / Mammographie": "Gynécologie",
    "Lésions cutanées": "Dermatologie",
    "Rétinopathie diabétique": "Ophtalmologie",
    "Cardiaque (IRM)": "Cardiologie",
    "Foie / Segmentation": "Gastroentérologie",
}


def normalize_specialty(raw: Optional[str] = None, specialty_code: Optional[str] = None) -> str:
    code = (specialty_code or "").strip()
    if code and code in SPECIALTY_ALIASES:
        return SPECIALTY_ALIASES[code]

    value = (raw or "").strip()
    if not value:
        return ""

    if value in SPECIALTY_ALIASES:
        return SPECIALTY_ALIASES[value]

    for key in SPECIALTY_ORGAN_MAP:
        if key.lower() == value.lower():
            return key

    return value


def is_organ_allowed_for_specialty(specialty: str, expected_organ: str) -> bool:
    normalized = normalize_specialty(specialty)
    if not normalized or normalized not in SPECIALTY_ORGAN_MAP:
        return False
    return expected_organ in SPECIALTY_ORGAN_MAP[normalized]


def build_restriction_message(specialty: str) -> str:
    normalized = normalize_specialty(specialty) or specialty
    organs: Set[str] = SPECIALTY_ORGAN_MAP.get(normalized, set())
    if not organs:
        return (
            f"Votre spécialité ({normalized}) ne permet pas l'import d'images scanner/IRM. "
            "Veuillez orienter le patient vers un spécialiste."
        )
    organ_labels = ", ".join(sorted(organs))
    return f"Votre spécialité ({normalized}) permet uniquement l'analyse de : {organ_labels}."
