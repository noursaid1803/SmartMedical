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
public class PatientRegistrationRequest {
    
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
    
    // Profession
    private String occupation;
    private String employer;
    
    // Contact d'urgence
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String emergencyContactEmail;
    
    // Assurance
    private String insuranceProvider;
    private String insurancePolicyNumber;
    private String insuranceCoverageType;
    
    // Assigné à un médecin (optionnel)
    private String assignedDoctorId;
}
