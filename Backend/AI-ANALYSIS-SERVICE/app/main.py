"""
AI Analysis Service - FastAPI Application
Microservice pour l'analyse d'images médicales par IA
"""

from fastapi import FastAPI, APIRouter, File, UploadFile, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
import uvicorn
import os

from app.services.image_analysis_service import get_analysis_service
from app.services.lung_cancer_service import get_lung_cancer_service
from app.services.specialty_validation import is_organ_allowed_for_specialty, build_restriction_message
from app.models.lung_cancer.inference import ModelNotFoundError

# Création de l'application FastAPI
app = FastAPI(
    title="AI Medical Analysis Service",
    description="Service d'analyse d'images médicales par intelligence artificielle",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS activé pour permettre les requêtes depuis Angular
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Router avec préfixe /ai pour correspondre à la configuration Gateway
router = APIRouter(prefix="/ai")

# Modèles Pydantic
class OrganPredictionResponse(BaseModel):
    success: bool
    organ: str
    organ_name: str
    confidence: float
    alternative_organs: List[dict]
    all_scores: dict

class OrganValidationResponse(BaseModel):
    success: bool
    valid: bool
    organ: str
    organ_name: str
    confidence: float
    expected_organ: Optional[str]
    expected_organ_name: Optional[str]
    reason: Optional[str] = None
    alternative_organs: List[dict]
    all_scores: dict
    method: Optional[str] = None

class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    model_loaded: bool


class LungCancerDetection(BaseModel):
    type: str
    location: str
    confidence: float
    severity: str


class LungCancerAnalysisResponse(BaseModel):
    success: bool
    stage: int
    stage_label: str
    stage_name: str
    confidence: float
    isNormal: bool
    diagnosis: str
    detections: List[LungCancerDetection]
    recommendations: str
    gradcam_image_base64: Optional[str] = None
    probabilities: Optional[Dict[str, float]] = None
    architecture: Optional[str] = None
    model_loaded: bool = True
    alert: Optional[bool] = None


# Endpoints
@app.get("/", response_model=HealthResponse)
async def health_check():
    """
    Endpoint de vérification de santé du service
    """
    return HealthResponse(
        status="healthy",
        service="AI Medical Analysis Service",
        version="1.0.0",
        model_loaded=True
    )


@router.get("/organs")
async def get_supported_organs():
    """
    Retourne la liste des organes supportés par le service
    """
    service = get_analysis_service()
    organs = service.get_supported_organs()
    
    return {
        "organs": organs,
        "count": len(organs)
    }


@router.post("/predict", response_model=OrganPredictionResponse)
async def predict_organ(file: UploadFile = File(...)):
    """
    Prédit l'organe présent dans une image médicale
    
    - **file**: Image médicale (JPG, PNG, DICOM)
    
    Retourne l'organe détecté avec son niveau de confiance
    """
    # Vérifier le type de fichier
    allowed_types = ['image/jpeg', 'image/png', 'image/jpg', 'application/dicom']
    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail=f"File type not supported. Allowed: {', '.join(allowed_types)}"
        )
    
    try:
        # Lire l'image
        image_bytes = await file.read()
        
        if len(image_bytes) == 0:
            raise HTTPException(status_code=400, detail="Empty file")
        
        # Analyser l'image
        service = get_analysis_service()
        result = service.analyze_image(image_bytes)
        
        if not result['success']:
            raise HTTPException(status_code=500, detail=result.get('error', 'Analysis failed'))
        
        return OrganPredictionResponse(
            success=True,
            organ=result['organ'],
            organ_name=result['organ_name'],
            confidence=result['confidence'],
            alternative_organs=result.get('alternative_organs', []),
            all_scores=result.get('all_scores', {})
        )
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Analysis error: {str(e)}")


