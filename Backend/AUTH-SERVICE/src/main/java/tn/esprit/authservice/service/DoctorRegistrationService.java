package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.dto.DoctorRegistrationResponse;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.repository.DoctorRepository;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorRegistrationService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Créer un nouveau médecin avec mot de passe temporaire et code de vérification
     */
    public DoctorRegistrationResponse registerDoctor(Doctor doctorRequest) {
        // Vérifier si l'email existe déjà
        if (doctorRepository.existsByEmail(doctorRequest.getEmail())) {
            return DoctorRegistrationResponse.builder()
                    .success(false)
                    .message("Un médecin avec cet email existe déjà")
                    .build();
        }

        // Générer mot de passe temporaire (8 caractères)
        String tempPassword = generateTempPassword();
        
        // Générer code de vérification (6 chiffres)
        String verificationCode = generateVerificationCode();

        // Créer le médecin
        Doctor doctor = Doctor.builder()
                .firstName(doctorRequest.getFirstName())
                .lastName(doctorRequest.getLastName())
                .email(doctorRequest.getEmail())
                .phone(doctorRequest.getPhone())
                .address(doctorRequest.getAddress())
                .specialization(doctorRequest.getSpecialization())
                .licenseNumber(doctorRequest.getLicenseNumber())
                .education(doctorRequest.getEducation())
                .experience(doctorRequest.getExperience())
                .hospital(doctorRequest.getHospital())
                .department(doctorRequest.getDepartment())
                .consultationFee(doctorRequest.getConsultationFee())
                .consultationDuration(doctorRequest.getConsultationDuration())
                .availableDays(doctorRequest.getAvailableDays())
                .startTime(doctorRequest.getStartTime())
                .endTime(doctorRequest.getEndTime())
                .bio(doctorRequest.getBio())
                .languages(doctorRequest.getLanguages())
                .active(true)
                .isVerified(false)
                .isFirstLogin(true)
                .password(passwordEncoder.encode(tempPassword)) // Mot de passe temporaire hashé
                .tempPassword(tempPassword) // Stocké en clair pour référence (à supprimer après vérification)
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(LocalDateTime.now().plusHours(24)) // Valide 24h
                .tempPasswordExpiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);

        // Envoyer l'email
        emailService.sendDoctorWelcomeEmail(
                doctor.getEmail(),
                doctor.getFirstName() + " " + doctor.getLastName(),
                tempPassword,
                verificationCode
        );

        return DoctorRegistrationResponse.builder()
                .doctorId(savedDoctor.getId())
                .email(savedDoctor.getEmail())
                .tempPassword(tempPassword) // Affiché une seule fois à l'admin
                .verificationCode(verificationCode) // Affiché une seule fois à l'admin
                .success(true)
                .message("Médecin créé avec succès. Email envoyé avec les identifiants.")
                .build();
    }

    /**
     * Vérifier le code et activer le compte
     */
    public boolean verifyDoctorAccount(String email, String verificationCode) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));

        // Vérifier si déjà vérifié
        if (doctor.isVerified()) {
            throw new RuntimeException("Compte déjà vérifié");
        }

        // Vérifier le code
        if (!verificationCode.equals(doctor.getVerificationCode())) {
            throw new RuntimeException("Code de vérification invalide");
        }

        // Vérifier expiration
        if (doctor.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Code de vérification expiré");
        }

        // Activer le compte
        doctor.setVerified(true);
        doctor.setUpdatedAt(LocalDateTime.now());
        doctorRepository.save(doctor);

        return true;
    }

    /**
     * Changer le mot de passe à la première connexion
     */
    public boolean changePassword(String email, String tempPassword, String newPassword) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));

        // Vérifier que c'est la première connexion
        if (!doctor.isFirstLogin()) {
            throw new RuntimeException("Mot de passe déjà changé. Utilisez la connexion normale.");
        }

        // Vérifier le mot de passe temporaire
        if (!passwordEncoder.matches(tempPassword, doctor.getPassword())) {
            throw new RuntimeException("Mot de passe temporaire invalide");
        }

        // Vérifier expiration
        if (doctor.getTempPasswordExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mot de passe temporaire expiré. Contactez l'administrateur.");
        }

        // Vérifier que le compte est vérifié
        if (!doctor.isVerified()) {
            throw new RuntimeException("Veuillez d'abord vérifier votre compte avec le code de vérification");
        }

        // Changer le mot de passe
        doctor.setPassword(passwordEncoder.encode(newPassword));
        doctor.setFirstLogin(false);
        doctor.setTempPassword(null); // Supprimer le temp password
        doctor.setUpdatedAt(LocalDateTime.now());
        doctorRepository.save(doctor);

        return true;
    }

    /**
     * Login pour médecin - vérifie si première connexion
     */
    public Doctor loginDoctor(String email, String password) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));

        if (!passwordEncoder.matches(password, doctor.getPassword())) {
            throw new RuntimeException("Mot de passe invalide");
        }

        return doctor;
    }

    /**
     * Vérifier si le médecin doit changer son mot de passe
     */
    public boolean needsPasswordChange(String email) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));
        return doctor.isFirstLogin();
    }

    /**
     * Lister tous les médecins
     */
    public java.util.List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    private String generateTempPassword() {
        // Générer un mot de passe aléatoire de 8 caractères
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateVerificationCode() {
        // Générer un code à 6 chiffres
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
