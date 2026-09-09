package tn.esprit.scanservice.Repository;



import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.scanservice.entity.Scan;


import java.util.List;

public interface ScanRepository extends MongoRepository<Scan, String> {

    List<Scan> findByPatientId(String patientId);
    List<Scan> findByDoctorId(String doctorId);
    List<Scan> findByDoctorIdAndSpecialtyCode(String doctorId, String specialtyCode);

}
