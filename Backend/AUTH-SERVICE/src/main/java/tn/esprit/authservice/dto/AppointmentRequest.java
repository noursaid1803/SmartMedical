package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {
    private String patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private String doctorId;
    private String appointmentDate;
    private String appointmentTime;
    private String reason;
}
