package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.dto.LoginResponse;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.DoctorRepository;
import tn.esprit.authservice.service.AuthService;
import tn.esprit.authservice.service.EmailService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return authService.register(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Object loggedEntity = authService.loginWithUserOrDoctor(user);
        
        // Si c'est un médecin, vérifier le statut
        if (loggedEntity instanceof Doctor) {
            Doctor doctor = (Doctor) loggedEntity;
            
            // Si le médecin doit encore vérifier son compte
            if (!doctor.isVerified()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "needsVerification", true,
                    "needsPasswordChange", false,
                    "email", doctor.getEmail(),
                    "message", "Veuillez vérifier votre compte avec le code de vérification envoyé par email"
                ));
            }
            
            // Si c'est la première connexion (doit changer le mot de passe)
            if (doctor.isFirstLogin()) {
                // Générer un token temporaire pour le changement de mot de passe
                String tempToken = authService.generateToken(doctor.getEmail());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "needsVerification", false,
                    "needsPasswordChange", true,
                    "email", doctor.getEmail(),
                    "tempToken", tempToken,
                    "message", "Veuillez changer votre mot de passe pour accéder à l'interface"
                ));
            }
            
            // Médecin vérifié et avec mot de passe changé - connexion normale
            String token = authService.generateToken(doctor.getEmail());
            return ResponseEntity.ok(LoginResponse.builder()
                    .token(token)
                    .message("Login successful")
                    .success(true)
                    .user(LoginResponse.UserInfo.builder()
                            .email(doctor.getEmail())
                            .firstName(doctor.getFirstName())
                            .lastName(doctor.getLastName())
                            .roles(new String[]{"DOCTOR"})
                            .verified(true)
                            .needsVerification(false)
                            .needsPasswordChange(false)
                            .build())
                    .build());
        }
        
        // C'est un User (admin, etc.)
        User loggedUser = (User) loggedEntity;
        String token = authService.generateToken(loggedUser.getEmail());
        
        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .message("Login successful")
                .success(true)
                .user(LoginResponse.UserInfo.builder()
                        .email(loggedUser.getEmail())
                        .firstName(loggedUser.getFirstName())
                        .lastName(loggedUser.getLastName())
                        .roles(new String[]{loggedUser.getRole()})
                        .verified(loggedUser.isVerified())
                        .needsVerification(false)
                        .needsPasswordChange(false)
                        .build())
                .build());
    }

    // Créer un admin par défaut si aucun utilisateur n'existe
    @PostMapping("/setup-admin")
    public String setupAdmin() {
        try {
            User admin = new User();
            admin.setName("Administrateur SmartMedical");
            admin.setEmail("arijhedhri4@gmail.com");
            admin.setPassword("admin123");
            admin.setFirstName("Admin");
            admin.setLastName("SmartMedical");
            admin.setRole("ADMIN");
            admin.setVerified(true);
            authService.register(admin);
            return "Admin créé avec succès : arijhedhri4@gmail.com / admin123";
        } catch (Exception e) {
            return "Erreur ou admin existe déjà : " + e.getMessage();
        }
    }

    // Setup mot de passe pour médecin (première connexion)
    @PostMapping("/setup-doctor-password")
    public ResponseEntity<?> setupDoctorPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body("Email et password requis");
        }
        
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findByEmail(email);
            if (!doctorOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Médecin non trouvé");
            }
            
            Doctor doctor = doctorOpt.get();
            doctor.setPassword(passwordEncoder.encode(password));
            doctorRepository.save(doctor);
            
            return ResponseEntity.ok("Mot de passe créé avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }
}
