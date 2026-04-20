package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.dto.DoctorRequest;
import tn.esprit.authservice.dto.DoctorResponse;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.DoctorRepository;
import tn.esprit.authservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceInterface emailService;

    // Créer un nouveau médecin avec compte utilisateur
    public DoctorResponse createDoctor(DoctorRequest request) {
        log.info("Création d'un nouveau médecin: {} {}", request.getFirstName(), request.getLastName());

        // Vérifier si l'email existe déjà
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un médecin avec cet email existe déjà");
        }

        // Vérifier si le numéro de licence existe déjà
        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new RuntimeException("Un médecin avec ce numéro de licence existe déjà");
        }

        // Créer le compte utilisateur associé
        String tempPassword = (request.getPassword() != null && !request.getPassword().isEmpty()) 
                ? request.getPassword() 
                : generateTempPassword();
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role("DOCTOR")
                .name(request.getFirstName() + " " + request.getLastName())
                .verificationCode(generateVerificationCode())
                .isVerified(false)
                .verificationCodeExpiresAt(LocalDateTime.now().plusHours(24))
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
                .phone(request.getPhone())
                .address(request.getAddress())
                .specialization(request.getSpecialization())
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Médecin créé avec succès: {}", savedDoctor.getId());

        // Envoyer email de confirmation avec code de vérification
        emailService.sendDoctorWelcomeEmail(
                request.getEmail(),
                request.getFirstName() + " " + request.getLastName(),
                savedUser.getVerificationCode(),
                tempPassword
        );

        return mapToResponse(savedDoctor);
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

    // Générer un mot de passe temporaire
    private String generateTempPassword() {
        return "Doc" + (int) (Math.random() * 900000 + 100000) + "!";
    }

    // Générer un code de vérification
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 900000 + 100000));
    }
}
