package tn.esprit.medicalservice.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "medical_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedicalRecord {

    @Id
    private String id;

    private String patientId;
    private String doctorId;
    private String doctorName;

    private String consultationDate;
    private String reasonForVisit;
    private String symptoms;

    private String bloodPressure;
    private String heartRate;
    private String temperature;
    private String weight;
    private String height;
    private String oxygenSaturation;

    private String diagnosis;
    private String severity;

    @Builder.Default
    private List<MedicationEntry> medications = new ArrayList<>();

    @Builder.Default
    private List<LabTestEntry> labTests = new ArrayList<>();

    /** Notes cliniques (alias frontend: clinicalNotes) */
    private String notes;

    @JsonAlias("clinicalNotes")
    private String clinicalNotes;

    private String recommendations;
    private Boolean followUpRequired;
    private String followUpDate;

    private String imageUrl;
    private String aiResult;

    private Date createdAt;
    private Date updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MedicationEntry {
        private String name;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LabTestEntry {
        private String testName;
        private String result;
        private String normalRange;
        private String notes;
    }
}
