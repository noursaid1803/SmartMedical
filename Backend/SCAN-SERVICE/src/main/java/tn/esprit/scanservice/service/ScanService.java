package tn.esprit.scanservice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tn.esprit.scanservice.Repository.ScanRepository;
import tn.esprit.scanservice.entity.Scan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final ScanRepository scanRepository;
    private final RestTemplate restTemplate;

    // URL to Auth-Service for Doctor API
    private final String DOCTOR_API_URL = "http://localhost:8081/doctors/";

    // 🔥 CREATE SCAN (FAKE AI)
    public Scan createScan(Scan scan) {
        // Validation: Verify doctor's specialty matches the scan type
        validateScanWithDoctorSpecialty(scan);

        // 🔥 FAKE RESULT (jusqu'à AI)
        scan.setResult(Math.random() > 0.5 ? "NORMAL" : "CANCER");
        scan.setDate(LocalDateTime.now().toString());

        return scanRepository.save(scan);
    }

    private void validateScanWithDoctorSpecialty(Scan scan) {
        if (scan.getDoctorId() == null || scan.getScanType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor ID and Scan Type are required.");
        }

        try {
            // Fetch Doctor details from Auth-Service
            Map<String, Object> doctorResponse = restTemplate.getForObject(DOCTOR_API_URL + scan.getDoctorId(), Map.class);
            if (doctorResponse == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.");
            }

            String specialization = (String) doctorResponse.get("specialization");
            if (specialization == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor has no defined specialization.");
            }

            // Simple validation logic
            String scanType = scan.getScanType().toUpperCase();
            specialization = specialization.toUpperCase();

            boolean isValid = false;

            if (scanType.contains("LUNG") || scanType.contains("POUMON")) {
                if (specialization.contains("PNEUMO") || specialization.contains("ONCOLOGY") || specialization.contains("ONCOLOGUE")) {
                    isValid = true;
                }
            } else if (scanType.contains("BRAIN") || scanType.contains("CERVEAU")) {
                if (specialization.contains("NEURO") || specialization.contains("ONCOLOGY") || specialization.contains("ONCOLOGUE")) {
                    isValid = true;
                }
            } else if (scanType.contains("HEART") || scanType.contains("COEUR")) {
                if (specialization.contains("CARDIO")) {
                    isValid = true;
                }
            } else if (scanType.contains("BONE") || scanType.contains("OS")) {
                if (specialization.contains("ORTHO") || specialization.contains("RHEUMATO")) {
                    isValid = true;
                }
            } else {
                // If it's a general scan, we might allow it or just be strict
                // For demonstration, let's allow general Radiology
                if (specialization.contains("RADIO")) {
                    isValid = true;
                }
            }

            // Allow if doctor is a generalist or radiologist
            if (specialization.contains("GENERAL") || specialization.contains("RADIO")) {
                isValid = true;
            }

            if (!isValid) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Validation failed: A doctor with specialization '" + specialization + 
                    "' is not authorized to upload a scan of type '" + scan.getScanType() + "'.");
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error validating doctor specialty: " + e.getMessage());
        }
    }

    // GET ALL SCANS
    public List<Scan> getAllScans() {
        return scanRepository.findAll();
    }

    // GET SCAN BY PATIENT
    public List<Scan> getByPatient(String patientId) {
        return scanRepository.findByPatientId(patientId);
    }

    // GET SCAN BY DOCTOR AND SPECIALTY
    public List<Scan> getByDoctorAndSpecialty(String doctorId, String specialtyCode) {
        return scanRepository.findByDoctorIdAndSpecialtyCode(doctorId, specialtyCode);
    }

    // DELETE
    public void delete(String id) {
        scanRepository.deleteById(id);
    }
}