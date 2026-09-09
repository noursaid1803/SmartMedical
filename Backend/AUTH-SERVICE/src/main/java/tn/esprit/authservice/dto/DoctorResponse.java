package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {

    private String id;

    // Informations personnelles
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;

    // Informations professionnelles
    private String specialization;
    private String specialtyCode;
    private String licenseNumber;
    private String education;
    private String experience;
    private String hospital;
    private String department;

    // Détails du cabinet
    private String consultationFee;
    private String consultationDuration;
    private List<String> availableDays;
    private String startTime;
    private String endTime;

    // Bio et description
    private String bio;
    private String languages;

    // Photo de profil
    private String profileImage;

    // Statut
    private boolean active;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nom complet
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
