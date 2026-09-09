package tn.esprit.authservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.authservice.entity.Appointment;

import java.util.List;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    List<Appointment> findByPatientEmailOrderByCreatedAtDesc(String patientEmail);

    List<Appointment> findByDoctorIdOrderByCreatedAtDesc(String doctorId);

    List<Appointment> findAllByOrderByCreatedAtDesc();
}
