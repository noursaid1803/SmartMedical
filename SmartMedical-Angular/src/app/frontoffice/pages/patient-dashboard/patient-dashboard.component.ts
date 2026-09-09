import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { MedicalRecordService, MedicalRecord, Patient } from '../../../core/services/medical-record.service';
import { DoctorService } from '../../../core/services/doctor.service';
import { Appointment, AppointmentService } from '../../../core/services/appointment.service';
import { environment } from '../../../../environments/environment';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './patient-dashboard.component.html',
  styleUrl: './patient-dashboard.component.scss'
})
export class PatientDashboardComponent implements OnInit {
  activeTab: 'overview' | 'history' | 'exams' | 'prescriptions' | 'appointments' | 'share' | 'chatbot' = 'overview';
  appointments: Appointment[] = [];
  appointmentsLoading = false;

  // Patient data
  currentUser: any = null;
  patient: Patient | null = null;
  medicalRecords: MedicalRecord[] = [];
  filteredRecords: MedicalRecord[] = [];
  loading = false;
  patientLoading = false;

  // Health metrics
  healthMetrics = {
    lastVisit: '',
    totalConsultations: 0,
    activeExams: 0,
    activePrescriptions: 0
  };

  // Filters
  filterDiagnosis = '';
  filterDate = '';

  // Doctors for sharing
  doctors: any[] = [];
  selectedDoctorShare = '';
  shareSuccess = '';

  // Chatbot
  chatMessages: ChatMessage[] = [];
  chatInput = '';
  chatLoading = false;
  chatSuggestions: string[] = [
    'Résume mon dossier',
    'Quels sont mes traitements ?',
    'Quels examens complémentaires faire ?',
    'Symptômes à surveiller'
  ];

  // Image modal
  selectedScan: MedicalRecord | null = null;
  showScanModal = false;

  constructor(
    private authService: AuthService,
    private medicalRecordService: MedicalRecordService,
    private doctorService: DoctorService,
    private appointmentService: AppointmentService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadPatientData();
    this.loadDoctors();
    this.loadAppointments();
  }

  loadAppointments(): void {
    const email = this.currentUser?.email;
    if (!email) return;
    this.appointmentsLoading = true;
    this.appointmentService.getByPatientEmail(email).subscribe({
      next: (list) => {
        this.appointments = list;
        this.appointmentsLoading = false;
      },
      error: () => {
        this.appointments = [];
        this.appointmentsLoading = false;
      }
    });
  }

  getAppointmentStatusLabel(status?: string): string {
    const map: Record<string, string> = {
      PENDING: 'En attente',
      CONFIRMED: 'Confirmé',
      CANCELLED: 'Annulé',
      COMPLETED: 'Terminé'
    };
    return map[status || 'PENDING'] || status || 'En attente';
  }

  loadPatientData(): void {
    this.patientLoading = true;
    const user = this.currentUser;
    if (!user) return;

    // Try to find patient by email
    this.medicalRecordService.getAllPatients().subscribe({
      next: (patients) => {
        const found = patients.find(p =>
          p.email?.toLowerCase() === user.email?.toLowerCase()
        );
        if (found) {
          this.patient = found;
          this.patientLoading = false;
          if (found.id) {
            this.loadMedicalRecords(found.id);
          }
        } else {
          // Auto-create patient profile
          this.createPatientProfile(user);
        }
      },
      error: () => {
        // Use mock data
        this.patient = this.getMockPatient(user);
        this.patientLoading = false;
        this.medicalRecords = this.getMockRecords();
        this.filteredRecords = [...this.medicalRecords];
        this.computeMetrics();
      }
    });
  }

  private createPatientProfile(user: any): void {
    const newPatient: any = {
      firstName: user.firstName || 'Patient',
      lastName: user.lastName || '',
      email: user.email || '',
      phone: user.phone || '',
      age: 0,
      gender: 'OTHER',
      address: '',
      medicalHistory: '',
      allergies: '',
      currentMedications: '',
      emergencyContact: {
        name: 'Non spécifié',
        phone: '00000000',
        relation: 'Autre'
      }
    };

    this.medicalRecordService.createPatient(newPatient).subscribe({
      next: (p) => {
        this.patient = p;
        this.patientLoading = false;
      },
      error: () => {
        this.patient = this.getMockPatient(user);
        this.patientLoading = false;
      }
    });
  }

