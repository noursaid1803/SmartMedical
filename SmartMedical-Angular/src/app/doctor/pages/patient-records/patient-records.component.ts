import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Patient, MedicalRecordService, MedicalRecord } from '../../../core/services/medical-record.service';

@Component({
  selector: 'app-patient-records',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './patient-records.component.html',
  styleUrl: './patient-records.component.scss'
})
export class PatientRecordsComponent implements OnInit {
  patients: Patient[] = [];
  filteredPatients: Patient[] = [];
  searchQuery = '';
  loading = false;

  // Selected patient & their records
  selectedPatient: Patient | null = null;
  patientRecords: MedicalRecord[] = [];
  filteredRecords: MedicalRecord[] = [];
  recordsLoading = false;
  filterDiagnosis = '';
  filterDate = '';

  constructor(
    private medicalRecordService: MedicalRecordService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.loading = true;
    this.medicalRecordService.getAllPatients().subscribe({
      next: (patients) => {
        this.patients = patients;
        this.filteredPatients = patients;
        this.loading = false;
      },
      error: () => {
        this.patients = [
          { id: '1', firstName: 'Mohamed', lastName: 'Ali', email: 'mohamed@test.com', phone: '+216 20 111 222', dateOfBirth: '1985-03-15', gender: 'M', bloodType: 'A+', emergencyContact: { name: 'Ali Mohamed', phone: '+216 20 999 888', relation: 'Parent' } },
          { id: '2', firstName: 'Fatima', lastName: 'Hassan', email: 'fatima@test.com', phone: '+216 20 333 444', dateOfBirth: '1990-07-22', gender: 'F', bloodType: 'O-', emergencyContact: { name: 'Hassan Ali', phone: '+216 20 777 666', relation: 'Conjoint(e)' } },
          { id: '3', firstName: 'Ahmed', lastName: 'Khalil', email: 'ahmed@test.com', phone: '+216 20 555 666', dateOfBirth: '1978-11-05', gender: 'M', bloodType: 'B+', emergencyContact: { name: 'Khalil Ali', phone: '+216 20 444 333', relation: 'Frère/Soeur' } }
        ];
        this.filteredPatients = this.patients;
        this.loading = false;
      }
    });
  }

  searchPatients(): void {
    const query = this.searchQuery?.toLowerCase() || '';
    if (!query) {
      this.filteredPatients = this.patients;
      return;
    }
    this.filteredPatients = this.patients.filter((p: Patient) =>
      p.firstName?.toLowerCase().includes(query) ||
      p.lastName?.toLowerCase().includes(query) ||
      p.email?.toLowerCase().includes(query) ||
      p.phone?.includes(query)
    );
  }

  selectPatient(patient: Patient): void {
    this.selectedPatient = patient;
    this.filterDiagnosis = '';
    this.filterDate = '';
    if (patient.id) {
      this.loadPatientRecords(patient.id);
    }
  }

  loadPatientRecords(patientId: string): void {
    this.recordsLoading = true;
    this.patientRecords = [];
    this.filteredRecords = [];
    this.medicalRecordService.getRecordsByPatient(patientId).subscribe({
      next: (records) => {
        this.patientRecords = records.sort((a, b) =>
          new Date(b.consultationDate).getTime() - new Date(a.consultationDate).getTime()
        );
        this.filteredRecords = [...this.patientRecords];
        this.recordsLoading = false;
      },
      error: () => {
        // Mock records for demo
        this.patientRecords = [
          {
            id: 'r1', patientId, doctorId: 'doc1', doctorName: 'Dr. Sami Bouraoui',
            consultationDate: '2026-04-20', reasonForVisit: 'Contrôle annuel',
            symptoms: 'Toux légère, fatigue', diagnosis: 'Bronchite légère', severity: 'low',
            medications: [{ name: 'Amoxicilline', dosage: '500mg', frequency: '3x/jour', duration: '7 jours' }],
            recommendations: 'Repos, hydratation.', clinicalNotes: 'RAS à l\'examen clinique.', aiResult: ''
          },
          {
            id: 'r2', patientId, doctorId: 'doc2', doctorName: 'Dr. Leila Bouzid',
            consultationDate: '2025-12-10', reasonForVisit: 'Douleurs dorsales',
            symptoms: 'Douleur lombaire chronique', diagnosis: 'Lombalgie commune', severity: 'medium',
            recommendations: 'Kinésithérapie 10 séances.', clinicalNotes: 'Radio normale.', aiResult: ''
          }
        ];
        this.filteredRecords = [...this.patientRecords];
        this.recordsLoading = false;
      }
    });
  }

  applyRecordFilters(): void {
    this.filteredRecords = this.patientRecords.filter(r => {
      const matchDiag = !this.filterDiagnosis ||
        r.diagnosis?.toLowerCase().includes(this.filterDiagnosis.toLowerCase()) ||
        r.reasonForVisit?.toLowerCase().includes(this.filterDiagnosis.toLowerCase());
      const matchDate = !this.filterDate || r.consultationDate?.startsWith(this.filterDate);
      return matchDiag && matchDate;
    });
  }

  resetRecordFilters(): void {
    this.filterDiagnosis = '';
    this.filterDate = '';
    this.filteredRecords = [...this.patientRecords];
  }

  deletePatient(patient: Patient, event: Event): void {
    event.stopPropagation();
    if (!patient.id) return;
    const confirmed = confirm(`Êtes-vous sûr de vouloir supprimer ${this.getFullName(patient)} ?`);
    if (confirmed) {
      this.medicalRecordService.deletePatient(patient.id).subscribe({
        next: () => {
          this.patients = this.patients.filter(p => p.id !== patient.id);
          this.filteredPatients = [...this.patients];
          if (this.selectedPatient?.id === patient.id) {
            this.selectedPatient = null;
            this.patientRecords = [];
            this.filteredRecords = [];
          }
        },
        error: () => alert('Erreur lors de la suppression.')
      });
    }
  }

  getFullName(patient: Patient): string {
    return `${patient.firstName} ${patient.lastName}`;
  }

  getAge(dateOfBirth?: string): number {
    if (!dateOfBirth) return 0;
    const birth = new Date(dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  }

  getInitials(patient: Patient): string {
    return ((patient.firstName?.[0] || '') + (patient.lastName?.[0] || '')).toUpperCase();
  }

  getSeverityLabel(severity: string): string {
    const map: any = { low: 'Faible', medium: 'Moyen', high: 'Élevé', critical: 'Critique' };
    return map[severity] || severity;
  }

  getSeverityClass(severity: string): string {
    const map: any = { low: 'badge-low', medium: 'badge-medium', high: 'badge-high', critical: 'badge-critical' };
    return map[severity] || '';
  }

  getExamRecords(): MedicalRecord[] {
    return this.patientRecords.filter(r => r.imageUrl);
  }

  getAiResult(record: MedicalRecord): any {
    if (!record.aiResult) return null;
    try { return JSON.parse(record.aiResult); } catch { return { diagnosis: record.aiResult }; }
  }
}
