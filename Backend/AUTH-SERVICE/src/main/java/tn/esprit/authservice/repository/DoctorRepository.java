package tn.esprit.authservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.authservice.entity.Doctor;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends MongoRepository<Doctor, String> {

    Optional<Doctor> findByEmail(String email);

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    List<Doctor> findByActiveTrue();

    List<Doctor> findByHospitalIgnoreCase(String hospital);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);
}