@router.post("/validate", response_model=OrganValidationResponse)
async def validate_organ(
    file: UploadFile = File(...),
    expected_organ: str = Form(...),
    doctor_specialty: Optional[str] = Form(None)
):
    """
    Valide si une image médicale correspond à l'organe attendu
    
    - **file**: Image médicale (JPG, PNG, DICOM)
    - **expected_organ**: Organe attendu (cerveau, sein, poumon, etc.)
    
    Retourne si l'image est compatible avec le modèle sélectionné
    """
    # Vérifier le type de fichier
    allowed_types = ['image/jpeg', 'image/png', 'image/jpg', 'application/dicom', 'image/webp']
    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail=f"File type '{file.content_type}' not supported. Allowed: JPEG, PNG, DICOM"
        )
    
    # Liste des organes valides
    valid_organs = ['cerveau', 'sein', 'peau', 'oeil', 'poumon', 'foie', 'coeur']
    if expected_organ not in valid_organs:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid organ. Must be one of: {', '.join(valid_organs)}"
        )

    if doctor_specialty and not is_organ_allowed_for_specialty(doctor_specialty, expected_organ):
        message = build_restriction_message(doctor_specialty)
        return OrganValidationResponse(
            success=False,
            valid=False,
            organ="unknown",
            organ_name="Inconnu",
            confidence=0.0,
            expected_organ=expected_organ,
            expected_organ_name=expected_organ,
            reason=message,
            alternative_organs=[],
            all_scores={},
            method="specialty-guard",
        )
    
    try:
        # Lire l'image
        image_bytes = await file.read()
        
        if len(image_bytes) == 0:
            raise HTTPException(status_code=400, detail="Empty file")
        
        # Analyser et valider
        service = get_analysis_service()
        result = service.analyze_image(image_bytes, expected_organ)
        
        if not result['success']:
            raise HTTPException(status_code=500, detail=result.get('error', 'Validation failed'))
        
        return OrganValidationResponse(
            success=True,
            valid=result['valid'],
            organ=result['organ'],
            organ_name=result['organ_name'],
            confidence=result['confidence'],
            expected_organ=result.get('expected_organ'),
            expected_organ_name=result.get('expected_organ_name'),
            reason=result.get('reason'),
            alternative_organs=result.get('alternative_organs', []),
            all_scores=result.get('all_scores', {}),
            method=result.get('method'),
        )
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Validation error: {str(e)}")


@router.get("/analyze/lung-cancer/status")
async def lung_cancer_model_status():
    """
    Statut du modèle SipDetect V20 (EfficientNet-B4 + Swin-Small).
    """
    service = get_lung_cancer_service()
    return service.get_status()


