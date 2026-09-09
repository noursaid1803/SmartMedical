package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.DoctorRepository;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // REGISTER
    public User register(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Un compte existe déjà avec cet email");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("PATIENT");
        }
        user.setVerified(true);
        if (user.getName() == null || user.getName().isBlank()) {
            String fn = user.getFirstName() != null ? user.getFirstName() : "";
            String ln = user.getLastName() != null ? user.getLastName() : "";
            user.setName((fn + " " + ln).trim());
        }
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }

    // LOGIN
    public String login(User request) {
        Object user = loginWithUserOrDoctor(request);
        String email = (user instanceof User) ? ((User) user).getEmail() : ((Doctor) user).getEmail();
        return jwtUtil.generateToken(email);
    }

    // LOGIN with User or Doctor return
    public Object loginWithUserOrDoctor(User request) {
        String email = request.getEmail();
        String password = request.getPassword();

        // 1. Médecins en priorité (compte User DOCTOR + profil Doctor)
        Optional<Doctor> doctorOpt = doctorRepository.findByEmail(email);
        if (doctorOpt.isPresent()) {
            Doctor doctor = doctorOpt.get();
            if (doctor.getPassword() == null || doctor.getPassword().isEmpty()) {
                throw new RuntimeException("Compte médecin non configuré. Contactez l'administrateur.");
            }
            if (!passwordEncoder.matches(password, doctor.getPassword())) {
                throw new RuntimeException("Mot de passe incorrect");
            }
            if (doctor.isFirstLogin()) {
                validateTempPasswordNotExpired(doctor);
            }
            return doctor;
        }

        // 2. Autres utilisateurs (admin, patient, etc.)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
            throw new RuntimeException("Mot de passe incorrect");
        }

        throw new RuntimeException("Aucun compte trouvé avec cet email");
    }

    private void validateTempPasswordNotExpired(Doctor doctor) {
        if (doctor.getTempPasswordExpiresAt() != null
                && doctor.getTempPasswordExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Votre mot de passe temporaire a expiré (validité : 24 heures). "
                            + "Veuillez contacter l'administrateur pour recevoir de nouveaux identifiants."
            );
        }
    }

    // LOGIN with User return (compatibilité)
    public User loginWithUser(User request) {
        Object result = loginWithUserOrDoctor(request);
        if (result instanceof User) {
            return (User) result;
        }
        // Si c'est un Doctor, on ne peut pas le retourner comme User
        throw new RuntimeException("Doctor login not supported for this endpoint");
    }

    // Generate token for user
    public String generateToken(String email) {
        return jwtUtil.generateToken(email);
    }
}