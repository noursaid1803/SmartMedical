"""
Service d'analyse d'images médicales
"""

from typing import Optional
import io
from PIL import Image
from app.models.organ_classifier import get_classifier, ORGAN_NAMES


class ImageAnalysisService:
    """
    Service pour analyser les images médicales et détecter les organes
    """
    
    def __init__(self, model_path: Optional[str] = None):
        self.classifier = get_classifier(model_path)
    
    def analyze_image(self, image_bytes: bytes, expected_organ: Optional[str] = None) -> dict:
        """
        Analyse une image médicale et retourne l'organe détecté
        
        Args:
            image_bytes: L'image en bytes
            expected_organ: L'organe attendu (optionnel, pour validation)
            
        Returns:
            Résultat de l'analyse avec organe détecté et confiance
        """
        # Valider l'image
        try:
            image = Image.open(io.BytesIO(image_bytes))
            # Vérifier que c'est une image valide
            image.verify()
        except Exception as e:
            return {
                'success': False,
                'error': f'Invalid image: {str(e)}',
                'organ': 'unknown',
                'confidence': 0.0
            }
        
        # Réouvrir l'image (verify() la ferme)
        image = Image.open(io.BytesIO(image_bytes))
        
        # Si un organe attendu est spécifié, valider la compatibilité
        if expected_organ:
            validation_result = self.classifier.validate_organ(image_bytes, expected_organ)
            
            return {
                'success': True,
                'organ': validation_result.get('detected_organ', validation_result.get('organ', 'unknown')),
                'organ_name': validation_result.get('detected_organ_name', validation_result.get('organ_name', 'Inconnu')),
                'confidence': validation_result['confidence'],
                'valid': validation_result['valid'],
                'expected_organ': validation_result.get('expected_organ'),
                'expected_organ_name': validation_result.get('expected_organ_name'),
                'reason': validation_result.get('reason') or validation_result.get('rejection_reason'),
                'alternative_organs': validation_result.get('alternative_organs', []),
                'all_scores': validation_result.get('all_scores', {}),
                'method': validation_result.get('method', 'geometry'),
            }
        else:
            # Simple prédiction sans validation
            prediction = self.classifier.predict(image_bytes)
            
            return {
                'success': prediction['detected'],
                'organ': prediction['organ'],
                'organ_name': prediction['organ_name'],
                'confidence': prediction['confidence'],
                'alternative_organs': prediction.get('alternatives', []),
                'all_scores': prediction.get('all_scores', {})
            }
    
    def get_supported_organs(self) -> list:
        """
        Retourne la liste des organes supportés
        """
        return [
            {'id': organ, 'name': name}
            for organ, name in ORGAN_NAMES.items()
        ]


# Instance singleton
_analysis_service = None

def get_analysis_service() -> ImageAnalysisService:
    """
    Obtient l'instance singleton du service d'analyse
    """
    global _analysis_service
    if _analysis_service is None:
        # Chemin vers les poids du modèle (si entraîné)
        import os
        model_path = os.getenv('MODEL_PATH', None)
        _analysis_service = ImageAnalysisService(model_path)
    return _analysis_service
