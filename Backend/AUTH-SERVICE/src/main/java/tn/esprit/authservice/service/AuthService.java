package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.DoctorRepository;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.util.JwtUtil;

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
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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

        // 1. Chercher d'abord dans users
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
            throw new RuntimeException("Invalid password");
        }

        // 2. Chercher ensuite dans doctors
        Optional<Doctor> doctorOpt = doctorRepository.findByEmail(email);
        if (doctorOpt.isPresent()) {
            Doctor doctor = doctorOpt.get();
            // Si le doctor n'a pas de password encodé (ancien compte)
            if (doctor.getPassword() == null || doctor.getPassword().isEmpty()) {
                throw new RuntimeException("Doctor account needs password setup");
            }
            if (passwordEncoder.matches(password, doctor.getPassword())) {
                return doctor;
            }
            throw new RuntimeException("Invalid password");
        }

        throw new RuntimeException("User not found");
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