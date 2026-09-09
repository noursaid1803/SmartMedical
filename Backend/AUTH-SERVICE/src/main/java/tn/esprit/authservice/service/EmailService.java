package tn.esprit.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * EmailService - version console (pas besoin de JavaMailSender).
 * Tous les emails sont loggés dans la console pour le développement.
 */
@Service
@Slf4j
public class EmailService {

    public void sendDoctorWelcomeEmail(String to, String fullName, String verificationCode, String tempPassword) {
        log.info("========================================");
        log.info("📧 EMAIL MÉDECIN (MODE CONSOLE)");
        log.info("À: {}", to);
        log.info("Nom: Dr. {}", fullName);
        log.info("🎯 CODE DE VÉRIFICATION: {}", verificationCode);
        log.info("🔑 MOT DE PASSE TEMPORAIRE: {}", tempPassword);
        log.info("⏱️  Valable 24 heures — changez-le à la première connexion");
        log.info("🌐 URL: http://localhost:4200/login");
        log.info("========================================");
    }

    public void sendPatientWelcomeEmail(String to, String fullName, String verificationCode, String tempPassword) {
        log.info("========================================");
        log.info("📧 EMAIL PATIENT (MODE CONSOLE)");
        log.info("À: {}", to);
        log.info("Nom: {}", fullName);
        log.info("🎯 CODE DE VÉRIFICATION: {}", verificationCode);
        log.info("🔑 MOT DE PASSE TEMPORAIRE: {}", tempPassword);
        log.info("========================================");
    }

    public void sendVerificationConfirmationEmail(String to, String fullName) {
        log.info("========================================");
        log.info("📧 CONFIRMATION VÉRIFICATION (MODE CONSOLE)");
        log.info("À: {} - Nom: {}", to, fullName);
        log.info("✅ Compte vérifié avec succès !");
        log.info("========================================");
    }

    public void sendPasswordChangedConfirmation(String to, String fullName) {
        log.info("========================================");
        log.info("📧 MOT DE PASSE CHANGÉ (MODE CONSOLE)");
        log.info("À: {} - Nom: {}", to, fullName);
        log.info("✅ Mot de passe changé avec succès !");
        log.info("========================================");
    }

    public void sendPasswordResetCode(String to, String code, int expirationMinutes) {
        log.info("========================================");
        log.info("📧 RÉINITIALISATION MOT DE PASSE (MODE CONSOLE)");
        log.info("À: {}", to);
        log.info("🎯 CODE: {} (valable {} minutes)", code, expirationMinutes);
        log.info("========================================");
    }
}
