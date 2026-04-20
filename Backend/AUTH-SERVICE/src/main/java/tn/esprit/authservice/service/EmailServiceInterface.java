package tn.esprit.authservice.service;

/**
 * Interface pour les services d'email
 */
public interface EmailServiceInterface {

    /**
     * Envoie un code de vérification par email
     * @param toEmail Email du destinataire
     * @param name Nom du destinataire
     * @param code Code de vérification à 6 chiffres
     */
    void sendVerificationCode(String toEmail, String name, String code);

    /**
     * Envoie un email de bienvenue après vérification
     * @param toEmail Email du destinataire
     * @param name Nom du destinataire
     */
    void sendWelcomeEmail(String toEmail, String name);

    /**
     * Envoie un email de bienvenue pour les docteurs
     * @param toEmail Email du destinataire
     * @param name Nom du destinataire
     * @param code Code de vérification à 6 chiffres
     * @param tempPassword Mot de passe temporaire
     */
    void sendDoctorWelcomeEmail(String toEmail, String name, String code, String tempPassword);
}
