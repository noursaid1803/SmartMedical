package tn.esprit.authservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String role; // ADMIN, DOCTOR, PATIENT
    
    // Champs de profil
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String bio;
    private String linkedin;
    private String website;

    // Vérification email
    private String verificationCode;

    @Builder.Default
    private boolean isVerified = false;

    private java.time.LocalDateTime verificationCodeExpiresAt;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
