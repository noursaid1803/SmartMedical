package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganValidationResponse {
    private boolean success;
    private boolean valid;
    private String organ;
    private String organName;
    private double confidence;
    private String expectedOrgan;
    private String expectedOrganName;
    private String reason;
    private List<Map<String, Object>> alternativeOrgans;
    private Map<String, Double> allScores;
    private String method;
}
