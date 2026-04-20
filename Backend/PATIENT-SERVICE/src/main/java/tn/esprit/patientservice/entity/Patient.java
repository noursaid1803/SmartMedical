package tn.esprit.patientservice.entity;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Patient {

    @Id
    private String id;

    private String firstName;
    private String lastName;

    private int age;
    private String gender;

    private String address;
    private String phone;

    // 🏥 infos médicales
    private String medicalHistory; // ex: diabète, hypertension
    private String allergies;
    private String currentMedications;
}