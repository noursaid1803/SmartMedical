package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.dto.DoctorRequest;
import tn.esprit.authservice.dto.DoctorResponse;
import tn.esprit.authservice.dto.VerifyCodeRequest;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.service.DoctorService;
import tn.esprit.authservice.service.AdminService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final AdminService adminService;

    // Créer un nouveau médecin
    @PostMapping
    public ResponseEntity<?> createDoctor(@RequestBody DoctorRequest request) {
        try {
            var result = doctorService.createDoctor(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getEmailDeliveryMessage());
            response.put("doctor", result.getDoctor());
            response.put("tempPassword", result.getTempPassword());
            response.put("verificationCode", result.getVerificationCode());
            response.put("emailSent", result.isEmailSent());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Récupérer tous les médecins
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        List<DoctorResponse> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }

    // Récupérer les médecins actifs
    @GetMapping("/active")
    public ResponseEntity<List<DoctorResponse>> getActiveDoctors() {
        List<DoctorResponse> doctors = doctorService.getActiveDoctors();
        return ResponseEntity.ok(doctors);
    }

    // Récupérer un médecin par ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable String id) {
        try {
            DoctorResponse doctor = doctorService.getDoctorById(id);
            return ResponseEntity.ok(doctor);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // Rechercher par spécialisation
    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<DoctorResponse>> getDoctorsBySpecialization(@PathVariable String specialization) {
        List<DoctorResponse> doctors = doctorService.getDoctorsBySpecialization(specialization);
        return ResponseEntity.ok(doctors);
    }

    // Rechercher par hôpital
    @GetMapping("/hospital/{hospital}")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByHospital(@PathVariable String hospital) {
        List<DoctorResponse> doctors = doctorService.getDoctorsByHospital(hospital);
        return ResponseEntity.ok(doctors);
    }

    // Mettre à jour un médecin
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable String id, @RequestBody DoctorRequest request) {
        try {
            DoctorResponse doctor = doctorService.updateDoctor(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Médecin mis à jour avec succès !");
            response.put("doctor", doctor);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Activer/Désactiver un médecin
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleDoctorStatus(@PathVariable String id) {
        try {
            DoctorResponse doctor = doctorService.toggleDoctorStatus(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Statut du médecin changé avec succès !");
            response.put("doctor", doctor);
            response.put("active", doctor.isActive());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Supprimer un médecin
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable String id) {
        try {
            doctorService.deleteDoctor(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Médecin supprimé avec succès !");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ==================== ENDPOINTS DE VÉRIFICATION POUR LES MÉDECINS ====================

    // Vérifier le code et activer le compte médecin
    @PostMapping("/verify")
    public ResponseEntity<?> verifyDoctorCode(@RequestBody VerifyCodeRequest request) {
        try {
            User doctor = adminService.verifyCode(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compte médecin activé avec succès !");
            response.put("doctor", doctor);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Renvoyer le code de vérification
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendDoctorCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isBlank()) {
                throw new RuntimeException("Email requis");
            }
            // Pour les médecins : renvoyer code + mot de passe temporaire
            doctorService.resendDoctorCredentials(email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Nouveaux identifiants (code et mot de passe temporaire) envoyés !");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Renvoyer les identifiants (mot de passe temporaire) par l'administrateur
    @PostMapping("/resend-credentials")
    public ResponseEntity<?> resendDoctorCredentials(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isBlank()) {
                throw new RuntimeException("Email requis");
            }
            doctorService.resendDoctorCredentials(email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Un nouvel email avec mot de passe temporaire a été envoyé au médecin.");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Changer le mot de passe du médecin
    @PostMapping("/change-password")
    public ResponseEntity<?> changeDoctorPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            adminService.changePassword(email, currentPassword, newPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mot de passe changé avec succès !");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", "false");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
