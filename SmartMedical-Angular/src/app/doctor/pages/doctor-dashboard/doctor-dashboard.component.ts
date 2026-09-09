import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { DoctorService } from '../../../core/services/doctor.service';
import { PatientService } from '../../../core/services/patient.service';
import { MedicalRecordService, MedicalRecord } from '../../../core/services/medical-record.service';
import { Patient } from '../../../core/models/patient.model';

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './doctor-dashboard.component.html',
  styleUrl: './doctor-dashboard.component.scss'
})
export class DoctorDashboardComponent implements OnInit {
  doctorName: string = '';
  doctorEmail: string = '';
  today: Date = new Date();
  recentPatients: Patient[] = [];
  recentMedicalRecords: MedicalRecord[] = [];

  stats = {
    totalPatients: 0,
    todayAppointments: 0,
    pendingRecords: 0
  };

  quickActions = [
    { label: 'Voir mes patients', icon: '👥', route: '/doctor/patients', color: '#3498db' },
    { label: 'Nouveau dossier', icon: '📝', route: '/doctor/medical-record', color: '#27ae60' },
    { label: 'Rendez-vous', icon: '📅', route: '/doctor/appointments', color: '#f39c12' },
    { label: 'Mon profil', icon: '👤', route: '/profile', color: '#9b59b6' }
  ];

  constructor(
    private authService: AuthService,
    private doctorService: DoctorService,
    private patientService: PatientService,
    private medicalRecordService: MedicalRecordService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.doctorName = user.firstName || user.email?.split('@')[0] || 'Docteur';
      this.doctorEmail = user.email || '';
    }

    // Vérifier que c'est bien un médecin
    if (!this.authService.isDoctor()) {
      this.router.navigate(['/login']);
      return;
    }

    // Charger les patients et les dossiers médicaux
    this.loadRecentPatients();
    this.loadRecentMedicalRecords();
  }

  loadRecentMedicalRecords(): void {
    this.medicalRecordService.getAllRecords().subscribe({
      next: (records) => {
        this.recentMedicalRecords = records
          .sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())
          .slice(0, 5);
        this.stats.pendingRecords = records.length;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des dossiers médicaux:', error);
      }
    });
  }

  loadRecentPatients(): void {
    this.patientService.getAllPatients().subscribe({
      next: (patients) => {
        // Prendre les 5 derniers patients (triés par date de création si disponible)
        this.recentPatients = patients
          .sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())
          .slice(0, 5);
        this.stats.totalPatients = patients.length;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des patients:', error);
      }
    });
  }

  getPatientDisplayName(patient: Patient): string {
    return `${patient.firstName} ${patient.lastName}`.trim() || 'Patient sans nom';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
