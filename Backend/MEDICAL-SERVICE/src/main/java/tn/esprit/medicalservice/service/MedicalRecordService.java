package tn.esprit.medicalservice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.medicalservice.entity.MedicalRecord;
import tn.esprit.medicalservice.repository.MedicalRecordRepository;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository repository;

    // CREATE RECORD
    public MedicalRecord create(MedicalRecord record) {
        record.setCreatedAt(new Date());
        return repository.save(record);
    }

    // GET ALL
    public List<MedicalRecord> getAll() {
        return repository.findAll();
    }

    // GET BY PATIENT
    public List<MedicalRecord> getByPatient(String patientId) {
        return repository.findByPatientId(patientId);
    }

    // GET BY DOCTOR
    public List<MedicalRecord> getByDoctor(String doctorId) {
        return repository.findByDoctorId(doctorId);
    }

    // DELETE
    public void delete(String id) {
        repository.deleteById(id);
    }
}