@router.post("/analyze/lung-cancer", response_model=LungCancerAnalysisResponse)
async def analyze_lung_cancer(
    file: UploadFile = File(...),
    doctor_specialty: Optional[str] = Form(None)
):
    """
    Analyse pulmonaire SipDetect V20 : stade du nodule (G0–G3+) + localisation Grad-CAM++.

    - **file**: Scanner / image pulmonaire (JPG, PNG)

    Placez `best_hybrid_v20.pt` dans `models/lung_cancer/` avant utilisation.
    """
    allowed_types = ["image/jpeg", "image/png", "image/jpg", "image/webp"]
    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail=f"Type de fichier non supporté : {file.content_type}. Formats : JPG, PNG",
        )

    if doctor_specialty and not is_organ_allowed_for_specialty(doctor_specialty, "poumon"):
        raise HTTPException(status_code=403, detail=build_restriction_message(doctor_specialty))

    try:
        image_bytes = await file.read()
        if len(image_bytes) == 0:
            raise HTTPException(status_code=400, detail="Fichier vide")

        service = get_lung_cancer_service()
        result = service.analyze_image(image_bytes)
        return LungCancerAnalysisResponse(**result)

    except ModelNotFoundError as exc:
        raise HTTPException(
            status_code=503,
            detail=str(exc),
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Erreur analyse pulmonaire : {str(exc)}")


@router.post("/analyze")
async def analyze_medical_image(
    file: UploadFile = File(...),
    model_type: Optional[str] = Form(None)
):
    """
    Endpoint principal pour analyse médicale complète
    
    - **file**: Image médicale
    - **model_type**: Type de modèle/analyse souhaité (optionnel)
    
    Retourne une analyse complète de l'image
    """
    try:
        image_bytes = await file.read()
        
        if len(image_bytes) == 0:
            raise HTTPException(status_code=400, detail="Empty file")
        
        service = get_analysis_service()
        
        # Analyse de base
        result = service.analyze_image(image_bytes)
        
        # Enrichir la réponse avec des métadonnées
        return {
            "success": result['success'],
            "analysis": {
                "organ": result['organ'],
                "organ_name": result['organ_name'],
                "confidence": result['confidence'],
                "alternative_organs": result.get('alternative_organs', [])
            },
            "image_info": {
                "filename": file.filename,
                "content_type": file.content_type,
                "size_bytes": len(image_bytes)
            },
            "model_used": "ResNet50_Medical_Organ_Classifier",
            "timestamp": None  # Sera ajouté par le client
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Analysis error: {str(e)}")


@router.get("/model/info")
async def get_model_info():
    """
    Retourne les informations sur le modèle utilisé
    """
    return {
        "model_name": "ResNet50 Medical Organ Classifier",
        "architecture": "ResNet50",
        "pretrained": "ImageNet",
        "num_classes": 7,
        "classes": [
            {"id": "cerveau", "name": "Cerveau"},
            {"id": "sein", "name": "Sein"},
            {"id": "peau", "name": "Peau"},
            {"id": "oeil", "name": "Œil"},
            {"id": "poumon", "name": "Poumon"},
            {"id": "foie", "name": "Foie"},
            {"id": "coeur", "name": "Cœur"}
        ],
        "input_shape": [224, 224, 3],
        "framework": "PyTorch"
    }


# Chatbot Models & Endpoint
class ChatbotMessage(BaseModel):
    role: str
    content: str

class ChatbotRequest(BaseModel):
    patient_id: Optional[str] = None
    patient_history: Optional[Dict[str, Any]] = None
    message: str
    chat_history: Optional[List[ChatbotMessage]] = []

class ChatbotResponse(BaseModel):
    response: str
    suggestions: List[str]

@router.post("/chatbot", response_model=ChatbotResponse)
async def medical_chatbot(request: ChatbotRequest):
    """
    Chatbot médical intelligent basé sur l'historique du patient.
    """
    try:
        # Extraire les infos
        history = request.patient_history or {}
        msg = request.message.strip()
        msg_lower = msg.lower()
        
        # 1. Tenter d'appeler l'API Gemini si la clé existe
        gemini_key = os.getenv("GEMINI_API_KEY")
        if gemini_key:
            try:
                first_name = history.get("firstName", "Patient")
                last_name = history.get("lastName", "")
                age = history.get("age", "")
                gender = history.get("gender", "")
                med_hist = history.get("medicalHistory", "Aucun antécédent particulier")
                allergies = history.get("allergies", "Aucune allergie connue")
                meds = history.get("currentMedications", "Aucun traitement en cours")
                
                records = history.get("records", [])
                records_str = ""
                for idx, rec in enumerate(records):
                    date_str = rec.get("consultationDate", rec.get("createdAt", ""))
                    if isinstance(date_str, str) and len(date_str) > 10:
                        date_str = date_str[:10]
                    records_str += f"- Consultation {idx+1} ({date_str}) par {rec.get('doctorName', 'Médecin')}:\n"
                    records_str += f"  Raison: {rec.get('reasonForVisit', '')}\n"
                    records_str += f"  Symptômes: {rec.get('symptoms', '')}\n"
                    records_str += f"  Diagnostic: {rec.get('diagnosis', '')}\n"
                    if rec.get('aiResult'):
                        records_str += f"  Analyse IA: {rec.get('aiResult')}\n"
                    if rec.get('clinicalNotes'):
                        records_str += f"  Notes cliniques: {rec.get('clinicalNotes')}\n"
                    if rec.get('recommendations'):
                        records_str += f"  Recommandations: {rec.get('recommendations')}\n"
                
                chat_history_str = ""
                for chat_msg in request.chat_history or []:
                    role_name = "Patient" if chat_msg.role == "user" else "Assistant"
                    chat_history_str += f"{role_name}: {chat_msg.content}\n"
                
                prompt = (
                    f"Tu es un assistant médical virtuel intelligent pour la plateforme SmartMedical.\n"
                    f"Voici les informations médicales du patient :\n"
                    f"Nom Complet: {first_name} {last_name}\n"
                    f"Âge: {age} ans | Genre: {gender}\n"
                    f"Antécédents médicaux: {med_hist}\n"
                    f"Allergies: {allergies}\n"
                    f"Traitements en cours: {meds}\n"
                    f"Historique des examens et consultations :\n"
                    f"{records_str}\n\n"
                    f"Historique de la conversation :\n"
                    f"{chat_history_str}\n"
                    f"Question actuelle du patient : {msg}\n\n"
                    f"Réponds de manière professionnelle, claire et empathique en français (au format Markdown).\n"
                    f"Structure ta réponse avec des sections ou listes à puces si nécessaire.\n"
                    f"Fournis des recommandations préliminaires utiles, des alertes de surveillance et des examens complémentaires pertinents si approprié.\n"
                    f"IMPORTANT : Termine TOUJOURS obligatoirement ta réponse par un paragraphe distinct contenant exactement cette mention de non-responsabilité :\n"
                    f"\"Cette recommandation ne remplace pas l'avis du médecin.\""
                )
                
                import requests
                import json
                
                url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={gemini_key}"
                headers = {"Content-Type": "application/json"}
                payload = {
                    "contents": [{
                        "parts": [{
                            "text": prompt
                        }]
                    }]
                }
                
                response = requests.post(url, headers=headers, json=payload, timeout=10)
                if response.status_code == 200:
                    resp_json = response.json()
                    response_text = resp_json['candidates'][0]['content']['parts'][0]['text']
                    
                    suggestions = ["Résume mon dossier", "Quels sont mes traitements ?", "Quels examens complémentaires faire ?"]
                    if "diab" in msg_lower or "diab" in str(med_hist).lower():
                        suggestions.append("Précautions pour le diabète")
                    if "nodule" in msg_lower or "poumon" in msg_lower:
                        suggestions.append("Symptômes pulmonaires à surveiller")
                    
                    return ChatbotResponse(response=response_text.strip(), suggestions=suggestions)
            except Exception as gemini_err:
                print(f"[CHATBOT] Gemini call failed, falling back to local expert system: {gemini_err}")
                
        # 2. Système expert local (Fallback de haute qualité)
        first_name = history.get("firstName", "Patient")
        last_name = history.get("lastName", "")
        age = history.get("age", "")
        gender = history.get("gender", "")
        med_hist = str(history.get("medicalHistory", "")).lower()
        allergies = str(history.get("allergies", "")).lower()
        meds = str(history.get("currentMedications", "")).lower()
        records = history.get("records", [])
        
        has_lung_issue = "poumon" in med_hist or "nodule" in med_hist or "asthme" in med_hist or "pulm" in med_hist
        has_brain_issue = "cerveau" in med_hist or "alzheimer" in med_hist or "démence" in med_hist or "demence" in med_hist
        has_diabetes = "diabète" in med_hist or "diabete" in med_hist or "metformine" in meds
        has_heart_issue = "coeur" in med_hist or "cardiaque" in med_hist or "tension" in med_hist or "hypertension" in med_hist
        
        for rec in records:
            diag = str(rec.get("diagnosis", "")).lower()
            if "poumon" in diag or "nodule" in diag or "cancer" in diag:
                has_lung_issue = True
            if "cerveau" in diag or "alzheimer" in diag:
                has_brain_issue = True
            if "coeur" in diag or "cardiaque" in diag or "insuffisance" in diag:
                has_heart_issue = True
            if "diabète" in diag or "diabete" in diag:
                has_diabetes = True

        response_text = ""
        suggestions = ["Résume mon dossier", "Quels sont mes traitements ?", "Quels examens complémentaires faire ?"]
        
        if "resume" in msg_lower or "résumé" in msg_lower or "dossier" in msg_lower or "historique" in msg_lower:
            gender_label = "Homme" if gender == 'M' else "Femme" if gender == 'F' else "Patient"
            age_label = f" âgé de {age} ans" if age else ""
            response_text = (
                f"### Résumé Synthétique de votre Dossier Médical 📋\n\n"
                f"Bonjour **{first_name} {last_name}**,\n\n"
                f"Voici une synthèse de vos données médicales disponibles sur notre plateforme :\n\n"
                f"- **Profil Personnel :** {gender_label}{age_label}.\n"
                f"- **Groupe Sanguin :** {history.get('bloodType', 'Non spécifié')}\n"
                f"- **Antécédents médicaux :** {history.get('medicalHistory', 'Aucun antécédent renseigné')}\n"
                f"- **Allergies :** {history.get('allergies', 'Aucune allergie connue')}\n"
                f"- **Traitements actuels :** {history.get('currentMedications', 'Aucun traitement en cours')}\n\n"
            )
            
            if records:
                response_text += "**Dernières consultations & examens :**\n"
                for idx, rec in enumerate(records[:3]):
                    date_str = rec.get("consultationDate", rec.get("createdAt", ""))
                    if isinstance(date_str, str) and len(date_str) > 10:
                        date_str = date_str[:10]
                    response_text += f"- **Le {date_str}** par {rec.get('doctorName', 'Médecin')} :\n"
                    response_text += f"  - *Diagnostic :* {rec.get('diagnosis')}\n"
                    if rec.get('aiResult'):
                        try:
                            import json
                            ai_data = json.loads(rec.get('aiResult'))
                            response_text += f"  - *Analyse IA :* {ai_data.get('diagnosis')} (Confiance: {ai_data.get('confidence')}%)\n"
                        except:
                            response_text += f"  - *Analyse IA :* {rec.get('aiResult')}\n"
                    if rec.get('recommendations'):
                        response_text += f"  - *Recommandations :* {rec.get('recommendations')}\n"
            else:
                response_text += "*Aucune consultation enregistrée à ce jour.*"
                
            suggestions = ["Quels sont mes traitements ?", "Quels examens complémentaires faire ?", "Conseils d'hygiène de vie"]

        elif "traitement" in msg_lower or "médicament" in msg_lower or "medicament" in msg_lower or "ordonnance" in msg_lower:
            meds_raw = history.get("currentMedications", "")
            if meds_raw and meds_raw != "Aucun" and meds_raw != "Aucun traitement en cours":
                response_text = (
                    f"### Vos Traitements Prescrits 💊\n\n"
                    f"D'après vos dossiers, vous suivez actuellement le(s) traitement(s) suivant(s) :\n"
                    f"👉 **{meds_raw}**\n\n"
                    f"**Recommandations importantes :**\n"
                    f"1. **Respect de l'ordonnance :** Prenez vos médicaments à heure régulière, selon la posologie exacte prescrite par votre médecin.\n"
                    f"2. **Effets secondaires :** Si vous ressentez des effets indésirables, parlez-en à votre praticien sans arrêter brusquement le traitement.\n"
                )
                allergies_str = history.get("allergies", "")
                if allergies_str and ("pénicilline" in str(allergies_str).lower() or "penicilline" in str(allergies_str).lower()):
                    response_text += "\n⚠️ **Alerte Allergie :** Rappel important, vous êtes allergique à la **Pénicilline**. Informez-en tout médecin qui vous prescrirait un nouvel antibiotique.\n"
            else:
                response_text = (
                    f"### Vos Traitements 💊\n\n"
                    f"Aucun traitement actif n'est répertorié dans votre dossier numérique.\n"
                    f"Si un médecin vous a récemment prescrit une ordonnance papier, veillez à ce qu'elle soit ajoutée à votre dossier."
                )
            suggestions = ["Résume mon dossier", "Conseils d'hygiène de vie", "Quels examens complémentaires faire ?"]

        elif "examen" in msg_lower or "complémentaire" in msg_lower or "faire" in msg_lower or "analyse" in msg_lower or "scan" in msg_lower or "irm" in msg_lower or "radio" in msg_lower:
            response_text = f"### Suggestions d'Examens Complémentaires 🔬\n\n"
            if has_lung_issue:
                response_text += (
                    f"Compte tenu de vos antécédents pulmonaires et/ou de la suspicion de nodule :\n"
                    f"- Un **Scanner Thoracique (CT Scan Thorax) de contrôle** est recommandé sous 3 à 6 mois pour surveiller toute évolution de la taille du nodule.\n"
                    f"- Des **Épreuves Fonctionnelles Respiratoires (EFR)** peuvent être demandées par votre pneumologue pour mesurer vos capacités pulmonaires.\n"
                )
            elif has_brain_issue:
                response_text += (
                    f"Compte tenu de votre suivi en Neurologie :\n"
                    f"- Une **IRM cérébrale de suivi** est généralement préconisée pour évaluer la stabilité des structures corticales et de l'hippocamppe.\n"
                    f"- Des **tests neuropsychologiques réguliers** (évaluation cognitive) sont conseillés.\n"
                )
            elif has_heart_issue:
                response_text += (
                    f"Compte tenu de vos antécédents cardiovasculaires :\n"
                    f"- Une **Échographie Cardiaque** pour surveiller la fraction d'éjection et les cavités cardiaques.\n"
                    f"- Un **Électrocardiogramme (ECG)** de contrôle annuel.\n"
                )
            else:
                response_text += (
                    f"Sur la base de votre profil général, les examens standards recommandés incluent :\n"
                    f"- Un **bilan sanguin annuel** (glycémie, cholestérol, fonction rénale).\n"
                    f"- Une surveillance de la tension artérielle lors de chaque consultation.\n"
                )
            suggestions = ["Symptômes à surveiller", "Résume mon dossier", "Quels sont mes traitements ?"]

        elif "symptôme" in msg_lower or "alerte" in msg_lower or "surveiller" in msg_lower or "danger" in msg_lower or "toux" in msg_lower or "respirer" in msg_lower or "memoire" in msg_lower or "mémoire" in msg_lower:
            response_text = f"### Vigilance & Symptômes à Surveiller ⚠️\n\n"
            if has_lung_issue:
                response_text += (
                    f"En raison de vos antécédents pulmonaires, veuillez surveiller attentivement les signes suivants :\n"
                    f"- Une augmentation ou un changement de nature de votre toux (toux grasse persistante, crachats striés de sang).\n"
                    f"- Une sensation d'essoufflement ou de gêne respiratoire inhabituelle (dyspnée), même au repos.\n"
                    f"- Une douleur thoracique aiguë ou persistante lors de l'inspiration.\n"
                    f"**En présence de l'un de ces symptômes, contactez rapidement votre pneumologue.**\n"
                )
            elif has_brain_issue:
                response_text += (
                    f"Pour les troubles neurologiques ou de la mémoire :\n"
                    f"- Surveillez toute désorientation spatio-temporelle soudaine.\n"
                    f"- Des difficultés inhabituelles à réaliser des tâches quotidiennes simples (ex: cuisiner, gérer un budget).\n"
                    f"- Des changements marqués d'humeur ou de comportement.\n"
                )
            elif has_diabetes:
                response_text += (
                    f"En tant que patient suivi pour diabète :\n"
                    f"- Surveillez toute soif intense, fatigue extrême ou besoin d'uriner anormalement fréquent (signes d'hyperglycémie).\n"
                    f"- Faites attention aux tremblements, sueurs froides et vertiges (signes d'hypoglycémie).\n"
                    f"- Inspectez quotidiennement vos pieds pour détecter toute plaie ou rougeur.\n"
                )
            else:
                response_text += (
                    f"De manière générale, restez attentif à :\n"
                    f"- Une fatigue persistante et inexpliquée.\n"
                    f"- Une perte de poids soudaine et involontaire.\n"
                    f"- Une fièvre inexpliquée durant plus de 48 heures.\n"
                )
            suggestions = ["Quels examens complémentaires faire ?", "Résume mon dossier", "Conseils d'hygiène de vie"]
            
        else:
            first_name_display = f" **{first_name}**" if first_name else ""
            response_text = (
                f"Bonjour{first_name_display}, je suis votre Assistant Médical intelligent.\n\n"
                f"Je peux analyser l'historique de votre dossier SmartMedical pour vous aider à mieux comprendre vos diagnostics, vos examens IA et vos prescriptions.\n\n"
                f"Que souhaitez-vous savoir aujourd'hui ? Vous pouvez par exemple me demander :\n"
                f"- *\"Résume mon dossier médical\"*\n"
                f"- *\"Quels sont mes traitements actuels ?\"*\n"
                f"- *\"Quels examens de contrôle devrais-je faire ?\"*\n"
            )
            suggestions = ["Résume mon dossier", "Quels sont mes traitements ?", "Conseils d'hygiène de vie"]

        response_text += "\n\n---\n*⚠️ Cette recommandation ne remplace pas l'avis du médecin.*"
        
        return ChatbotResponse(response=response_text, suggestions=suggestions)
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Chatbot error: {str(e)}")


# Inclusion du router avec préfixe /ai
app.include_router(router)

# Gestion des erreurs
@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    return JSONResponse(
        status_code=500,
        content={"success": False, "error": str(exc)}
    )


if __name__ == "__main__":
    port = int(os.getenv("PORT", 8086))
    host = os.getenv("HOST", "0.0.0.0")
    
    print(f"Starting AI Analysis Service on {host}:{port}")
    uvicorn.run(app, host=host, port=port)
