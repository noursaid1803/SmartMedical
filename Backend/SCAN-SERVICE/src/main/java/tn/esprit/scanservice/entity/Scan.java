package tn.esprit.scanservice.entity;



import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "scans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scan {

    @Id
    private String id;

    private String patientId;
    private String imageUrl; // chemin image

    private String result; // NORMAL / CANCER

    private String doctorId;

    private String date;
    
    // Type of scan for validation (e.g., LUNG_MRI, BRAIN_MRI, XRAY)
    private String scanType;
    
    // Specialty code for validation (e.g., CERVEAU, SEIN, PEAU, OEIL, ALZHEIMER, POUMON)
    private String specialtyCode;
    
    // Validation results
    private boolean isValidated;
    private String validationMessage;
    private String detectedOrgan;
    private Double confidence;
}
