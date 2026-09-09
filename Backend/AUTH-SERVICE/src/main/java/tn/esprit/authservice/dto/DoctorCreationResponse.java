package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreationResponse {

    private DoctorResponse doctor;
    private String tempPassword;
    private String verificationCode;
    private boolean emailSent;
    private String emailDeliveryMessage;
}
