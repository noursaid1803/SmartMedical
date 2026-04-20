package tn.esprit.scanservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.scanservice.entity.Scan;
import tn.esprit.scanservice.service.ScanService;

import java.util.List;

@RestController
@RequestMapping("/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    // CREATE SCAN
    @PostMapping
    public Scan create(@RequestBody Scan scan) {
        return scanService.createScan(scan);
    }

    // GET ALL
    @GetMapping
    public List<Scan> getAll() {
        return scanService.getAllScans();
    }

    // GET BY PATIENT
    @GetMapping("/patient/{id}")
    public List<Scan> getByPatient(@PathVariable String id) {
        return scanService.getByPatient(id);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        scanService.delete(id);
    }
}
