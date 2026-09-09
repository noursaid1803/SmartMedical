package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.entity.PasswordResetToken;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.DoctorRepository;
import tn.esprit.authservice.repository.PasswordResetTokenRepository;
import tn.esprit.authservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 30;
    
    /**
     * Générer et envoyer un code de réinitialisation
     */
    public boolean requestPasswordReset(String email) {
        // Vérifier si l'email existe (User ou Doctor)
        boolean userExists = userRepository.existsByEmail(email);
        boolean doctorExists = doctorRepository.existsByEmail(email);
        
        if (!userExists && !doctorExists) {
            log.warn("Tentative de réinitialisation pour email inconnu: {}", email);
            return false;
        }
        
        // Générer un code à 6 chiffres
        String code = generateCode();
        
        // Supprimer l'ancien token s'il existe
        tokenRepository.deleteByEmail(email);
        
        // Créer un nouveau token
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .used(false)
                .build();
        
        tokenRepository.save(token);
        
        // Envoyer l'email
        emailService.sendPasswordResetCode(email, code, EXPIRATION_MINUTES);
        
        log.info("Code de réinitialisation envoyé à: {}", email);
        return true;
    }
    
    /**
     * Vérifier le code de réinitialisation
     */
    public boolean verifyCode(String email, String code) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByEmailAndCode(email, code);
        
        if (tokenOpt.isEmpty()) {
            log.warn("Code invalide pour email: {}", email);
            return false;
        }
        
        PasswordResetToken token = tokenOpt.get();
        
        if (token.isUsed()) {
            log.warn("Code déjà utilisé pour email: {}", email);
            return false;
        }
        
        if (token.isExpired()) {
            log.warn("Code expiré pour email: {}", email);
            return false;
        }
        
        return true;
    }
    
    /**
     * Réinitialiser le mot de passe
     */
    public boolean resetPassword(String email, String code, String newPassword) {
        // Vérifier le code
        if (!verifyCode(email, code)) {
            return false;
        }
        
        // Marquer le code comme utilisé
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByEmailAndCode(email, code);
        PasswordResetToken token = tokenOpt.get();
        token.setUsed(true);
        tokenRepository.save(token);
        
        // Mettre à jour le mot de passe
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        // Mettre à jour User
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(encodedPassword);
            userRepository.save(user);
            emailService.sendPasswordChangedConfirmation(email, user.getFirstName() + " " + user.getLastName());
            log.info("Mot de passe réinitialisé pour User: {}", email);
            return true;
        }
        
        // Mettre à jour Doctor
        Optional<Doctor> doctorOpt = doctorRepository.findByEmail(email);
        if (doctorOpt.isPresent()) {
            Doctor doctor = doctorOpt.get();
            doctor.setPassword(encodedPassword);
            doctor.setFirstLogin(false); // Plus besoin de changer le mot de passe
            doctorRepository.save(doctor);
            emailService.sendPasswordChangedConfirmation(email, doctor.getFirstName() + " " + doctor.getLastName());
            log.info("Mot de passe réinitialisé pour Doctor: {}", email);
            return true;
        }
        
        return false;
    }
    
    /**
     * Générer un code aléatoire à 6 chiffres
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 à 999999
        return String.valueOf(code);
    }
}
