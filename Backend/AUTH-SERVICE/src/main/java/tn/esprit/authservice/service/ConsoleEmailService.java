package tn.esprit.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service d'email pour le développement qui affiche les codes dans la console
 * au lieu d'envoyer de vrais emails. Parfait pour les tests sans configuration SMTP.
 */
@Service
@ConditionalOnProperty(name = "app.email.mode", havingValue = "console", matchIfMissing = true)
@Slf4j
public class ConsoleEmailService implements EmailServiceInterface {

    @Override
    public void sendVerificationCode(String toEmail, String name, String code) {
        log.info("");
        log.info("========================================");
        log.info("📧 EMAIL DE VÉRIFICATION (MODE CONSOLE)");
        log.info("========================================");
        log.info("À: {}", toEmail);
        log.info("Nom: {}", name);
        log.info("");
        log.info("🎯 CODE DE VÉRIFICATION: {}", code);
        log.info("");
        log.info("Ce code est valable pendant 24 heures.");
        log.info("Saisissez ce code dans l'interface de vérification.");
        log.info("========================================");
        log.info("");
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String name) {
        log.info("");
        log.info("========================================");
        log.info("📧 EMAIL DE BIENVENUE (MODE CONSOLE)");
        log.info("========================================");
        log.info("À: {}", toEmail);
        log.info("Nom: {}", name);
        log.info("");
        log.info("✅ Votre compte administrateur a été activé avec succès !");
        log.info("🌐 URL: http://localhost:4200/admin");
        log.info("========================================");
        log.info("");
    }

    @Override
    public void sendDoctorWelcomeEmail(String toEmail, String name, String code, String tempPassword) {
        log.info("");
        log.info("========================================");
        log.info("📧 EMAIL DE BIENVENUE MÉDECIN (MODE CONSOLE)");
        log.info("========================================");
        log.info("À: {}", toEmail);
        log.info("Nom: Dr. {}", name);
        log.info("");
        log.info("🎯 CODE DE VÉRIFICATION: {}", code);
        log.info("🔑 MOT DE PASSE TEMPORAIRE: {}", tempPassword);
        log.info("");
        log.info("📝 Instructions:");
        log.info("1. Connectez-vous avec votre email et le mot de passe temporaire");
        log.info("2. Vérifiez votre compte avec le code de vérification");
        log.info("3. Changez votre mot de passe");
        log.info("🌐 URL: http://localhost:4200/login");
        log.info("========================================");
        log.info("");
    }
}
