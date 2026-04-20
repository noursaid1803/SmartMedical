package tn.esprit.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    /**
     * Envoyer un email de bienvenue au médecin avec les identifiants
     */
    public void sendDoctorWelcomeEmail(String to, String fullName, String tempPassword, String verificationCode) {
        // Log l'email (pour le développement)
        log.info("========================================");
        log.info("EMAIL ENVOYÉ À: {}", to);
        log.info("========================================");
        log.info("Objet: Bienvenue sur SmartMedical - Vos identifiants");
        log.info("");
        log.info("Bonjour Dr. {},", fullName);
        log.info("");
        log.info("Votre compte médecin a été créé sur SmartMedical.");
        log.info("");
        log.info("Vos identifiants de connexion temporaires:");
        log.info("  Email: {}", to);
        log.info("  Mot de passe temporaire: {}", tempPassword);
        log.info("");
        log.info("Code de vérification: {}", verificationCode);
        log.info("");
        log.info("IMPORTANT:");
        log.info("1. Connectez-vous avec ces identifiants");
        log.info("2. Entrez le code de vérification");
        log.info("3. Changez immédiatement votre mot de passe");
        log.info("");
        log.info("Le code et le mot de passe temporaire expirent dans 24 heures.");
        log.info("========================================");
        
        // TODO: Implémenter l'envoi d'email réel avec JavaMailSender
        // Exemple:
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(to);
        // message.setSubject("Bienvenue sur SmartMedical");
        // message.setText(body);
        // mailSender.send(message);
    }

    /**
     * Envoyer un email de confirmation après vérification
     */
    public void sendVerificationConfirmationEmail(String to, String fullName) {
        log.info("========================================");
        log.info("EMAIL CONFIRMATION À: {}", to);
        log.info("========================================");
        log.info("Objet: Votre compte SmartMedical est vérifié");
        log.info("");
        log.info("Bonjour Dr. {},", fullName);
        log.info("");
        log.info("Votre compte a été vérifié avec succès.");
        log.info("Vous pouvez maintenant changer votre mot de passe.");
        log.info("========================================");
    }

    /**
     * Envoyer un email après changement de mot de passe
     */
    public void sendPasswordChangedConfirmation(String to, String fullName) {
        log.info("========================================");
        log.info("EMAIL CONFIRMATION CHANGEMENT MDP À: {}", to);
        log.info("========================================");
        log.info("Objet: Votre mot de passe a été changé");
        log.info("");
        log.info("Bonjour Dr. {},", fullName);
        log.info("");
        log.info("Votre mot de passe a été changé avec succès.");
        log.info("Vous pouvez maintenant accéder à votre interface.");
        log.info("========================================");
    }
}
