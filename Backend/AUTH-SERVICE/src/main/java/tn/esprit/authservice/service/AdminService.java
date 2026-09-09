package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.dto.CreateAdminRequest;
import tn.esprit.authservice.dto.VerifyCodeRequest;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.repository.DoctorRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceInterface emailService;
    private final DoctorRepository doctorRepository;

    // Générer un code à 6 chiffres
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }

    /**
     * Admin principal pré-vérifié (démarrage application).
     * Pas d'email de vérification requis.
     */
    public User createBootstrapAdmin(CreateAdminRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        User admin = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .isVerified(true)
                .verificationCode(null)
                .verificationCodeExpiresAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(admin);
        log.info("Admin bootstrap créé (pré-vérifié) : {}", saved.getEmail());
        return saved;
    }

    public java.util.List<User> getAllAdmins() {
        return userRepository.findByRole("ADMIN");
    }

    // Créer un compte admin et envoyer le code de vérification
    public User createAdmin(CreateAdminRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        // Générer le code de vérification
        String verificationCode = generateVerificationCode();

        // Créer l'utilisateur
        User admin = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .verificationCode(verificationCode)
                .isVerified(false)
                .verificationCodeExpiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Sauvegarder
        User saved = userRepository.save(admin);
        log.info("Admin créé : {}", saved.getEmail());

        // Envoyer l'email avec le code de vérification
        emailService.sendVerificationCode(
                request.getEmail(),
                request.getName(),
                verificationCode
        );

        return saved;
    }

    // Vérifier le code et activer le compte
    public User verifyCode(VerifyCodeRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier si déjà vérifié
        if (user.isVerified()) {
            throw new RuntimeException("Ce compte est déjà vérifié");
        }

        // Vérifier si le code est expiré
        if (user.getVerificationCodeExpiresAt() == null) {
            throw new RuntimeException("Le code de vérification n'a pas été généré correctement. Veuillez demander un nouveau code.");
        }
        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Le code de vérification a expiré. Veuillez demander un nouveau code.");
        }

        // Vérifier le code
        if (!user.getVerificationCode().equals(request.getCode())) {
            throw new RuntimeException("Code de vérification invalide");
        }

        // Activer le compte
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("Compte utilisateur vérifié : {}", saved.getEmail());

        // Si c'est un médecin, activer également son profil médecin
        if ("DOCTOR".equalsIgnoreCase(saved.getRole())) {
            doctorRepository.findByEmail(saved.getEmail()).ifPresent(doctor -> {
                doctor.setVerified(true);
                doctor.setVerificationCode(null);
                doctor.setVerificationCodeExpiresAt(null);
                doctor.setUpdatedAt(LocalDateTime.now());
                doctorRepository.save(doctor);
                log.info("Profil médecin vérifié : {}", doctor.getEmail());
            });
        }

        // Email de bienvenue (admin uniquement — le médecin change son mot de passe ensuite)
        if (!"DOCTOR".equalsIgnoreCase(saved.getRole())) {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        }

        return saved;
    }

    // Renvoyer un code de vérification
    public User resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.isVerified()) {
            throw new RuntimeException("Ce compte est déjà vérifié");
        }

        // Générer nouveau code
        String newCode = generateVerificationCode();
        user.setVerificationCode(newCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(24));
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        if ("DOCTOR".equalsIgnoreCase(saved.getRole())) {
            doctorRepository.findByEmail(email).ifPresent(doctor -> {
                doctor.setVerificationCode(newCode);
                doctor.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(24));
                doctor.setUpdatedAt(LocalDateTime.now());
                doctorRepository.save(doctor);
            });
        }

        // Envoyer l'email
        emailService.sendVerificationCode(user.getEmail(), user.getName(), newCode);
        log.info("Nouveau code envoyé à : {}", email);

        return saved;
    }

    // Changer le mot de passe
    public User changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que le compte est vérifié
        if (!user.isVerified()) {
            throw new RuntimeException("Le compte doit être vérifié avant de changer le mot de passe");
        }

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        if ("DOCTOR".equalsIgnoreCase(user.getRole())) {
            Doctor doctor = doctorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Profil médecin non trouvé"));
            if (doctor.isFirstLogin()
                    && doctor.getTempPasswordExpiresAt() != null
                    && doctor.getTempPasswordExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException(
                        "Votre mot de passe temporaire a expiré (validité : 24 heures). "
                                + "Veuillez contacter l'administrateur pour recevoir de nouveaux identifiants."
                );
            }
        }

        // Encoder et sauvegarder le nouveau mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("Mot de passe changé pour : {}", email);

        // Si c'est un médecin, mettre à jour également son profil médecin (première connexion terminée)
        if ("DOCTOR".equalsIgnoreCase(saved.getRole())) {
            doctorRepository.findByEmail(saved.getEmail()).ifPresent(doctor -> {
                doctor.setPassword(passwordEncoder.encode(newPassword));
                doctor.setFirstLogin(false);
                doctor.setTempPassword(null);
                doctor.setTempPasswordExpiresAt(null);
                doctor.setUpdatedAt(LocalDateTime.now());
                doctorRepository.save(doctor);
                log.info("Profil médecin mis à jour avec le nouveau mot de passe : {}", doctor.getEmail());
                emailService.sendPasswordChangedConfirmation(doctor.getEmail(),
                        doctor.getFirstName() + " " + doctor.getLastName());
            });
        }

        return saved;
    }
}
