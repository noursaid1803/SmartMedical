import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Patient, MedicalRecordService } from '../../../core/services/medical-record.service';

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
      error: (error) => {
        console.error('Erreur chargement patients:', error);
        // Données de test si l'API n'est pas prête
        this.patients = [
          { id: '1', firstName: 'Mohamed', lastName: 'Ali', email: 'mohamed@test.com', phone: '+216 20 111 222', dateOfBirth: '1985-03-15', gender: 'M', bloodType: 'A+', emergencyContact: { name: 'Ali Mohamed', phone: '+216 20 999 888', relation: 'Parent' } },
          { id: '2', firstName: 'Fatima', lastName: 'Hassan', email: 'fatima@test.com', phone: '+216 20 333 444', dateOfBirth: '1990-07-22', gender: 'F', bloodType: 'O-', emergencyContact: { name: 'Hassan Fatima', phone: '+216 20 777 666', relation: 'Conjoint(e)' } },
          { id: '3', firstName: 'Ahmed', lastName: 'Khalil', email: 'ahmed@test.com', phone: '+216 20 555 666', dateOfBirth: '1978-11-05', gender: 'M', bloodType: 'B+', emergencyContact: { name: 'Khalil Ahmed', phone: '+216 20 444 333', relation: 'Frère/Soeur' } }
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

  deletePatient(patient: Patient, event: Event): void {
    event.stopPropagation(); // Empêcher la sélection du patient
    
    if (!patient.id) return;
    
    const confirmed = confirm(`Êtes-vous sûr de vouloir supprimer le patient ${this.getFullName(patient)} ?`);
    
    if (confirmed) {
      this.medicalRecordService.deletePatient(patient.id).subscribe({
        next: () => {
          this.patients = this.patients.filter(p => p.id !== patient.id);
          this.filteredPatients = [...this.patients];
          alert('Patient supprimé avec succès !');
        },
        error: (error: any) => {
          console.error('Erreur suppression patient:', error);
          alert('Erreur lors de la suppression du patient');
        }
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
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }
}
