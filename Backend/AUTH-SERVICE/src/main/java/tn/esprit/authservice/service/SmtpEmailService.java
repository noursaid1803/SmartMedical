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
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("!'${spring.mail.password:}'.isBlank()")
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
        if (!sendDoctorWelcomeEmailInternal(toEmail, name, code, tempPassword)) {
            throw new RuntimeException("Échec de l'envoi de l'email médecin");
        }
    }

    private boolean sendDoctorWelcomeEmailInternal(String toEmail, String name, String code, String tempPassword) {
        log.info("📨 Tentative d'envoi email médecin à: {} (from: {})", toEmail, fromEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SmartMedical - Confirmation de votre compte médecin");
            message.setText(
                "Bonjour Dr. " + name + ",\n\n" +
                "Votre compte médecin a été créé par l'administrateur SmartMedical.\n\n" +
                "============================================\n" +
                "VOS INFORMATIONS DE CONNEXION\n" +
                "============================================\n" +
                "Adresse e-mail : " + toEmail + "\n" +
                "Mot de passe temporaire : " + tempPassword + "\n" +
                "Code de vérification : " + code + "\n" +
                "============================================\n\n" +
                "IMPORTANT : Le mot de passe temporaire est valable 24 heures.\n" +
                "Vous devez vous connecter et définir un nouveau mot de passe avant expiration.\n\n" +
                "ÉTAPES À SUIVRE :\n" +
                "1. Rendez-vous sur http://localhost:4200/login\n" +
                "2. Connectez-vous avec votre e-mail et le mot de passe temporaire ci-dessus\n" +
                "3. Saisissez le code de vérification : " + code + "\n" +
                "4. Choisissez un mot de passe définitif (obligatoire dans les 24 heures)\n\n" +
                "Si le mot de passe temporaire expire, contactez l'administrateur pour recevoir de nouveaux identifiants.\n\n" +
                "Cordialement,\n" +
                "L'équipe SmartMedical"
            );

            mailSender.send(message);
            log.info("✅ Email de bienvenue médecin envoyé avec succès à : {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("❌ ERREUR SMTP lors de l'envoi à {} : {}", toEmail, e.getMessage());
            log.error("   Détails de l'erreur : ", e);
            // Fallback console pour ne pas perdre les informations
            log.warn("=================================================================");
            log.warn("⚠️  FALLBACK CONSOLE - EMAIL NON ENVOYÉ - Informations médecin :");
            log.warn("=================================================================");
            log.warn("📧 À: {}", toEmail);
            log.warn("👤 Nom: Dr. {}", name);
            log.warn("🔑 Mot de passe temporaire: {}", tempPassword);
            log.warn("🎯 Code de vérification: {}", code);
            log.warn("🌐 URL connexion: http://localhost:4200/login");
            log.warn("=================================================================");

            // Fallback fichier pour l'utilisateur
            try {
                java.io.File file = new java.io.File("/tmp/email_credentials_fallback.txt");
                java.io.FileWriter writer = new java.io.FileWriter(file, true);
                writer.write(String.format(
                    "\n=================================================================\n" +
                    "⚠️  FALLBACK EMAIL MEDECIN - EMAIL NON ENVOYE A %s\n" +
                    "Nom: Dr. %s\n" +
                    "Mot de passe temporaire: %s\n" +
                    "Code de verification: %s\n" +
                    "Date: %s\n" +
                    "=================================================================\n",
                    toEmail, name, tempPassword, code, java.time.LocalDateTime.now()
                ));
                writer.close();
            } catch (Exception fe) {
                log.error("Impossible d'écrire le fichier de fallback : {}", fe.getMessage());
            }
            return false;
        }
    }

    @Override
    public void sendPatientWelcomeEmail(String toEmail, String name, String code, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SmartMedical - Bienvenue " + name);
            message.setText(
                "Bonjour " + name + ",\n\n" +
                "Votre compte patient a été créé avec succès sur SmartMedical !\n\n" +
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
            log.info("📧 Email de bienvenue patient envoyé à : {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de bienvenue patient : {}", e.getMessage());
            // Fallback fichier pour l'utilisateur
            try {
                java.io.File file = new java.io.File("/tmp/email_credentials_fallback.txt");
                java.io.FileWriter writer = new java.io.FileWriter(file, true);
                writer.write(String.format(
                    "\n=================================================================\n" +
                    "⚠️  FALLBACK EMAIL PATIENT - EMAIL NON ENVOYE A %s\n" +
                    "Nom: %s\n" +
                    "Mot de passe temporaire: %s\n" +
                    "Code de verification: %s\n" +
                    "Date: %s\n" +
                    "=================================================================\n",
                    toEmail, name, tempPassword, code, java.time.LocalDateTime.now()
                ));
                writer.close();
            } catch (Exception fe) {
                log.error("Impossible d'écrire le fichier de fallback : {}", fe.getMessage());
            }
        }
    }

    @Override
    public void sendPasswordChangedConfirmation(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SmartMedical - Mot de passe changé");
            message.setText(
                "Bonjour " + name + ",\n\n" +
                "Votre mot de passe a été changé avec succès !\n\n" +
                "Vous pouvez maintenant vous connecter avec votre nouveau mot de passe :\n" +
                "http://localhost:4200/login\n\n" +
                "Si vous n'êtes pas à l'origine de ce changement, veuillez contacter le support immédiatement.\n\n" +
                "Cordialement,\n" +
                "L'équipe SmartMedical"
            );

            mailSender.send(message);
            log.info("📧 Email de confirmation changement mot de passe envoyé à : {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de confirmation : {}", e.getMessage());
        }
    }
}
