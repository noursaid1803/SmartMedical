package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.dto.AppointmentRequest;
import tn.esprit.authservice.entity.Appointment;
import tn.esprit.authservice.entity.Doctor;
import tn.esprit.authservice.repository.AppointmentRepository;
import tn.esprit.authservice.repository.DoctorRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public Appointment create(AppointmentRequest request) {
        if (request.getDoctorId() == null || request.getDoctorId().isBlank()) {
            throw new RuntimeException("Médecin requis");
        }
        if (request.getAppointmentDate() == null || request.getAppointmentTime() == null) {
            throw new RuntimeException("Date et heure requises");
        }

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));

        if (!doctor.isActive()) {
            throw new RuntimeException("Ce médecin n'accepte pas de nouveaux rendez-vous");
        }

        Appointment appointment = Appointment.builder()
                .patientId(request.getPatientId())
                .patientName(request.getPatientName())
                .patientEmail(request.getPatientEmail())
                .patientPhone(request.getPatientPhone())
                .doctorId(doctor.getId())
                .doctorName(doctor.getFirstName() + " " + doctor.getLastName())
                .doctorSpecialty(doctor.getSpecialization())
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .reason(request.getReason())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAll() {
        return appointmentRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Appointment> getByPatientEmail(String email) {
        return appointmentRepository.findByPatientEmailOrderByCreatedAtDesc(email);
    }

    public List<Appointment> getByDoctorId(String doctorId) {
        return appointmentRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    public Appointment updateStatus(String id, String status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rendez-vous non trouvé"));
        appointment.setStatus(status);
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointmentRepository.save(appointment);
    }

    public void delete(String id) {
        appointmentRepository.deleteById(id);
    }
}
