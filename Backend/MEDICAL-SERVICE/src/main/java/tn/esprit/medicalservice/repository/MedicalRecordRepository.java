package tn.esprit.medicalservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.medicalservice.entity.MedicalRecord;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface MedicalRecordRepository extends MongoRepository<MedicalRecord, String> {

    List<MedicalRecord> findByPatientId(String patientId);
    
    // 🔗 Trouver les dossiers par médecin
    List<MedicalRecord> findByDoctorId(String doctorId);
    
    // 🔗 Trouver un dossier spécifique pour un patient et un médecin
    Optional<MedicalRecord> findByPatientIdAndDoctorId(String patientId, String doctorId);
    
    // 🔗 Vérifier si un dossier existe déjà pour ce patient et ce médecin
    boolean existsByPatientIdAndDoctorId(String patientId, String doctorId);
}