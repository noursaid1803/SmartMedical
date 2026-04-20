package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.dto.CreateAdminRequest;
import tn.esprit.authservice.dto.VerifyCodeRequest;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.service.AdminService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // Créer un compte admin et envoyer code de vérification
    @PostMapping("/create")
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminRequest request) {
        try {
            User admin = adminService.createAdmin(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compte admin créé avec succès. Un code de vérification a été envoyé à l'email : " + request.getEmail());
            response.put("adminId", admin.getId());
            response.put("email", admin.getEmail());
            response.put("requiresVerification", true);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Vérifier le code et activer le compte
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequest request) {
        try {
            User admin = adminService.verifyCode(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compte vérifié et activé avec succès !");
            response.put("adminId", admin.getId());
            response.put("email", admin.getEmail());
            response.put("isVerified", admin.isVerified());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Renvoyer le code de vérification
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            adminService.resendVerificationCode(email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Un nouveau code de vérification a été envoyé à : " + email);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Créer l'admin Said Nour directement (endpoint rapide)
    @PostMapping("/setup-said-nour")
    public ResponseEntity<?> setupSaidNourAdmin() {
        try {
            CreateAdminRequest request = CreateAdminRequest.builder()
                    .name("said nour")
                    .email("noour.said1803@gmail.com")
                    .password("Admin123!")
                    .build();

            User admin = adminService.createAdmin(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admin 'said nour' créé avec succès ! Vérifiez l'email noour.said1803@gmail.com pour le code de vérification.");
            response.put("adminId", admin.getId());
            response.put("email", admin.getEmail());
            response.put("tempPassword", "Admin123!");
            response.put("requiresVerification", true);
            response.put("note", "IMPORTANT: Changez le mot de passe après la première connexion!");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Changer le mot de passe
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            adminService.changePassword(email, currentPassword, newPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mot de passe changé avec succès !");
            response.put("email", email);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
