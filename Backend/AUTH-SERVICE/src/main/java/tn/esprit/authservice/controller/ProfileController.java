package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.dto.ProfileDTO;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.DoctorRepository;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.service.DoctorSpecialtyValidationService;
import tn.esprit.authservice.util.JwtUtil;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final JwtUtil jwtUtil;
    private final DoctorSpecialtyValidationService doctorSpecialtyValidationService;

    private String extractEmailFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return jwtUtil.extractEmail(token);
    }

    /**
     * Parse le frais de consultation en gérant les formats avec devise.
     * Ex: "80 DT", "80.5", "80", null
     */
    private Double parseConsultationFee(String consultationFee) {
        if (consultationFee == null || consultationFee.trim().isEmpty()) {
            return null;
        }
        try {
            String cleaned = consultationFee.replaceAll("[^\\d.,]", "").trim();
            cleaned = cleaned.replace(",", ".");
            if (cleaned.isEmpty()) {
                return null;
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        String email = extractEmailFromToken(authHeader);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProfileDTO.ProfileDTOBuilder builder = ProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .verified(user.isVerified())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .postalCode(user.getPostalCode())
                .bio(user.getBio())
                .linkedin(user.getLinkedin())
                .website(user.getWebsite());

        if ("DOCTOR".equals(user.getRole())) {
            Doctor doctor = doctorRepository.findByEmail(email).orElse(null);
            if (doctor != null) {
                builder.specialty(doctorSpecialtyValidationService.normalizeSpecialty(doctor))
                        .specialtyCode(doctor.getSpecialtyCode())
                        .licenseNumber(doctor.getLicenseNumber())
                        .consultationFee(parseConsultationFee(doctor.getConsultationFee()));
            }
        }

        return ResponseEntity.ok(builder.build());
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileDTO> updateMyProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ProfileDTO profileDTO) {
        String email = extractEmailFromToken(authHeader);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(profileDTO.getFirstName());
        user.setLastName(profileDTO.getLastName());
        user.setPhone(profileDTO.getPhone());
        user.setAddress(profileDTO.getAddress());
        user.setCity(profileDTO.getCity());
        user.setPostalCode(profileDTO.getPostalCode());
        user.setBio(profileDTO.getBio());
        user.setLinkedin(profileDTO.getLinkedin());
        user.setWebsite(profileDTO.getWebsite());

        userRepository.save(user);

        if ("DOCTOR".equals(user.getRole())) {
            Doctor doctor = doctorRepository.findByEmail(email).orElse(null);
            if (doctor != null) {
                if (profileDTO.getSpecialty() != null && !profileDTO.getSpecialty().isBlank()) {
                    doctor.setSpecialization(profileDTO.getSpecialty());
                }
                doctor.setLicenseNumber(profileDTO.getLicenseNumber());
                if (profileDTO.getConsultationFee() != null) {
                    doctor.setConsultationFee(String.valueOf(profileDTO.getConsultationFee()));
                }
                doctorRepository.save(doctor);
            }
        }

        return getMyProfile(authHeader);
    }
}
