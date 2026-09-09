package tn.esprit.scanservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.scanservice.entity.Scan;
import tn.esprit.scanservice.service.ScanService;
import tn.esprit.scanservice.service.ScanValidationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;
    private final ScanValidationService validationService;

    // CREATE SCAN
    @PostMapping
    public Scan create(@RequestBody Scan scan) {
        return scanService.createScan(scan);
    }

    // UPLOAD SCAN WITH VALIDATION
    @PostMapping("/upload")
    public ResponseEntity<?> uploadScan(
            @RequestParam("file") MultipartFile file,
            @RequestParam("patientId") String patientId,
            @RequestParam("doctorId") String doctorId,
            @RequestParam("specialtyCode") String specialtyCode,
            @RequestParam(value = "scanType", required = false) String scanType) {
        
        try {
            // Valider le fichier selon la spécialité
            ScanValidationService.ValidationResult validationResult = 
                validationService.validateScan(file, specialtyCode);
            
            if (!validationResult.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", validationResult.getValidationMessage(),
                    "detectedOrgan", validationResult.getDetectedOrgan(),
                    "confidence", validationResult.getConfidence()
                ));
            }
            
            // Créer le scan avec les résultats de validation
            Scan scan = Scan.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .specialtyCode(specialtyCode)
                .scanType(scanType != null ? scanType : specialtyCode + "_SCAN")
                .imageUrl(file.getOriginalFilename())
                .isValidated(true)
                .validationMessage(validationResult.getValidationMessage())
                .detectedOrgan(validationResult.getDetectedOrgan())
                .confidence(validationResult.getConfidence())
                .build();
            
            Scan savedScan = scanService.createScan(scan);
            
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "message", "Scan uploadé et validé avec succès",
                "scan", savedScan,
                "validation", validationResult
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "valid", false,
                "message", "Erreur lors de l'upload: " + e.getMessage()
            ));
        }
    }

    // GET ALL
    @GetMapping
    public List<Scan> getAll() {
        return scanService.getAllScans();
    }

    // GET BY PATIENT
    @GetMapping("/patient/{id}")
    public List<Scan> getByPatient(@PathVariable String id) {
        return scanService.getByPatient(id);
    }

    // GET BY DOCTOR AND SPECIALTY
    @GetMapping("/doctor/{doctorId}/specialty/{specialtyCode}")
    public List<Scan> getByDoctorAndSpecialty(
            @PathVariable String doctorId,
            @PathVariable String specialtyCode) {
        return scanService.getByDoctorAndSpecialty(doctorId, specialtyCode);
    }

    // GET VALIDATION RULES FOR SPECIALTY
    @GetMapping("/validation-rules/{specialtyCode}")
    public ResponseEntity<?> getValidationRules(@PathVariable String specialtyCode) {
        ScanValidationService.ValidationRules rules = 
            validationService.getValidationRulesForSpecialty(specialtyCode);
        
        if (rules == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(rules);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        scanService.delete(id);
    }
}
