package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.dto.PatientRegistrationRequest;
import tn.esprit.authservice.dto.PatientRegistrationResponse;
import tn.esprit.authservice.entity.Patient;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.PatientRepository;
import tn.esprit.authservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceInterface emailService;

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

    // Enregistrer un patient avec email de confirmation
    public PatientRegistrationResponse registerPatient(PatientRegistrationRequest request) {
        log.info("Registration d'un nouveau patient: {} {}", request.getFirstName(), request.getLastName());

        // Vérifier si l'email existe déjà dans patients ou users
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un patient avec cet email existe déjà");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        // Générer mot de passe temporaire et code de vérification
        String tempPassword = generateTempPassword();
        String verificationCode = generateVerificationCode();

        // Créer le compte utilisateur associé
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role("PATIENT")
                .name(request.getFirstName() + " " + request.getLastName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .verificationCode(verificationCode)
                .isVerified(false)
                .verificationCodeExpiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Compte utilisateur créé pour le patient: {}", savedUser.getEmail());

        // Créer le profil patient
        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .age(request.getAge())
                .gender(request.getGender())
                .address(request.getAddress())
                .city(request.getCity())
                .region(request.getRegion())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .bloodType(request.getBloodType())
                .height(request.getHeight())
                .weight(request.getWeight())
                .allergies(request.getAllergies())
                .chronicDiseases(request.getChronicDiseases())
                .currentMedications(request.getCurrentMedications())
                .occupation(request.getOccupation())
                .employer(request.getEmployer())
                .emergencyContact(Patient.EmergencyContact.builder()
                        .name(request.getEmergencyContactName())
                        .phone(request.getEmergencyContactPhone())
                        .relation(request.getEmergencyContactRelation())
                        .email(request.getEmergencyContactEmail())
                        .build())
                .insuranceInfo(Patient.InsuranceInfo.builder()
                        .provider(request.getInsuranceProvider())
                        .policyNumber(request.getInsurancePolicyNumber())
                        .coverageType(request.getInsuranceCoverageType())
                        .build())
                .isVerified(false)
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(LocalDateTime.now().plusHours(24))
                .tempPassword(tempPassword)
                .tempPasswordExpiresAt(LocalDateTime.now().plusHours(24))
                .isFirstLogin(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastVisitDate(LocalDateTime.now())
                .build();

        Patient savedPatient = patientRepository.save(patient);
        log.info("Patient créé avec succès: {}", savedPatient.getId());

        // Envoyer email de bienvenue avec code de vérification et mot de passe temporaire
        emailService.sendPatientWelcomeEmail(
                request.getEmail(),
                request.getFirstName() + " " + request.getLastName(),
                verificationCode,
                tempPassword
        );

        return PatientRegistrationResponse.builder()
                .id(savedPatient.getId())
                .firstName(savedPatient.getFirstName())
                .lastName(savedPatient.getLastName())
                .email(savedPatient.getEmail())
                .isVerified(false)
                .needsVerification(true)
                .message("Patient créé avec succès. Un email de confirmation a été envoyé avec le code de vérification et le mot de passe temporaire.")
                .build();
    }

    // Vérifier le code du patient
    public Patient verifyPatientCode(String email, String code) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Patient non trouvé"));

        if (patient.isVerified()) {
            throw new RuntimeException("Ce compte est déjà vérifié");
        }

        if (patient.getVerificationCodeExpiresAt() == null || 
            patient.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Le code de vérification a expiré");
        }

        if (!patient.getVerificationCode().equals(code)) {
            throw new RuntimeException("Code de vérification invalide");
        }

        // Activer le compte
        patient.setVerified(true);
        patient.setVerificationCode(null);
        patient.setVerificationCodeExpiresAt(null);
        patient.setUpdatedAt(LocalDateTime.now());

        Patient saved = patientRepository.save(patient);
        log.info("Compte patient vérifié: {}", saved.getEmail());

        // Envoyer email de confirmation
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getFirstName() + " " + saved.getLastName());

        return saved;
    }

    // Changer le mot de passe du patient (première connexion)
    public Patient changePatientPassword(String email, String tempPassword, String newPassword) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Patient non trouvé"));

        if (!patient.isFirstLogin()) {
            throw new RuntimeException("Ce n'est pas la première connexion");
        }

        // Vérifier que le mot de passe temporaire n'a pas expiré
        if (patient.getTempPasswordExpiresAt() != null && 
            patient.getTempPasswordExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Le mot de passe temporaire a expiré");
        }

        // Vérifier le mot de passe temporaire
        if (!tempPassword.equals(patient.getTempPassword())) {
            throw new RuntimeException("Mot de passe temporaire incorrect");
        }

        // Mettre à jour le mot de passe dans User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mettre à jour le patient
        patient.setFirstLogin(false);
        patient.setTempPassword(null);
        patient.setTempPasswordExpiresAt(null);
        patient.setUpdatedAt(LocalDateTime.now());

        Patient saved = patientRepository.save(patient);
        log.info("Mot de passe changé pour le patient: {}", email);

        // Envoyer email de confirmation
        emailService.sendPasswordChangedConfirmation(saved.getEmail(), saved.getFirstName() + " " + saved.getLastName());

        return saved;
    }

    // Générer un mot de passe temporaire
    private String generateTempPassword() {
        return "Pat" + (int) (Math.random() * 900000 + 100000) + "!";
    }

    // Générer un code de vérification
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
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
