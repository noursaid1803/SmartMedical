package tn.esprit.authservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "specialties")
public class Specialty {

    @Id
    private String id;

    // Basic information
    private String code; // e.g., "CERVEAU", "SEIN", "PEAU", "OEIL", "ALZHEIMER", "POUMON"
    private String name; // e.g., "Cerveau (2D/3D Tumeurs)"
    private String icon; // e.g., "🧠", "🎀", "🩺", "🩸", "🧠", "🫁"
    private String description;

    // AI Model information
    private String modelName;
    private String modelArchitecture;
    private String dataset;
    private String referencePaper;
    private String doi;

    // Upload validation rules
    private List<String> acceptedFileTypes; // e.g., ["DICOM", "NIfTI", "PNG", "JPEG"]
    private List<String> acceptedOrgans; // e.g., ["cerveau"] for brain specialty
    private Integer minImageWidth;
    private Integer minImageHeight;
    private Integer maxImageWidth;
    private Integer maxImageHeight;
    private Long maxFileSizeBytes; // Maximum file size in bytes

    // Status
    @Builder.Default
    private boolean active = true;

    // Timestamps
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
