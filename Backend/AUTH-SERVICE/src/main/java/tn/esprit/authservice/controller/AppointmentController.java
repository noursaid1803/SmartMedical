package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.dto.AppointmentRequest;
import tn.esprit.authservice.entity.Appointment;
import tn.esprit.authservice.service.AppointmentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AppointmentRequest request) {
        try {
            Appointment appointment = appointmentService.create(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rendez-vous enregistré avec succès");
            response.put("appointment", appointment);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public List<Appointment> getAll() {
        return appointmentService.getAll();
    }

    @GetMapping("/patient/{email}")
    public List<Appointment> getByPatient(@PathVariable String email) {
        return appointmentService.getByPatientEmail(email);
    }

    @GetMapping("/doctor/{doctorId}")
    public List<Appointment> getByDoctor(@PathVariable String doctorId) {
        return appointmentService.getByDoctorId(doctorId);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            Appointment updated = appointmentService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        appointmentService.delete(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Rendez-vous supprimé"));
    }
}
