package tn.esprit.authservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patients")
public class Patient {

    @Id
    private String id;

    // Informations de base
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String dateOfBirth;
    private Integer age;
    private String gender; // M, F, OTHER

    // Adresse
    private String address;
    private String city;
    private String region;
    private String postalCode;
    private String country;

    // Informations médicales
    private String bloodType;
    private Double height; // cm
    private Double weight; // kg
    private List<String> allergies;
    private List<String> chronicDiseases;
    private List<String> currentMedications;
    
    // Vérification et première connexion
    @Builder.Default
    private boolean isVerified = false;
    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;
    private String tempPassword; // Mot de passe temporaire envoyé par email
    @Builder.Default
    private boolean isFirstLogin = true; // true = doit changer son mot de passe
    private LocalDateTime tempPasswordExpiresAt;

    // Profession
    private String occupation;
    private String employer;

    // Contact d'urgence
    private EmergencyContact emergencyContact;

    // Assurance
    private InsuranceInfo insuranceInfo;

    // Historique
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastVisitDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyContact {
        private String name;
        private String phone;
        private String relation;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceInfo {
        private String provider;
        private String policyNumber;
        private String coverageType;
    }
}
