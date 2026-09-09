import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { DoctorService } from '../../../core/services/doctor.service';

interface SpecialtyStats {
  name: string;
  count: number;
  percent: number;
  color: string;
  icon: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  today = new Date();

  // Real dynamic stats
  totalPatients = 0;
  totalDoctors = 0;
  totalRecords = 0;
  totalAiAnalyses = 0;
  loadingStats = true;

  specialtyStats: SpecialtyStats[] = [];

  stats: any[] = [];

  recentAppointments = [
    { patient: 'Jean Dupont', doctor: 'Dr. Martin', date: '2024-04-18', time: '09:00', status: 'confirmé' },
    { patient: 'Marie Curie', doctor: 'Dr. Sophie', date: '2024-04-18', time: '10:30', status: 'en attente' },
    { patient: 'Pierre Lefebvre', doctor: 'Dr. Bernard', date: '2024-04-18', time: '14:00', status: 'confirmé' },
    { patient: 'Sophie Martin', doctor: 'Dr. Martin', date: '2024-04-19', time: '09:30', status: 'confirmé' }
  ];

  private specialtyColors: { [key: string]: string } = {
    'Pneumologie': '#0ea5e9',
    'Neurologie': '#8b5cf6',
    'Cardiologie': '#ef4444',
    'Dermatologie': '#f59e0b',
    'Ophtalmologie': '#10b981',
    'Gynécologie': '#ec4899',
    'Pédiatrie': '#3b82f6',
    'Radiologie': '#64748b',
    'Chirurgie': '#f97316',
    'Médecine générale': '#22c55e'
  };

  private specialtyIcons: { [key: string]: string } = {
    'Pneumologie': 'bi-wind',
    'Neurologie': 'bi-brain',
    'Cardiologie': 'bi-heart-pulse',
    'Dermatologie': 'bi-person',
    'Ophtalmologie': 'bi-eye',
    'Gynécologie': 'bi-gender-female',
    'Pédiatrie': 'bi-emoji-smile',
    'Radiologie': 'bi-radioactive',
    'Chirurgie': 'bi-scissors',
    'Médecine générale': 'bi-hospital'
  };

  constructor(
    private medicalRecordService: MedicalRecordService,
    private doctorService: DoctorService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loadingStats = true;

    // Load patients
    this.medicalRecordService.getAllPatients().subscribe({
      next: (patients) => {
        this.totalPatients = patients.length;
        this.updateStatsCards();
      },
      error: () => { this.totalPatients = 0; this.updateStatsCards(); }
    });

    // Load doctors & compute specialty breakdown
    this.doctorService.getAllDoctors().subscribe({
      next: (doctors) => {
        this.totalDoctors = doctors.length;
        this.computeSpecialtyStats(doctors);
        this.updateStatsCards();
      },
      error: () => { this.totalDoctors = 0; this.updateStatsCards(); }
    });

    // Load medical records
    this.medicalRecordService.getAllRecords().subscribe({
      next: (records) => {
        this.totalRecords = records.length;
        this.totalAiAnalyses = records.filter(r => r.aiResult || r.imageUrl).length;
        this.loadingStats = false;
        this.updateStatsCards();
      },
      error: () => {
        this.totalRecords = 0;
        this.totalAiAnalyses = 0;
        this.loadingStats = false;
        this.updateStatsCards();
      }
    });
  }

  private updateStatsCards(): void {
    this.stats = [
      {
        title: 'Total Patients',
        value: this.totalPatients.toLocaleString(),
        icon: 'bi-people',
        color: 'primary',
        trend: '+12%',
        trendUp: true
      },
      {
        title: 'Médecins actifs',
        value: this.totalDoctors.toLocaleString(),
        icon: 'bi-person-badge',
        color: 'info',
        trend: '+2',
        trendUp: true
      },
      {
        title: 'Dossiers médicaux',
        value: this.totalRecords.toLocaleString(),
        icon: 'bi-folder2-open',
        color: 'success',
        trend: '+8%',
        trendUp: true
      },
      {
        title: 'Analyses IA',
        value: this.totalAiAnalyses.toLocaleString(),
        icon: 'bi-cpu',
        color: 'warning',
        trend: '+24%',
        trendUp: true
      }
    ];
  }

  private computeSpecialtyStats(doctors: any[]): void {
    const specCount: { [key: string]: number } = {};
    doctors.forEach(doc => {
      const spec = doc.specialty || 'Médecine générale';
      specCount[spec] = (specCount[spec] || 0) + 1;
    });

    const total = doctors.length || 1;
    this.specialtyStats = Object.entries(specCount)
      .map(([name, count]) => ({
        name,
        count,
        percent: Math.round((count / total) * 100),
        color: this.specialtyColors[name] || '#6b7a99',
        icon: this.specialtyIcons[name] || 'bi-hospital'
      }))
      .sort((a, b) => b.count - a.count);
  }

  getUsageRate(): number {
    if (this.totalDoctors === 0) return 0;
    return Math.min(100, Math.round((this.totalRecords / Math.max(this.totalPatients, 1)) * 100));
  }
}
