package tn.esprit.authservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    private String id;

    // Informations personnelles
    private String firstName;
    private String lastName;
    private String email;
    private String password; // Pour l'authentification
    private String phone;
    private String address;

    // Informations professionnelles
    private String specialization; // Spécialité médicale
    private String licenseNumber; // Numéro de licence médicale
    private String education; // Formation universitaire
    private String experience; // Années d'expérience
    private String hospital; // Hôpital/Clinique affiliée
    private String department; // Département

    // Détails du cabinet
    private String consultationFee; // Frais de consultation
    private String consultationDuration; // Durée consultation (minutes)
    private List<String> availableDays; // Jours disponibles
    private String startTime; // Heure début
    private String endTime; // Heure fin

    // Bio et description
    private String bio; // Biographie
    private String languages; // Langues parlées

    // Photo de profil
    private String profileImage;

    // Compte utilisateur associé
    private String userId;

    // Statut
    private boolean active;
    
    // Vérification et première connexion
    @Builder.Default
    private boolean isVerified = false;
    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;
    private String tempPassword; // Mot de passe temporaire envoyé par email
    @Builder.Default
    private boolean isFirstLogin = true; // true = doit changer son mot de passe
    private LocalDateTime tempPasswordExpiresAt;

    // Timestamps
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
