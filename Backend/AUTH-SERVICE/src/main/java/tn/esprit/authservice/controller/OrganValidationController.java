package tn.esprit.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.authservice.dto.OrganValidationResponse;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.repository.DoctorRepository;
import tn.esprit.authservice.service.DoctorSpecialtyValidationService;
import tn.esprit.authservice.service.OrganValidationService;
import tn.esprit.authservice.util.JwtUtil;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/auth/organ-validation")
public class OrganValidationController {

    private static final Set<String> VALID_ORGANS = Set.of(
            "cerveau", "sein", "peau", "oeil", "poumon", "foie", "coeur"
    );

    private final OrganValidationService organValidationService;
    private final DoctorSpecialtyValidationService doctorSpecialtyValidationService;
    private final DoctorRepository doctorRepository;
    private final JwtUtil jwtUtil;

    public OrganValidationController(
            OrganValidationService organValidationService,
            DoctorSpecialtyValidationService doctorSpecialtyValidationService,
            DoctorRepository doctorRepository,
            JwtUtil jwtUtil
    ) {
        this.organValidationService = organValidationService;
        this.doctorSpecialtyValidationService = doctorSpecialtyValidationService;
        this.doctorRepository = doctorRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/validate")
    public ResponseEntity<OrganValidationResponse> validateOrgan(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam("expected_organ") String expectedOrgan
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    OrganValidationResponse.builder()
                            .success(false)
                            .valid(false)
                            .reason("Fichier vide")
                            .build()
            );
        }

        if (!VALID_ORGANS.contains(expectedOrgan)) {
            return ResponseEntity.badRequest().body(
                    OrganValidationResponse.builder()
                            .success(false)
                            .valid(false)
                            .reason("Organe attendu invalide: " + expectedOrgan)
                            .build()
            );
        }

        Optional<Doctor> doctor = resolveDoctorFromToken(authHeader);
        if (doctor.isPresent() && !doctorSpecialtyValidationService.isOrganAllowed(doctor.get(), expectedOrgan)) {
            return ResponseEntity.status(403).body(
                    OrganValidationResponse.builder()
                            .success(false)
                            .valid(false)
                            .expectedOrgan(expectedOrgan)
                            .reason(doctorSpecialtyValidationService.buildRestrictionMessage(doctor.get()))
                            .build()
            );
        }

        try {
            OrganValidationResponse response = organValidationService.validate(file, expectedOrgan);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(
                    OrganValidationResponse.builder()
                            .success(false)
                            .valid(false)
                            .reason("Erreur de validation: " + ex.getMessage())
                            .build()
            );
        }
    }

    private Optional<Doctor> resolveDoctorFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            String email = jwtUtil.extractEmail(authHeader.substring(7));
            return doctorRepository.findByEmail(email);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
