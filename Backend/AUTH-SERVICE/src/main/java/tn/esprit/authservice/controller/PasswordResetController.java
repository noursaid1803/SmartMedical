package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.service.PasswordResetService;

import java.util.Map;

@RestController
@RequestMapping("/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    
    /**
     * Étape 1: Demander un code de réinitialisation
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email requis"));
        }
        
        boolean sent = passwordResetService.requestPasswordReset(email);
        
        if (sent) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Code de vérification envoyé à votre email",
                    "email", email
            ));
        } else {
            // Pour des raisons de sécurité, ne pas révéler si l'email existe ou non
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Si cet email existe dans notre système, un code a été envoyé",
                    "email", email
            ));
        }
    }
    
    /**
     * Étape 2: Vérifier le code
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        if (email == null || code == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email et code requis"));
        }
        
        boolean valid = passwordResetService.verifyCode(email, code);
        
        if (valid) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Code vérifié avec succès",
                    "email", email,
                    "code", code
            ));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Code invalide ou expiré"));
        }
    }
    
    /**
     * Étape 3: Réinitialiser le mot de passe
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");
        
        if (email == null || code == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tous les champs sont requis"));
        }
        
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Le mot de passe doit contenir au moins 6 caractères"));
        }
        
        boolean success = passwordResetService.resetPassword(email, code, newPassword);
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mot de passe réinitialisé avec succès"
            ));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Erreur lors de la réinitialisation"));
        }
    }
}
