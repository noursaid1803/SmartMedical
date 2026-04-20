package tn.esprit.scanservice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.scanservice.Repository.ScanRepository;
import tn.esprit.scanservice.entity.Scan;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final ScanRepository scanRepository;

    // 🔥 CREATE SCAN (FAKE AI)
    public Scan createScan(Scan scan) {

        // 🔥 FAKE RESULT (jusqu'à AI)
        scan.setResult(Math.random() > 0.5 ? "NORMAL" : "CANCER");

        scan.setDate(LocalDateTime.now().toString());

        return scanRepository.save(scan);
    }

    // GET ALL SCANS
    public List<Scan> getAllScans() {
        return scanRepository.findAll();
    }

    // GET SCAN BY PATIENT
    public List<Scan> getByPatient(String patientId) {
        return scanRepository.findByPatientId(patientId);
    }

    // DELETE
    public void delete(String id) {
        scanRepository.deleteById(id);
    }
}