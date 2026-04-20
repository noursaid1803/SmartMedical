package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.entity.Patient;
import tn.esprit.authservice.repository.PatientRepository;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;

    // Créer un nouveau patient
    public Patient createPatient(Patient patient) {
        // Vérifier si l'email existe déjà
        if (patient.getEmail() != null && !patient.getEmail().isEmpty() 
            && patientRepository.existsByEmail(patient.getEmail())) {
            throw new RuntimeException("Un patient avec cet email existe déjà");
        }

        // Calculer l'âge si la date de naissance est fournie
        if (patient.getDateOfBirth() != null && !patient.getDateOfBirth().isEmpty()) {
            try {
                LocalDateTime birthDate = LocalDateTime.parse(patient.getDateOfBirth() + "T00:00:00");
                patient.setAge(Period.between(birthDate.toLocalDate(), LocalDateTime.now().toLocalDate()).getYears());
            } catch (Exception e) {
                log.warn("Impossible de calculer l'âge pour: {}", patient.getDateOfBirth());
            }
        }

        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        patient.setLastVisitDate(LocalDateTime.now());

        Patient saved = patientRepository.save(patient);
        log.info("Patient créé: {} {}", saved.getFirstName(), saved.getLastName());
        return saved;
    }

    // Récupérer tous les patients
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    // Récupérer un patient par ID
    public Patient getPatientById(String id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient non trouvé"));
    }

    // Récupérer un patient par email
    public Optional<Patient> getPatientByEmail(String email) {
        return patientRepository.findByEmail(email);
    }

    // Rechercher des patients
    public List<Patient> searchPatients(String query) {
        return patientRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query);
    }

    // Mettre à jour un patient
    public Patient updatePatient(String id, Patient patientDetails) {
        Patient patient = getPatientById(id);

        // Mettre à jour les champs
        if (patientDetails.getFirstName() != null) patient.setFirstName(patientDetails.getFirstName());
        if (patientDetails.getLastName() != null) patient.setLastName(patientDetails.getLastName());
        if (patientDetails.getEmail() != null) patient.setEmail(patientDetails.getEmail());
        if (patientDetails.getPhone() != null) patient.setPhone(patientDetails.getPhone());
        if (patientDetails.getDateOfBirth() != null) {
            patient.setDateOfBirth(patientDetails.getDateOfBirth());
            // Recalculer l'âge
            try {
                LocalDateTime birthDate = LocalDateTime.parse(patientDetails.getDateOfBirth() + "T00:00:00");
                patient.setAge(Period.between(birthDate.toLocalDate(), LocalDateTime.now().toLocalDate()).getYears());
            } catch (Exception e) {
                log.warn("Impossible de recalculer l'âge");
            }
        }
        if (patientDetails.getGender() != null) patient.setGender(patientDetails.getGender());
        if (patientDetails.getAddress() != null) patient.setAddress(patientDetails.getAddress());
        if (patientDetails.getCity() != null) patient.setCity(patientDetails.getCity());
        if (patientDetails.getRegion() != null) patient.setRegion(patientDetails.getRegion());
        if (patientDetails.getPostalCode() != null) patient.setPostalCode(patientDetails.getPostalCode());
        if (patientDetails.getBloodType() != null) patient.setBloodType(patientDetails.getBloodType());
        if (patientDetails.getHeight() != null) patient.setHeight(patientDetails.getHeight());
        if (patientDetails.getWeight() != null) patient.setWeight(patientDetails.getWeight());
        if (patientDetails.getAllergies() != null) patient.setAllergies(patientDetails.getAllergies());
        if (patientDetails.getChronicDiseases() != null) patient.setChronicDiseases(patientDetails.getChronicDiseases());
        if (patientDetails.getCurrentMedications() != null) patient.setCurrentMedications(patientDetails.getCurrentMedications());
        if (patientDetails.getOccupation() != null) patient.setOccupation(patientDetails.getOccupation());
        if (patientDetails.getEmployer() != null) patient.setEmployer(patientDetails.getEmployer());
        if (patientDetails.getEmergencyContact() != null) patient.setEmergencyContact(patientDetails.getEmergencyContact());
        if (patientDetails.getInsuranceInfo() != null) patient.setInsuranceInfo(patientDetails.getInsuranceInfo());

        patient.setUpdatedAt(LocalDateTime.now());

        Patient saved = patientRepository.save(patient);
        log.info("Patient mis à jour: {}", id);
        return saved;
    }

    // Supprimer un patient
    public void deletePatient(String id) {
        if (!patientRepository.existsById(id)) {
            throw new RuntimeException("Patient non trouvé");
        }
        patientRepository.deleteById(id);
        log.info("Patient supprimé: {}", id);
    }

    // Mettre à jour la date de dernière visite
    public void updateLastVisitDate(String patientId) {
        Patient patient = getPatientById(patientId);
        patient.setLastVisitDate(LocalDateTime.now());
        patientRepository.save(patient);
    }
}
