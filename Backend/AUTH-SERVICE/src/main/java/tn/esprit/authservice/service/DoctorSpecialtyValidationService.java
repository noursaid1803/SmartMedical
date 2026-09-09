package tn.esprit.authservice.service;

import org.springframework.stereotype.Service;
import tn.esprit.authservice.entity.Doctor;

import java.util.*;

@Service
public class DoctorSpecialtyValidationService {

    private static final Map<String, Set<String>> SPECIALTY_ORGANS = new LinkedHashMap<>();

    private static final Map<String, String> SPECIALTY_ALIASES = new HashMap<>();

    static {
        SPECIALTY_ORGANS.put("Pneumologie", Set.of("poumon"));
        SPECIALTY_ORGANS.put("Oncologie", Set.of("poumon"));
        SPECIALTY_ORGANS.put("Neurologie", Set.of("cerveau"));
        SPECIALTY_ORGANS.put("Cardiologie", Set.of("coeur"));
        SPECIALTY_ORGANS.put("Dermatologie", Set.of("peau"));
        SPECIALTY_ORGANS.put("Ophtalmologie", Set.of("oeil"));
        SPECIALTY_ORGANS.put("Gynécologie", Set.of("sein"));
        SPECIALTY_ORGANS.put("Gastroentérologie", Set.of("foie"));
        SPECIALTY_ORGANS.put("Radiologie", Set.of("cerveau", "sein", "peau", "oeil", "poumon", "foie", "coeur"));
        SPECIALTY_ORGANS.put("Médecine générale", Set.of());

        SPECIALTY_ALIASES.put("POUMON", "Pneumologie");
        SPECIALTY_ALIASES.put("PNEUMOLOGIE", "Pneumologie");
        SPECIALTY_ALIASES.put("ONCOLOGIE", "Oncologie");
        SPECIALTY_ALIASES.put("CERVEAU", "Neurologie");
        SPECIALTY_ALIASES.put("ALZHEIMER", "Neurologie");
        SPECIALTY_ALIASES.put("NEUROLOGIE", "Neurologie");
        SPECIALTY_ALIASES.put("SEIN", "Gynécologie");
        SPECIALTY_ALIASES.put("GYNECOLOGIE", "Gynécologie");
        SPECIALTY_ALIASES.put("GYNÉCOLOGIE", "Gynécologie");
        SPECIALTY_ALIASES.put("PEAU", "Dermatologie");
        SPECIALTY_ALIASES.put("DERMATOLOGIE", "Dermatologie");
        SPECIALTY_ALIASES.put("OEIL", "Ophtalmologie");
        SPECIALTY_ALIASES.put("OPHTALMOLOGIE", "Ophtalmologie");
        SPECIALTY_ALIASES.put("COEUR", "Cardiologie");
        SPECIALTY_ALIASES.put("CARDIOLOGIE", "Cardiologie");
        SPECIALTY_ALIASES.put("FOIE", "Gastroentérologie");
        SPECIALTY_ALIASES.put("GASTROENTEROLOGIE", "Gastroentérologie");
        SPECIALTY_ALIASES.put("GASTROENTÉROLOGIE", "Gastroentérologie");
        SPECIALTY_ALIASES.put("RADIOLOGIE", "Radiologie");
        SPECIALTY_ALIASES.put("MEDECINE_GENERALE", "Médecine générale");
        SPECIALTY_ALIASES.put("Poumon / Nodules", "Pneumologie");
        SPECIALTY_ALIASES.put("Cerveau (2D/3D Tumeurs)", "Neurologie");
        SPECIALTY_ALIASES.put("Alzheimer (IRM)", "Neurologie");
        SPECIALTY_ALIASES.put("Sein / Mammographie", "Gynécologie");
        SPECIALTY_ALIASES.put("Lésions cutanées", "Dermatologie");
        SPECIALTY_ALIASES.put("Rétinopathie diabétique", "Ophtalmologie");
        SPECIALTY_ALIASES.put("Cardiaque (IRM)", "Cardiologie");
        SPECIALTY_ALIASES.put("Foie / Segmentation", "Gastroentérologie");
    }

    public String normalizeSpecialty(Doctor doctor) {
        if (doctor == null) {
            return "";
        }
        String code = doctor.getSpecialtyCode();
        if (code != null && SPECIALTY_ALIASES.containsKey(code.trim())) {
            return SPECIALTY_ALIASES.get(code.trim());
        }
        String specialization = doctor.getSpecialization();
        if (specialization == null || specialization.isBlank()) {
            return "";
        }
        String trimmed = specialization.trim();
        if (SPECIALTY_ALIASES.containsKey(trimmed)) {
            return SPECIALTY_ALIASES.get(trimmed);
        }
        for (String key : SPECIALTY_ORGANS.keySet()) {
            if (key.equalsIgnoreCase(trimmed)) {
                return key;
            }
        }
        return trimmed;
    }

    public boolean isOrganAllowed(Doctor doctor, String expectedOrgan) {
        if (doctor == null || expectedOrgan == null) {
            return true;
        }
        String specialty = normalizeSpecialty(doctor);
        if (specialty.isBlank() || !SPECIALTY_ORGANS.containsKey(specialty)) {
            return false;
        }
        return SPECIALTY_ORGANS.get(specialty).contains(expectedOrgan);
    }

    public String buildRestrictionMessage(Doctor doctor) {
        String specialty = normalizeSpecialty(doctor);
        Set<String> organs = SPECIALTY_ORGANS.getOrDefault(specialty, Set.of());
        if (organs.isEmpty()) {
            return "Votre spécialité (" + specialty + ") ne permet pas l'import d'images scanner/IRM.";
        }
        return "Votre spécialité (" + specialty + ") permet uniquement l'analyse de : "
                + String.join(", ", organs) + ".";
    }
}
