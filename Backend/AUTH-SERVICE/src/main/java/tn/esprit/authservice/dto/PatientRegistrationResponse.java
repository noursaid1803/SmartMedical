package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRegistrationResponse {
    
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean isVerified;
    private boolean needsVerification;
    private String message;
}
