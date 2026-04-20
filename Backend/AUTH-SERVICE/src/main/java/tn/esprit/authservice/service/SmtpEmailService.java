package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service d'email via SMTP (Gmail, Mailtrap, etc.)
 * Nécessite une configuration SMTP valide dans application.properties
 */
@Service
@ConditionalOnProperty(name = "app.email.mode", havingValue = "smtp")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailServiceInterface {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String toEmail, String name, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SmartMedical - Code de vérification admin");
            message.setText(
                "Bonjour " + name + ",\n\n" +
                "Vous avez été ajouté comme administrateur sur SmartMedical.\n\n" +
                "Voici votre code de vérification : " + code + "\n\n" +
                "Ce code est valable pendant 24 heures.\n\n" +
                "Pour activer votre compte, veuillez saisir ce code dans l'interface de vérification.\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet email.\n\n" +
                "Cordialement,\n" +
                "L'équipe SmartMedical"
            );

            mailSender.send(message);
            log.info("📧 Email de vérification envoyé à : {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email à {} : {}", toEmail, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email de vérification. " +
                "Vérifiez votre configuration SMTP ou passez en mode 'console'.", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SmartMedical - Bienvenue administrateur");
            message.setText(
                "Bonjour " + name + ",\n\n" +
                "Votre compte administrateur a été activé avec succès !\n\n" +
                "Vous pouvez maintenant vous connecter à l'interface d'administration :\n" +
                "http://localhost:4200/admin\n\n" +
                "Cordialement,\n" +
                "L'équipe SmartMedical"
            );

            mailSender.send(message);
            log.info("📧 Email de bienvenue envoyé à : {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de bienvenue : {}", e.getMessage());
        }
    }

    @Override
    public void sendDoctorWelcomeEmail(String toEmail, String name, String code, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SmartMedical - Bienvenue Dr. " + name);
            message.setText(
                "Bonjour Dr. " + name + ",\n\n" +
                "Votre compte médecin a été créé avec succès sur SmartMedical !\n\n" +
                "🔑 Vos informations de connexion :\n" +
                "Email: " + toEmail + "\n" +
                "Mot de passe temporaire: " + tempPassword + "\n\n" +
                "🎯 Code de vérification: " + code + "\n\n" +
                "Instructions :\n" +
                "1. Connectez-vous avec votre email et le mot de passe temporaire\n" +
                "2. Vérifiez votre compte avec le code de vérification\n" +
                "3. Changez votre mot de passe\n\n" +
                "URL de connexion : http://localhost:4200/login\n\n" +
                "Cordialement,\n" +
                "L'équipe SmartMedical"
            );

            mailSender.send(message);
            log.info("📧 Email de bienvenue médecin envoyé à : {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de bienvenue médecin : {}", e.getMessage());
        }
    }
}
