package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean verified;
    
    // Contact
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    
    // Bio & Social
    private String bio;
    private String linkedin;
    private String website;
    
    // Spécifique Médecin
    private String specialty;
    private String licenseNumber;
    private Integer yearsExperience;
    private Double consultationFee;
}
