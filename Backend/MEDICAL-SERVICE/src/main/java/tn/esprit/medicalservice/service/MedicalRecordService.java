package tn.esprit.medicalservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medicalservice.entity.MedicalRecord;
import tn.esprit.medicalservice.repository.MedicalRecordRepository;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository repository;

    public MedicalRecord create(MedicalRecord record) {
        normalizeClinicalNotes(record);
        record.setCreatedAt(new Date());
        record.setUpdatedAt(new Date());
        return repository.save(record);
    }

    public MedicalRecord getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found"));
    }

    public MedicalRecord update(String id, MedicalRecord incoming) {
        MedicalRecord existing = getById(id);
        mergeRecord(existing, incoming);
        existing.setUpdatedAt(new Date());
        return repository.save(existing);
    }

    public List<MedicalRecord> getAll() {
        return repository.findAll();
    }

    public List<MedicalRecord> getByPatient(String patientId) {
        return repository.findByPatientId(patientId);
    }

    public List<MedicalRecord> getByDoctor(String doctorId) {
        return repository.findByDoctorId(doctorId);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    private void mergeRecord(MedicalRecord target, MedicalRecord source) {
        if (source.getPatientId() != null) target.setPatientId(source.getPatientId());
        if (source.getDoctorId() != null) target.setDoctorId(source.getDoctorId());
        if (source.getDoctorName() != null) target.setDoctorName(source.getDoctorName());
        if (source.getConsultationDate() != null) target.setConsultationDate(source.getConsultationDate());
        if (source.getReasonForVisit() != null) target.setReasonForVisit(source.getReasonForVisit());
        if (source.getSymptoms() != null) target.setSymptoms(source.getSymptoms());
        if (source.getBloodPressure() != null) target.setBloodPressure(source.getBloodPressure());
        if (source.getHeartRate() != null) target.setHeartRate(source.getHeartRate());
        if (source.getTemperature() != null) target.setTemperature(source.getTemperature());
        if (source.getWeight() != null) target.setWeight(source.getWeight());
        if (source.getHeight() != null) target.setHeight(source.getHeight());
        if (source.getOxygenSaturation() != null) target.setOxygenSaturation(source.getOxygenSaturation());
        if (source.getDiagnosis() != null) target.setDiagnosis(source.getDiagnosis());
        if (source.getSeverity() != null) target.setSeverity(source.getSeverity());
        if (source.getMedications() != null) target.setMedications(source.getMedications());
        if (source.getLabTests() != null) target.setLabTests(source.getLabTests());
        if (source.getRecommendations() != null) target.setRecommendations(source.getRecommendations());
        if (source.getFollowUpRequired() != null) target.setFollowUpRequired(source.getFollowUpRequired());
        if (source.getFollowUpDate() != null) target.setFollowUpDate(source.getFollowUpDate());
        if (source.getImageUrl() != null) target.setImageUrl(source.getImageUrl());
        if (source.getAiResult() != null) target.setAiResult(source.getAiResult());

        if (source.getClinicalNotes() != null) {
            target.setClinicalNotes(source.getClinicalNotes());
            target.setNotes(source.getClinicalNotes());
        } else if (source.getNotes() != null) {
            target.setNotes(source.getNotes());
            target.setClinicalNotes(source.getNotes());
        }
    }

    private void normalizeClinicalNotes(MedicalRecord record) {
        if (record.getClinicalNotes() != null && record.getNotes() == null) {
            record.setNotes(record.getClinicalNotes());
        } else if (record.getNotes() != null && record.getClinicalNotes() == null) {
            record.setClinicalNotes(record.getNotes());
        }
    }
}
