package tn.esprit.authservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.authservice.entity.Patient;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {

    Optional<Patient> findByEmail(String email);

    List<Patient> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    List<Patient> findByPhoneContaining(String phone);

    boolean existsByEmail(String email);

    List<Patient> findByBloodType(String bloodType);

    List<Patient> findByRegion(String region);
}
