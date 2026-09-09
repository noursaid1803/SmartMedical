package tn.esprit.medicalservice.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medicalservice.entity.MedicalRecord;
import tn.esprit.medicalservice.service.MedicalRecordService;

import java.util.List;

@RestController
@RequestMapping("/medical")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService service;

    // CREATE
    @PostMapping
    public MedicalRecord create(@RequestBody MedicalRecord record) {
        return service.create(record);
    }

    // GET ALL
    @GetMapping
    public List<MedicalRecord> getAll() {
        return service.getAll();
    }

    // GET BY PATIENT
    @GetMapping("/patient/{patientId}")
    public List<MedicalRecord> getByPatient(@PathVariable String patientId) {
        return service.getByPatient(patientId);
    }

    // GET BY DOCTOR
    @GetMapping("/doctor/{doctorId}")
    public List<MedicalRecord> getByDoctor(@PathVariable String doctorId) {
        return service.getByDoctor(doctorId);
    }

    // GET BY ID
    @GetMapping("/{id}")
    public MedicalRecord getById(@PathVariable String id) {
        return service.getById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public MedicalRecord update(@PathVariable String id, @RequestBody MedicalRecord record) {
        return service.update(id, record);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        service.delete(id);
        return "Record deleted";
    }
}
