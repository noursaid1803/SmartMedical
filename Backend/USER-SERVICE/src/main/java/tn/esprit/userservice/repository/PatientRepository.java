package tn.esprit.userservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.userservice.entity.Patient;

public interface PatientRepository extends MongoRepository<Patient, String> {
}