package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequest {

    // Informations personnelles
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;

    // Informations professionnelles
    private String specialization;
    private String specialtyCode; // Code de la spécialité (CERVEAU, SEIN, etc.)
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

    // Mot de passe pour créer le compte utilisateur
    private String password;
}
