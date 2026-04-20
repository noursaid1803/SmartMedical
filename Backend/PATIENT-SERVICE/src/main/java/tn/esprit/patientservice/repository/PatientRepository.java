package tn.esprit.patientservice.repository;



import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.patientservice.entity.Patient;

public interface PatientRepository extends MongoRepository<Patient, String> {
}
