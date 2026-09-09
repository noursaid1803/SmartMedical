package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.dto.PatientRegistrationRequest;
import tn.esprit.authservice.dto.PatientRegistrationResponse;
import tn.esprit.authservice.dto.VerifyCodeRequest;
import tn.esprit.authservice.entity.Patient;
import tn.esprit.authservice.service.PatientService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    // Créer un nouveau patient
    @PostMapping
    public ResponseEntity<?> createPatient(@RequestBody Patient patient) {
        try {
            Patient created = patientService.createPatient(patient);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Patient créé avec succès !");
            response.put("patient", created);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Enregistrer un patient avec email de confirmation
    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@RequestBody PatientRegistrationRequest request) {
        try {
            PatientRegistrationResponse response = patientService.registerPatient(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Vérifier le code du patient
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPatientCode(@RequestBody VerifyCodeRequest request) {
        try {
            Patient patient = patientService.verifyPatientCode(request.getEmail(), request.getCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compte patient vérifié avec succès !");
            response.put("patient", patient);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Changer le mot de passe du patient (première connexion)
    @PostMapping("/change-password")
    public ResponseEntity<?> changePatientPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String tempPassword = request.get("tempPassword");
            String newPassword = request.get("newPassword");

            Patient patient = patientService.changePatientPassword(email, tempPassword, newPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mot de passe changé avec succès !");
            response.put("patient", patient);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Récupérer tous les patients
    @GetMapping
    public ResponseEntity<?> getAllPatients() {
        try {
            List<Patient> patients = patientService.getAllPatients();
            return ResponseEntity.ok(patients);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Récupérer un patient par ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPatientById(@PathVariable String id) {
        try {
            Patient patient = patientService.getPatientById(id);
            return ResponseEntity.ok(patient);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // Rechercher des patients
    @GetMapping("/search")
    public ResponseEntity<?> searchPatients(@RequestParam String query) {
        try {
            List<Patient> patients = patientService.searchPatients(query);
            return ResponseEntity.ok(patients);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Mettre à jour un patient
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePatient(@PathVariable String id, @RequestBody Patient patient) {
        try {
            Patient updated = patientService.updatePatient(id, patient);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Patient mis à jour avec succès !");
            response.put("patient", updated);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Supprimer un patient
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable String id) {
        try {
            patientService.deletePatient(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Patient supprimé avec succès !");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
