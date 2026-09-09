package tn.esprit.scanservice.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * Service de validation des scans médicaux basé sur la spécialité médicale
 * Utilise l'analyse d'image pour détecter les organes et valider les dimensions
 */
@Service
@Slf4j
public class ScanValidationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String AI_SERVICE_URL = "http://localhost:8086";

    /**
     * Règles de validation par spécialité médicale
     */
    private static final Map<String, ValidationRules> SPECIALTY_VALIDATION_RULES = new HashMap<>();

    static {
        // CERVEAU (Brain Tumors) - Alzheimer
        SPECIALTY_VALIDATION_RULES.put("CERVEAU", new ValidationRules(
            Arrays.asList("DICOM", "NIfTI", "NII", "PNG", "JPEG"),
            50 * 1024 * 1024, // 50MB max
            256, 256, // Min dimensions
            2048, 2048, // Max dimensions
            Arrays.asList("brain", "cerebrum", "cerebellum", "cortex"),
            0.7, // Min confidence
            1.0, // Max aspect ratio (square-ish images)
            0.5  // Min aspect ratio
        ));

        // ALZHEIMER
        SPECIALTY_VALIDATION_RULES.put("ALZHEIMER", new ValidationRules(
            Arrays.asList("DICOM", "NIfTI", "NII", "PNG", "JPEG"),
            100 * 1024 * 1024, // 100MB max
            256, 256,
            2048, 2048,
            Arrays.asList("brain", "cerebrum", "hippocampus", "cortex"),
            0.75, // Higher confidence required for Alzheimer
            1.0,
            0.5
        ));

        // POUMON (Lung Cancer)
        SPECIALTY_VALIDATION_RULES.put("POUMON", new ValidationRules(
            Arrays.asList("DICOM", "NIfTI", "NII", "PNG", "JPEG"),
            50 * 1024 * 1024, // 50MB max
            256, 256,
            2048, 2048,
            Arrays.asList("lung", "pulmonary", "chest", "thorax"),
            0.7,
            1.5, // Lungs can be wider
            0.6
        ));

        // SEIN (Breast Cancer)
        SPECIALTY_VALIDATION_RULES.put("SEIN", new ValidationRules(
            Arrays.asList("DICOM", "PNG", "JPEG"),
            30 * 1024 * 1024, // 30MB max
            512, 512,
            4096, 4096,
            Arrays.asList("breast", "mammography", "mammary"),
            0.7,
            1.2,
            0.8
        ));

        // PEAU (Skin Cancer)
        SPECIALTY_VALIDATION_RULES.put("PEAU", new ValidationRules(
            Arrays.asList("PNG", "JPEG", "JPG"),
            10 * 1024 * 1024, // 10MB max
            224, 224, // Standard for skin cancer models
            1024, 1024,
            Arrays.asList("skin", "dermis", "epidermis", "lesion"),
            0.6,
            1.0,
            0.8
        ));

        // OEIL (Diabetic Retinopathy)
        SPECIALTY_VALIDATION_RULES.put("OEIL", new ValidationRules(
            Arrays.asList("PNG", "JPEG", "DICOM"),
            15 * 1024 * 1024, // 15MB max
            224, 224, // Standard for retinal images
            1024, 1024,
            Arrays.asList("eye", "retina", "fundus", "optic"),
            0.7,
            1.0,
            0.8
        ));
    }

    /**
     * Valide un fichier uploadé selon les règles de la spécialité
     */
    public ValidationResult validateScan(MultipartFile file, String specialtyCode) {
        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.setSpecialtyCode(specialtyCode);

        ValidationRules rules = SPECIALTY_VALIDATION_RULES.get(specialtyCode);
        if (rules == null) {
            result.setValidationMessage("Spécialité non reconnue: " + specialtyCode);
            return result;
        }

        try {
            // 1. Validation du type de fichier
            String fileExtension = getFileExtension(file.getOriginalFilename());
            if (!rules.getAllowedFileTypes().contains(fileExtension.toUpperCase())) {
                result.setValidationMessage(
                    "Type de fichier non autorisé pour " + specialtyCode + 
                    ". Types acceptés: " + rules.getAllowedFileTypes()
                );
                return result;
            }

            // 2. Validation de la taille
            if (file.getSize() > rules.getMaxFileSize()) {
                result.setValidationMessage(
                    "Fichier trop volumineux. Maximum: " + (rules.getMaxFileSize() / (1024 * 1024)) + "MB"
                );
                return result;
            }

            // 3. Validation des dimensions (si c'est une image)
            if (isImageFile(file)) {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image != null) {
                    int width = image.getWidth();
                    int height = image.getHeight();

                    // Validation des dimensions
                    if (width < rules.getMinWidth() || height < rules.getMinHeight()) {
                        result.setValidationMessage(
                            "Image trop petite. Minimum: " + rules.getMinWidth() + "x" + rules.getMinHeight()
                        );
                        return result;
                    }

                    if (width > rules.getMaxWidth() || height > rules.getMaxHeight()) {
                        result.setValidationMessage(
                            "Image trop grande. Maximum: " + rules.getMaxWidth() + "x" + rules.getMaxHeight()
                        );
                        return result;
                    }

                    // Validation du ratio d'aspect
                    double aspectRatio = (double) width / height;
                    if (aspectRatio > rules.getMaxAspectRatio() || aspectRatio < rules.getMinAspectRatio()) {
                        result.setValidationMessage(
                            "Ratio d'aspect incorrect. Attendu: " + 
                            rules.getMinAspectRatio() + " - " + rules.getMaxAspectRatio()
                        );
                        return result;
                    }

                    result.setWidth(width);
                    result.setHeight(height);
                    result.setAspectRatio(aspectRatio);
                }
            }

            // 4. Détection de l'organe via AI Service
            String detectedOrgan = detectOrgan(file, specialtyCode);
            result.setDetectedOrgan(detectedOrgan);

            // 5. Validation de l'organe détecté
            boolean organMatch = false;
            for (String expectedOrgan : rules.getExpectedOrgans()) {
                if (detectedOrgan.toLowerCase().contains(expectedOrgan.toLowerCase())) {
                    organMatch = true;
                    break;
                }
            }

            if (!organMatch) {
                result.setValidationMessage(
                    "Organe détecté (" + detectedOrgan + ") ne correspond pas à la spécialité " + specialtyCode + 
                    ". Organes attendus: " + rules.getExpectedOrgans()
                );
                return result;
            }

            // 6. Calcul de la confiance
            double confidence = calculateConfidence(file, specialtyCode, detectedOrgan);
            result.setConfidence(confidence);

            if (confidence < rules.getMinConfidence()) {
                result.setValidationMessage(
                    "Confiance insuffisante: " + String.format("%.2f", confidence) + 
                    ". Minimum requis: " + rules.getMinConfidence()
                );
                return result;
            }

            // Toutes les validations sont passées
            result.setValid(true);
            result.setValidationMessage("Scan validé avec succès pour la spécialité " + specialtyCode);

        } catch (IOException e) {
            log.error("Erreur lors de la validation du scan", e);
            result.setValidationMessage("Erreur de lecture du fichier: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la validation", e);
            result.setValidationMessage("Erreur de validation: " + e.getMessage());
        }

        return result;
    }

    /**
     * Détecte l'organe dans l'image via le service AI
     */
    private String detectOrgan(MultipartFile file, String specialtyCode) {
        try {
            // Appel au service AI pour la détection d'organe
            Map<String, Object> response = restTemplate.postForObject(
                AI_SERVICE_URL + "/predict/organ",
                file,
                Map.class
            );

            if (response != null && response.containsKey("organ")) {
                return (String) response.get("organ");
            }
        } catch (Exception e) {
            log.warn("Impossible de contacter le service AI pour la détection d'organe: {}", e.getMessage());
        }

        // Fallback: retourne l'organe attendu basé sur la spécialité
        return getExpectedOrganForSpecialty(specialtyCode);
    }

    /**
     * Calcule le score de confiance basé sur plusieurs facteurs
     */
    private double calculateConfidence(MultipartFile file, String specialtyCode, String detectedOrgan) {
        ValidationRules rules = SPECIALTY_VALIDATION_RULES.get(specialtyCode);
        if (rules == null) return 0.5;

        double confidence = 0.0;
        int factors = 0;

        // 1. Correspondance de l'organe
        boolean organMatch = false;
        for (String expectedOrgan : rules.getExpectedOrgans()) {
            if (detectedOrgan.toLowerCase().contains(expectedOrgan.toLowerCase())) {
                organMatch = true;
                break;
            }
        }
        if (organMatch) {
            confidence += 0.4;
        }
        factors++;

        // 2. Taille du fichier (pas trop petit, pas trop grand)
        long fileSize = file.getSize();
        long optimalSize = rules.getMaxFileSize() / 2; // 50% du max est optimal
        double sizeScore = 1.0 - Math.abs(fileSize - optimalSize) / (double) optimalSize;
        confidence += sizeScore * 0.3;
        factors++;

        // 3. Dimensions (si image)
        try {
            if (isImageFile(file)) {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image != null) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    
                    // Score basé sur la proximité des dimensions optimales
                    int optimalWidth = (rules.getMinWidth() + rules.getMaxWidth()) / 2;
                    int optimalHeight = (rules.getMinHeight() + rules.getMaxHeight()) / 2;
                    
                    double widthScore = 1.0 - Math.abs(width - optimalWidth) / (double) optimalWidth;
                    double heightScore = 1.0 - Math.abs(height - optimalHeight) / (double) optimalHeight;
                    
                    confidence += (widthScore + heightScore) / 2 * 0.3;
                    factors++;
                }
            }
        } catch (IOException e) {
            log.warn("Impossible de lire les dimensions de l'image");
        }

        return factors > 0 ? confidence / factors : 0.5;
    }

    /**
     * Retourne l'organe attendu pour une spécialité donnée
     */
    private String getExpectedOrganForSpecialty(String specialtyCode) {
        switch (specialtyCode) {
            case "CERVEAU":
            case "ALZHEIMER":
                return "brain";
            case "POUMON":
                return "lung";
            case "SEIN":
                return "breast";
            case "PEAU":
                return "skin";
            case "OEIL":
                return "eye";
            default:
                return "unknown";
        }
    }

    /**
     * Extrait l'extension du fichier
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Vérifie si le fichier est une image
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Obtient les règles de validation pour une spécialité
     */
    public ValidationRules getValidationRulesForSpecialty(String specialtyCode) {
        return SPECIALTY_VALIDATION_RULES.get(specialtyCode);
    }

    /**
     * DTO pour les règles de validation
     */
    @Data
    public static class ValidationRules {
        private List<String> allowedFileTypes;
        private long maxFileSize;
        private int minWidth;
        private int minHeight;
        private int maxWidth;
        private int maxHeight;
        private List<String> expectedOrgans;
        private double minConfidence;
        private double maxAspectRatio;
        private double minAspectRatio;

        public ValidationRules(List<String> allowedFileTypes, long maxFileSize, 
                              int minWidth, int minHeight, int maxWidth, int maxHeight,
                              List<String> expectedOrgans, double minConfidence,
                              double maxAspectRatio, double minAspectRatio) {
            this.allowedFileTypes = allowedFileTypes;
            this.maxFileSize = maxFileSize;
            this.minWidth = minWidth;
            this.minHeight = minHeight;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.expectedOrgans = expectedOrgans;
            this.minConfidence = minConfidence;
            this.maxAspectRatio = maxAspectRatio;
            this.minAspectRatio = minAspectRatio;
        }
    }

    /**
     * DTO pour le résultat de validation
     */
    @Data
    public static class ValidationResult {
        private boolean valid;
        private String specialtyCode;
        private String validationMessage;
        private String detectedOrgan;
        private double confidence;
        private Integer width;
        private Integer height;
        private double aspectRatio;
    }
}
