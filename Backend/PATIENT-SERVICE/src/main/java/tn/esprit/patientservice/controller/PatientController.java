package tn.esprit.patientservice.controller;



import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.patientservice.entity.Patient;
import tn.esprit.patientservice.service.PatientService;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    // CREATE
    @PostMapping
    public Patient create(@RequestBody Patient patient) {
        return patientService.create(patient);
    }

    // GET ALL
    @GetMapping
    public List<Patient> getAll() {
        return patientService.getAll();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public Patient getById(@PathVariable String id) {
        return patientService.getById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Patient update(@PathVariable String id, @RequestBody Patient patient) {
        return patientService.update(id, patient);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        patientService.delete(id);
        return "Patient deleted";
    }
}
