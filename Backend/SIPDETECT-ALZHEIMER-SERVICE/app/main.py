"""SipDetect v6 — Microservice FastAPI détection Alzheimer."""

from typing import Any, Dict, List, Optional

import uvicorn
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from app.models.inference import InvalidBrainMRIError, ModelNotFoundError
from app.services.alzheimer_service import get_alzheimer_service
from app.utils.json_utils import to_native

app = FastAPI(
    title="SipDetect Alzheimer Service",
    description="Microservice IA pour la détection de la maladie d'Alzheimer sur IRM cérébrales",
    version="6.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ALLOWED_TYPES = {"image/jpeg", "image/png", "image/jpg", "image/webp"}


class ValidationResponse(BaseModel):
    success: bool
    valid: bool
    organ: str
    organ_name: str
    confidence: float
    score: int
    total_checks: int
    criteria_tags: List[str]
    message: Optional[str] = None
    rejection_reason: Optional[str] = None


class AnalyzeResponse(BaseModel):
    success: bool
    stage: int
    stage_label: str
    stage_name: str
    confidence: float
    precision: float
    diagnosis: str
    recommendations: str
    gradcam_image_base64: Optional[str] = None
    probabilities: Dict[str, float]
    affected_zones: List[Dict[str, Any]]
    validation: Optional[Dict[str, Any]] = None
    model: Dict[str, Any]
    alert: bool


async def _read_upload(file: UploadFile) -> bytes:
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Type non supporté : {file.content_type}. Formats : JPG, PNG, WEBP",
        )
    data = await file.read()
    if not data:
        raise HTTPException(status_code=400, detail="Fichier vide")
    return data


@app.get("/")
async def root():
    return {
        "service": "SipDetect Alzheimer MS-2",
        "version": "v6-optimized-final",
        "status": "running",
        "endpoints": {
            "health": "GET /health",
            "status": "GET /status",
            "validate": "POST /validate",
            "analyze": "POST /analyze",
            "predict": "POST /predict",
        },
    }


@app.get("/health")
async def health():
    service = get_alzheimer_service()
    status = service.get_status()
    return {
        "status": "healthy" if status["checkpoint_exists"] else "degraded",
        "model_ready": status["checkpoint_exists"],
    }


@app.get("/status")
async def model_status():
    return get_alzheimer_service().get_status()


@app.post("/validate", response_model=ValidationResponse)
async def validate_brain_mri(file: UploadFile = File(...)):
    """
    Valide qu'une image est une IRM cérébrale via l'algorithme anatomique 7 points.
    """
    try:
        image_bytes = await _read_upload(file)
        result = get_alzheimer_service().validate_mri(image_bytes)
        return ValidationResponse(**to_native(result))
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Erreur validation : {exc}") from exc


@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze_alzheimer(file: UploadFile = File(...)):
    """
    Pipeline complet : validation IRM cerveau (7 points) puis analyse SipDetect v6
    avec stade, confiance, précision F1 et Grad-CAM.
    """
    try:
        image_bytes = await _read_upload(file)
        result = get_alzheimer_service().analyze(image_bytes)
        return AnalyzeResponse(**to_native(result))
    except InvalidBrainMRIError as exc:
        raise HTTPException(
            status_code=422,
            detail={
                "error": "invalid_brain_mri",
                "message": str(exc),
                "hint": "Seules les IRM cérébrales validées par l'algorithme 7 points sont acceptées.",
            },
        ) from exc
    except ModelNotFoundError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Erreur analyse : {exc}") from exc


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    """Alias de /analyze pour compatibilité model_card."""
    return await analyze_alzheimer(file)


@app.exception_handler(Exception)
async def general_exception_handler(_request, exc):
    return JSONResponse(status_code=500, content={"success": False, "error": str(exc)})


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8087)
