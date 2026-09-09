package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.entity.Specialty;
import tn.esprit.authservice.service.SpecialtyService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/specialties")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @PostMapping("/initialize")
    public ResponseEntity<?> initializeSpecialties() {
        specialtyService.initializeSpecialties();
        return ResponseEntity.ok().body(Map.of(
            "success", true,
            "message", "Medical specialties initialized successfully"
        ));
    }

    @GetMapping
    public ResponseEntity<List<Specialty>> getAllSpecialties() {
        return ResponseEntity.ok(specialtyService.getAllSpecialties());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Specialty>> getActiveSpecialties() {
        return ResponseEntity.ok(specialtyService.getActiveSpecialties());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Specialty> getSpecialtyByCode(@PathVariable String code) {
        return ResponseEntity.ok(specialtyService.getSpecialtyByCode(code));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Specialty> getSpecialtyById(@PathVariable String id) {
        return ResponseEntity.ok(specialtyService.getSpecialtyById(id));
    }
}
