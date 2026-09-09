package tn.esprit.authservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    private String id;

    private String patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;

    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;

    private String appointmentDate; // yyyy-MM-dd
    private String appointmentTime; // HH:mm
    private String reason;

    @Builder.Default
    private String status = "PENDING"; // PENDING, CONFIRMED, CANCELLED, COMPLETED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
