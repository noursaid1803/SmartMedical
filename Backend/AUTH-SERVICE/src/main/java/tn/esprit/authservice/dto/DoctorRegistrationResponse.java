package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorRegistrationResponse {
    private String doctorId;
    private String email;
    private String tempPassword; // Affiché une seule fois à l'admin
    private String verificationCode; // Affiché une seule fois à l'admin
    private String message;
    private boolean success;
}
