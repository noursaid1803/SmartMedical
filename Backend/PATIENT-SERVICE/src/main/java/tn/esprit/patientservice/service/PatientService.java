package tn.esprit.patientservice.service;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.patientservice.entity.Patient;
import tn.esprit.patientservice.repository.PatientRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    // ✅ CREATE
    public Patient create(Patient patient) {
        return patientRepository.save(patient);
    }

    // ✅ GET ALL
    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    // ✅ GET BY ID
    public Patient getById(String id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    // ✅ UPDATE
    public Patient update(String id, Patient newPatient) {
        Patient patient = getById(id);

        patient.setFirstName(newPatient.getFirstName());
        patient.setLastName(newPatient.getLastName());
        patient.setAge(newPatient.getAge());
        patient.setGender(newPatient.getGender());
        patient.setAddress(newPatient.getAddress());
        patient.setPhone(newPatient.getPhone());
        patient.setMedicalHistory(newPatient.getMedicalHistory());
        patient.setAllergies(newPatient.getAllergies());
        patient.setCurrentMedications(newPatient.getCurrentMedications());

        return patientRepository.save(patient);
    }

    // ✅ DELETE
    public void delete(String id) {
        patientRepository.deleteById(id);
    }
}