package tn.esprit.medicalservice.entity;



import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "medical_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    private String id;

    private String patientId; // 🔗 lien avec patient
    private String doctorId;  // 🔗 lien avec médecin

    private String diagnosis; // résultat
    private String notes;

    private String imageUrl; // radio (scan)

    private String aiResult; // cancer / normal

    private Date createdAt;
}