  private getMockPatient(user: any): Patient {
    return {
      firstName: user.firstName || 'Patient',
      lastName: user.lastName || '',
      email: user.email || '',
      phone: user.phone || '',
      bloodType: 'UNKNOWN',
      emergencyContact: { name: 'Non spécifié', phone: '0000', relation: 'Autre' }
    };
  }

  private getMockRecords(): MedicalRecord[] {
    return [
      {
        id: '1',
        patientId: 'mock',
        doctorId: 'doc1',
        doctorName: 'Dr. Ahmed Ben Salah',
        consultationDate: '2026-04-15',
        reasonForVisit: 'Contrôle pulmonaire de routine',
        symptoms: 'Légère toux sèche persistante depuis 2 semaines',
        diagnosis: 'Inflammation bronchique modérée',
        severity: 'low',
        medications: [{ name: 'Ventoline', dosage: '100mcg', frequency: '2x/jour', duration: '7 jours' }],
        recommendations: 'Éviter les environnements poussiéreux, contrôle dans 1 mois.',
        clinicalNotes: 'Spirométrie dans les normes. Pas d\'anomalie structurelle détectée.',
        imageUrl: '',
        aiResult: ''
      },
      {
        id: '2',
        patientId: 'mock',
        doctorId: 'doc2',
        doctorName: 'Dr. Sonia Maaref',
        consultationDate: '2026-02-10',
        reasonForVisit: 'Douleurs thoraciques et essoufflement',
        symptoms: 'Essoufflement à l\'effort, douleur thoracique gauche intermittente',
        diagnosis: 'Syndrome intercostal bénin',
        severity: 'medium',
        recommendations: 'Repos et anti-inflammatoires 5 jours.',
        clinicalNotes: 'ECG normal. Radiographie thoracique sans anomalie.',
        imageUrl: '',
        aiResult: ''
      }
    ];
  }

  loadMedicalRecords(patientId: string): void {
    this.loading = true;
    this.medicalRecordService.getRecordsByPatient(patientId).subscribe({
      next: (records) => {
        this.medicalRecords = records.sort((a, b) =>
          new Date(b.consultationDate).getTime() - new Date(a.consultationDate).getTime()
        );
        this.filteredRecords = [...this.medicalRecords];
        this.loading = false;
        this.computeMetrics();
      },
      error: () => {
        this.medicalRecords = this.getMockRecords();
        this.filteredRecords = [...this.medicalRecords];
        this.loading = false;
        this.computeMetrics();
      }
    });
  }

  computeMetrics(): void {
    this.healthMetrics.totalConsultations = this.medicalRecords.length;
    if (this.medicalRecords.length > 0) {
      this.healthMetrics.lastVisit = this.medicalRecords[0].consultationDate;
    }
    this.healthMetrics.activeExams = this.medicalRecords.filter(r => r.imageUrl).length;
    let presCount = 0;
    this.medicalRecords.forEach(r => {
      if (r.medications && r.medications.length > 0) presCount += r.medications.length;
    });
    this.healthMetrics.activePrescriptions = presCount;
  }

  loadDoctors(): void {
    this.doctorService.getAllDoctors().subscribe({
      next: (docs) => { this.doctors = docs; },
      error: () => {
        this.doctors = [
          { id: 'doc1', firstName: 'Ahmed', lastName: 'Ben Salah', specialty: 'Pneumologie' },
          { id: 'doc2', firstName: 'Sonia', lastName: 'Maaref', specialty: 'Cardiologie' }
        ];
      }
    });
  }

  setTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
  }

  applyFilters(): void {
    this.filteredRecords = this.medicalRecords.filter(r => {
      const matchDiag = !this.filterDiagnosis ||
        r.diagnosis?.toLowerCase().includes(this.filterDiagnosis.toLowerCase());
      const matchDate = !this.filterDate ||
        r.consultationDate?.startsWith(this.filterDate);
      return matchDiag && matchDate;
    });
  }

  resetFilters(): void {
    this.filterDiagnosis = '';
    this.filterDate = '';
    this.filteredRecords = [...this.medicalRecords];
  }

  getExamRecords(): MedicalRecord[] {
    return this.medicalRecords.filter(r => r.imageUrl);
  }

  getPrescriptionRecords(): MedicalRecord[] {
    return this.medicalRecords.filter(r => r.medications && r.medications.length > 0);
  }

  openScanModal(record: MedicalRecord): void {
    this.selectedScan = record;
    this.showScanModal = true;
  }

  closeScanModal(): void {
    this.showScanModal = false;
    this.selectedScan = null;
  }

  shareRecord(): void {
    if (!this.selectedDoctorShare) return;
    const doc = this.doctors.find(d => d.id === this.selectedDoctorShare);
    if (doc) {
      this.shareSuccess = `Dossier partagé avec succès avec Dr. ${doc.firstName} ${doc.lastName} !`;
      setTimeout(() => this.shareSuccess = '', 4000);
      this.selectedDoctorShare = '';
    }
  }

  downloadReport(record: MedicalRecord): void {
    const content = this.generateReportContent(record);
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `rapport_${record.consultationDate}_SmartMedical.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  private generateReportContent(record: MedicalRecord): string {
    let content = `=== RAPPORT MÉDICAL - SMARTMEDICAL ===\n\n`;
    content += `Patient: ${this.patient?.firstName} ${this.patient?.lastName}\n`;
    content += `Date de consultation: ${record.consultationDate}\n`;
    content += `Médecin: ${record.doctorName}\n\n`;
    content += `Motif: ${record.reasonForVisit}\n`;
    content += `Symptômes: ${record.symptoms}\n`;
    content += `Diagnostic: ${record.diagnosis}\n`;
    content += `Sévérité: ${record.severity}\n\n`;
    if (record.medications?.length) {
      content += `Traitements prescrits:\n`;
      record.medications.forEach(m => {
        content += `  - ${m.name} ${m.dosage} - ${m.frequency} pendant ${m.duration}\n`;
      });
    }
    if (record.recommendations) content += `\nRecommandations: ${record.recommendations}\n`;
    if (record.aiResult) content += `\nAnalyse IA: ${record.aiResult}\n`;
    content += `\n--- Cette recommandation ne remplace pas l'avis du médecin. ---\n`;
    return content;
  }

  // ---- CHATBOT ----
  sendChatMessage(msg?: string): void {
    const text = (msg || this.chatInput).trim();
    if (!text) return;

    this.chatMessages.push({ role: 'user', content: text, timestamp: new Date() });
    this.chatInput = '';
    this.chatLoading = true;

    const patientHistory: any = {
      firstName: this.patient?.firstName || '',
      lastName: this.patient?.lastName || '',
      age: this.patient?.age || '',
      gender: this.patient?.gender || '',
      bloodType: this.patient?.bloodType || '',
      medicalHistory: Array.isArray(this.patient?.chronicDiseases)
        ? this.patient?.chronicDiseases?.join(', ')
        : (this.patient as any)?.medicalHistory || '',
      allergies: Array.isArray(this.patient?.allergies)
        ? this.patient?.allergies?.join(', ')
        : (this.patient as any)?.allergies || '',
      currentMedications: Array.isArray(this.patient?.currentMedications)
        ? this.patient?.currentMedications?.join(', ')
        : (this.patient as any)?.currentMedications || '',
      records: this.medicalRecords
    };

    const chatHistory = this.chatMessages.slice(-6).map(m => ({ role: m.role, content: m.content }));

    this.http.post<any>(`${environment.gatewayUrl}/ai/chatbot`, {
      message: text,
      patient_history: patientHistory,
      chat_history: chatHistory
    }).subscribe({
      next: (res) => {
        this.chatMessages.push({ role: 'assistant', content: res.response, timestamp: new Date() });
        this.chatSuggestions = res.suggestions || this.chatSuggestions;
        this.chatLoading = false;
        setTimeout(() => this.scrollChatToBottom(), 100);
      },
      error: () => {
        this.chatMessages.push({
          role: 'assistant',
          content: this.buildLocalRecommendation(text),
          timestamp: new Date()
        });
        this.chatLoading = false;
        setTimeout(() => this.scrollChatToBottom(), 100);
      }
    });
  }

  /** Recommandations locales basées sur l'historique (si l'API IA est indisponible) */
  private buildLocalRecommendation(question: string): string {
    const q = question.toLowerCase();
    const records = this.medicalRecords;
    const allergies = Array.isArray(this.patient?.allergies)
      ? this.patient!.allergies!.join(', ')
      : '';
    const meds = Array.isArray(this.patient?.currentMedications)
      ? this.patient!.currentMedications!.join(', ')
      : '';

    let response = '### Assistant SmartMedical\n\n';

    if (q.includes('résum') || q.includes('dossier')) {
      response += `Vous avez **${records.length} consultation(s)** enregistrée(s).`;
      if (records[0]) {
        response += `\n- Dernier diagnostic : ${records[0].diagnosis || 'Non renseigné'}`;
        response += `\n- Dernière visite : ${records[0].consultationDate}`;
      }
    } else if (q.includes('traitement') || q.includes('médicament')) {
      response += meds
        ? `Traitements actuels : ${meds}.`
        : 'Aucun traitement en cours enregistré dans votre dossier.';
      records.forEach(r => {
        if (r.medications?.length) {
          r.medications.forEach(m => {
            response += `\n- ${m.name} (${m.dosage}) — ${r.consultationDate}`;
          });
        }
      });
    } else if (q.includes('examen') || q.includes('irm') || q.includes('scan')) {
      const withImg = records.filter(r => r.imageUrl);
      response += withImg.length
        ? `${withImg.length} examen(s) d'imagerie disponible(s). Consultez l'onglet « Examens & Scans ».`
        : 'Aucun examen d\'imagerie pour le moment. Votre médecin pourra en ajouter après analyse.';
    } else if (q.includes('symptôme') || q.includes('surveill')) {
      const last = records[0];
      response += last?.symptoms
        ? `Derniers symptômes notés : ${last.symptoms}. En cas d'aggravation, contactez votre médecin ou les urgences.`
        : 'Surveillez tout symptôme nouveau et notez-le pour votre prochaine consultation.';
    } else {
      response += 'Selon votre historique, nous vous recommandons de maintenir vos traitements prescrits et de planifier un suivi régulier.';
      if (allergies) response += `\n\n⚠️ Allergies connues : ${allergies}`;
    }

    response += '\n\n---\n*Cette aide est informative et ne remplace pas l\'avis de votre médecin.*';
    return response;
  }

  scrollChatToBottom(): void {
    const container = document.querySelector('.chat-messages');
    if (container) container.scrollTop = container.scrollHeight;
  }

  onChatKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendChatMessage();
    }
  }

  getSeverityLabel(severity: string): string {
    const map: any = { low: 'Faible', medium: 'Moyen', high: 'Élevé', critical: 'Critique' };
    return map[severity] || severity;
  }

  getSeverityClass(severity: string): string {
    const map: any = { low: 'severity-low', medium: 'severity-medium', high: 'severity-high', critical: 'severity-critical' };
    return map[severity] || '';
  }

  getAiResultParsed(record: MedicalRecord): any {
    if (!record.aiResult) return null;
    try { return JSON.parse(record.aiResult); } catch { return { diagnosis: record.aiResult }; }
  }

  formatMarkdown(text: string): string {
    if (!text) return '';
    return text
      .replace(/### (.*?)(\n|$)/g, '<h4>$1</h4>')
      .replace(/## (.*?)(\n|$)/g, '<h3>$1</h3>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/^- (.*?)(\n|$)/gm, '<li>$1</li>')
      .replace(/\n/g, '<br>')
      .replace(/---/g, '<hr>');
  }

  getPatientInitials(): string {
    const fn = this.patient?.firstName?.[0] || this.currentUser?.firstName?.[0] || 'P';
    const ln = this.patient?.lastName?.[0] || this.currentUser?.lastName?.[0] || '';
    return (fn + ln).toUpperCase();
  }
}
