package tn.esprit.authservice.service;

import org.springframework.beans.factory.ObjectProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.dto.DoctorCreationResponse;
import tn.esprit.authservice.dto.DoctorRequest;
import tn.esprit.authservice.dto.DoctorResponse;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.DoctorRepository;
import tn.esprit.authservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceInterface emailService;
    private final ObjectProvider<SmtpEmailService> smtpEmailService;

    // Créer un nouveau médecin avec compte utilisateur
    public DoctorCreationResponse createDoctor(DoctorRequest request) {
        log.info("Création d'un nouveau médecin: {} {}", request.getFirstName(), request.getLastName());

        // Vérifier si l'email existe déjà
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un médecin avec cet email existe déjà");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        // Vérifier si le numéro de licence existe déjà
        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new RuntimeException("Un médecin avec ce numéro de licence existe déjà");
        }

        // Mot de passe temporaire généré automatiquement (envoyé par email, valable 24h)
        String tempPassword = generateTempPassword();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role("DOCTOR")
                .name(request.getFirstName() + " " + request.getLastName())
                .verificationCode(generateVerificationCode())
                .isVerified(false)
                .verificationCodeExpiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Compte utilisateur créé pour le médecin: {}", savedUser.getEmail());

        // Créer le profil médecin
        Doctor doctor = Doctor.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .phone(request.getPhone())
                .address(request.getAddress())
                .specialization(request.getSpecialization())
                .specialtyCode(request.getSpecialtyCode())
                .licenseNumber(request.getLicenseNumber())
                .education(request.getEducation())
                .experience(request.getExperience())
                .hospital(request.getHospital())
                .department(request.getDepartment())
                .consultationFee(request.getConsultationFee())
                .consultationDuration(request.getConsultationDuration())
                .availableDays(request.getAvailableDays())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .bio(request.getBio())
                .languages(request.getLanguages())
                .userId(savedUser.getId())
                .active(true)
                .isVerified(false)
                .verificationCode(savedUser.getVerificationCode())
                .verificationCodeExpiresAt(expiresAt)
                .tempPassword(tempPassword)
                .tempPasswordExpiresAt(expiresAt)
                .isFirstLogin(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Médecin créé avec succès: {}", savedDoctor.getId());

        // Envoyer email de confirmation avec code de vérification
        String verificationCode = savedUser.getVerificationCode();
        boolean emailSent;
        if (smtpEmailService.getIfAvailable() != null) {
            emailSent = emailService.sendDoctorWelcomeEmailSafe(
                    request.getEmail(),
                    request.getFirstName() + " " + request.getLastName(),
                    verificationCode,
                    tempPassword
            );
        } else {
            emailService.sendDoctorWelcomeEmail(
                    request.getEmail(),
                    request.getFirstName() + " " + request.getLastName(),
                    verificationCode,
                    tempPassword
            );
            emailSent = false;
        }

        String emailMessage = emailSent
                ? "Un email avec le code de vérification et le mot de passe temporaire a été envoyé au médecin."
                : "L'email n'a pas pu être envoyé. Communiquez manuellement le code et le mot de passe temporaire au médecin (affichés ci-dessous).";

        return DoctorCreationResponse.builder()
                .doctor(mapToResponse(savedDoctor))
                .tempPassword(tempPassword)
                .verificationCode(verificationCode)
                .emailSent(emailSent)
                .emailDeliveryMessage(emailMessage)
                .build();
    }

    /**
     * Renvoyer les identifiants (mot de passe temporaire) à un médecin dont l'inscription n'est pas finalisée.
     */
    public void resendDoctorCredentials(String email) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Compte utilisateur non trouvé"));

        if (!doctor.isFirstLogin()) {
            throw new RuntimeException("Ce médecin a déjà finalisé son inscription");
        }

        String tempPassword = generateTempPassword();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        String verificationCode = doctor.isVerified() ? null : generateVerificationCode();

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setUpdatedAt(LocalDateTime.now());

        if (!doctor.isVerified()) {
            user.setVerificationCode(verificationCode);
            user.setVerificationCodeExpiresAt(expiresAt);
            user.setVerified(false);
            doctor.setVerificationCode(verificationCode);
            doctor.setVerificationCodeExpiresAt(expiresAt);
            doctor.setVerified(false);
        }

        doctor.setTempPassword(tempPassword);
        doctor.setTempPasswordExpiresAt(expiresAt);
        doctor.setPassword(passwordEncoder.encode(tempPassword));
        doctor.setFirstLogin(true);
        doctor.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        doctorRepository.save(doctor);

        String codeForEmail = verificationCode != null ? verificationCode : doctor.getVerificationCode();
        emailService.sendDoctorWelcomeEmail(
                email,
                doctor.getFirstName() + " " + doctor.getLastName(),
                codeForEmail != null ? codeForEmail : "",
                tempPassword
        );
        log.info("Identifiants renvoyés au médecin : {}", email);
    }

    // Récupérer tous les médecins
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Récupérer les médecins actifs
    public List<DoctorResponse> getActiveDoctors() {
        return doctorRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Récupérer un médecin par ID
    public DoctorResponse getDoctorById(String id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));
        return mapToResponse(doctor);
    }

    // Récupérer un médecin par email
    public DoctorResponse getDoctorByEmail(String email) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));
        return mapToResponse(doctor);
    }

    // Rechercher par spécialisation
    public List<DoctorResponse> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecializationIgnoreCase(specialization)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Rechercher par hôpital
    public List<DoctorResponse> getDoctorsByHospital(String hospital) {
        return doctorRepository.findByHospitalIgnoreCase(hospital)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Mettre à jour un médecin
    public DoctorResponse updateDoctor(String id, DoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));

        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setPhone(request.getPhone());
        doctor.setAddress(request.getAddress());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setEducation(request.getEducation());
        doctor.setExperience(request.getExperience());
        doctor.setHospital(request.getHospital());
        doctor.setDepartment(request.getDepartment());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setConsultationDuration(request.getConsultationDuration());
        doctor.setAvailableDays(request.getAvailableDays());
        doctor.setStartTime(request.getStartTime());
        doctor.setEndTime(request.getEndTime());
        doctor.setBio(request.getBio());
        doctor.setLanguages(request.getLanguages());
        doctor.setUpdatedAt(LocalDateTime.now());

        Doctor updated = doctorRepository.save(doctor);
        log.info("Médecin mis à jour: {}", updated.getId());

        return mapToResponse(updated);
    }

    // Activer/Désactiver un médecin
    public DoctorResponse toggleDoctorStatus(String id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));

        doctor.setActive(!doctor.isActive());
        doctor.setUpdatedAt(LocalDateTime.now());

        Doctor updated = doctorRepository.save(doctor);
        log.info("Statut du médecin {} changé à: {}", updated.getId(), updated.isActive());

        return mapToResponse(updated);
    }

    // Supprimer un médecin
    public void deleteDoctor(String id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));

        // Supprimer aussi le compte utilisateur associé
        if (doctor.getUserId() != null) {
            userRepository.deleteById(doctor.getUserId());
        }

        doctorRepository.deleteById(id);
        log.info("Médecin supprimé: {}", id);
    }

    // Mapper Doctor vers DoctorResponse
    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .email(doctor.getEmail())
                .phone(doctor.getPhone())
                .address(doctor.getAddress())
                .specialization(doctor.getSpecialization())
                .specialtyCode(doctor.getSpecialtyCode())
                .licenseNumber(doctor.getLicenseNumber())
                .education(doctor.getEducation())
                .experience(doctor.getExperience())
                .hospital(doctor.getHospital())
                .department(doctor.getDepartment())
                .consultationFee(doctor.getConsultationFee())
                .consultationDuration(doctor.getConsultationDuration())
                .availableDays(doctor.getAvailableDays())
                .startTime(doctor.getStartTime())
                .endTime(doctor.getEndTime())
                .bio(doctor.getBio())
                .languages(doctor.getLanguages())
                .profileImage(doctor.getProfileImage())
                .active(doctor.isActive())
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .build();
    }

    // Générer un mot de passe temporaire sécurisé
    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
        Random random = new Random();
        StringBuilder sb = new StringBuilder("Doc");
        for (int i = 0; i < 9; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Générer un code de vérification
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 900000 + 100000));
    }
}
