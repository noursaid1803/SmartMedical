import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { MedicalRecordService, MedicalRecord, Patient } from '../../../core/services/medical-record.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-medical-record-form',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './medical-record-form.component.html',
  styleUrl: './medical-record-form.component.scss'
})
export class MedicalRecordFormComponent implements OnInit {
  recordForm: FormGroup;
  patientForm: FormGroup;
  patientId: string | null = null;
  recordId: string | null = null; // ID du dossier médical existant
  patient: Patient | null = null;
  doctorId: string = '';
  doctorName: string = '';
  loading = false;
  saving = false;
  successMessage = '';
  errorMessage = '';
  showPatientForm = false; // Afficher le formulaire patient si nouveau

  severities = [
    { value: 'low', label: 'Faible', color: '#27ae60' },
    { value: 'medium', label: 'Moyenne', color: '#f39c12' },
    { value: 'high', label: 'Élevée', color: '#e74c3c' },
    { value: 'critical', label: 'Critique', color: '#c0392b' }
  ];

  bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-', 'UNKNOWN'];
  genders = [
    { value: 'M', label: 'Masculin' },
    { value: 'F', label: 'Féminin' },
    { value: 'OTHER', label: 'Autre' }
  ];
  relations = ['Parent', 'Conjoint(e)', 'Enfant', 'Frère/Soeur', 'Ami(e)', 'Autre'];
  regions = ['Tunis', 'Ariana', 'Ben Arous', 'Manouba', 'Nabeul', 'Zaghouan', 'Bizerte', 'Béja', 'Jendouba', 'Le Kef', 'Siliana', 'Sousse', 'Monastir', 'Mahdia', 'Sfax', 'Kairouan', 'Kasserine', 'Sidi Bouzid', 'Gabès', 'Medenine', 'Tataouine', 'Gafsa', 'Tozeur', 'Kebili'];

  constructor(
    private fb: FormBuilder,
    private medicalRecordService: MedicalRecordService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.recordForm = this.createForm();
    this.patientForm = this.createPatientForm();
  }

  ngOnInit(): void {
    // Récupérer l'ID du patient depuis l'URL
    this.patientId = this.route.snapshot.paramMap.get('patientId');

    // Récupérer les infos du médecin connecté
    const user = this.authService.getCurrentUser();
    if (user) {
      this.doctorId = String(user.id || '');
      this.doctorName = user.firstName ? `Dr. ${user.firstName}` : 'Docteur';
    }

    // Si un patientId est fourni, charger ses infos
    if (this.patientId) {
      this.loadPatient(this.patientId);
    }
  }

  createForm(): FormGroup {
    return this.fb.group({
      // Consultation
      consultationDate: [new Date().toISOString().split('T')[0], Validators.required],
      reasonForVisit: ['', Validators.required],
      symptoms: ['', Validators.required],

      // Examen physique
      bloodPressure: [''],
      heartRate: [''],
      temperature: [''],
      weight: [''],
      height: [''],
      oxygenSaturation: [''],

      // Diagnostic
      diagnosis: ['', Validators.required],
      severity: ['medium', Validators.required],

      // Traitement
      medications: this.fb.array([]),

      // Tests
      labTests: this.fb.array([]),

      // Notes
      clinicalNotes: [''],
      recommendations: [''],
      followUpRequired: [false],
      followUpDate: ['']
    });
  }

  loadPatient(patientId: string): void {
    this.loading = true;
    this.medicalRecordService.getPatientById(patientId).subscribe({
      next: (patient) => {
        this.patient = patient;
        // Afficher le formulaire et le pré-remplir avec les données existantes
        this.showPatientForm = true;
        this.populatePatientForm(patient);
        // Charger aussi le dossier médical existant
        this.loadMedicalRecord(patientId);
      },
      error: () => {
        // Données de test si l'API n'est pas prête
        this.patient = {
          id: patientId,
          firstName: 'Mohamed',
          lastName: 'Ali',
          dateOfBirth: '1985-03-15',
          gender: 'M',
          bloodType: 'A+',
          phone: '+216 20 111 222',
          emergencyContact: {
            name: 'Ahmed Ali',
            phone: '+216 20 333 444',
            relation: 'Parent'
          }
        };
        this.showPatientForm = true;
        this.populatePatientForm(this.patient);
        this.loadMedicalRecord(patientId);
      }
    });
  }

  // Pré-remplir le formulaire patient avec les données existantes
  populatePatientForm(patient: Patient): void {
    this.patientForm.patchValue({
      firstName: patient.firstName || '',
      lastName: patient.lastName || '',
      email: patient.email || '',
      phone: patient.phone || '',
      dateOfBirth: patient.dateOfBirth || '',
      gender: patient.gender || '',
      address: patient.address || '',
      city: patient.city || '',
      region: patient.region || '',
      postalCode: patient.postalCode || '',
      bloodType: patient.bloodType || 'UNKNOWN',
      height: patient.height || null,
      weight: patient.weight || null,
      allergies: Array.isArray(patient.allergies) ? patient.allergies.join(', ') : (patient.allergies || ''),
      chronicDiseases: Array.isArray(patient.chronicDiseases) ? patient.chronicDiseases.join(', ') : (patient.chronicDiseases || ''),
      currentMedications: Array.isArray(patient.currentMedications) ? patient.currentMedications.join(', ') : (patient.currentMedications || ''),
      occupation: patient.occupation || '',
      employer: patient.employer || '',
      emergencyContactName: patient.emergencyContact?.name || '',
      emergencyContactPhone: patient.emergencyContact?.phone || '',
      emergencyContactRelation: patient.emergencyContact?.relation || '',
      emergencyContactEmail: patient.emergencyContact?.email || '',
      insuranceProvider: patient.insuranceInfo?.provider || '',
      insurancePolicyNumber: patient.insuranceInfo?.policyNumber || ''
    });
  }

  loadMedicalRecord(patientId: string): void {
    this.medicalRecordService.getRecordsByPatient(patientId).subscribe({
      next: (records) => {
        if (records && records.length > 0) {
          // Prendre le dossier le plus récent
          const record = records[0];
          // Stocker l'ID pour la mise à jour
          this.recordId = record.id || null;
          // Pré-remplir le formulaire avec les données existantes
          this.recordForm.patchValue({
            consultationDate: record.consultationDate || new Date().toISOString().split('T')[0],
            reasonForVisit: record.reasonForVisit || '',
            symptoms: record.symptoms || '',
            bloodPressure: record.bloodPressure || '',
            heartRate: record.heartRate || '',
            temperature: record.temperature || '',
            weight: record.weight || '',
            height: record.height || '',
            oxygenSaturation: record.oxygenSaturation || '',
            diagnosis: record.diagnosis || '',
            severity: record.severity || 'medium',
            clinicalNotes: record.clinicalNotes || '',
            recommendations: record.recommendations || '',
            followUpRequired: record.followUpRequired || false,
            followUpDate: record.followUpDate || ''
          });

          // Charger les médicaments s'il y en a
          if (record.medications && record.medications.length > 0) {
            this.medications.clear();
            record.medications.forEach((med: any) => {
              this.medications.push(this.fb.group({
                name: [med.name || '', Validators.required],
                dosage: [med.dosage || '', Validators.required],
                frequency: [med.frequency || '', Validators.required],
                duration: [med.duration || '', Validators.required],
                instructions: [med.instructions || '']
              }));
            });
          }

          // Charger les tests labo s'il y en a
          if (record.labTests && record.labTests.length > 0) {
            this.labTests.clear();
            record.labTests.forEach((test: any) => {
              this.labTests.push(this.fb.group({
                testName: [test.testName || '', Validators.required],
                result: [test.result || ''],
                normalRange: [test.normalRange || ''],
                notes: [test.notes || '']
              }));
            });
          }
        }
        this.loading = false;
      },
      error: () => {
        // Pas de dossier existant, garder le formulaire vide pour nouvelle consultation
        this.loading = false;
      }
    });
  }

  // Formulaire patient complet
  createPatientForm(): FormGroup {
    return this.fb.group({
      // Informations de base
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s-]{8,}$/)]],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],

      // Adresse
      address: [''],
      city: [''],
      region: [''],
      postalCode: [''],

      // Informations médicales
      bloodType: ['UNKNOWN'],
      height: [null],
      weight: [null],
      allergies: [''],
      chronicDiseases: [''],
      currentMedications: [''],

      // Professionnel
      occupation: [''],
      employer: [''],

      // Contact d'urgence
      emergencyContactName: ['', Validators.required],
      emergencyContactPhone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s-]{8,}$/)]],
      emergencyContactRelation: ['', Validators.required],
      emergencyContactEmail: [''],

      // Assurance
      insuranceProvider: [''],
      insurancePolicyNumber: ['']
    });
  }

  // Créer un nouveau patient
  createNewPatient(): void {
    if (this.patientForm.invalid) {
      this.patientForm.markAllAsTouched();
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires du patient.';
      return;
    }

    const formData = this.patientForm.value;
    
    // Fonction utilitaire pour splitter en toute sécurité
    const safeSplit = (value: string | null | undefined): string[] => {
      if (!value || typeof value !== 'string') return [];
      return value.split(',').map((item: string) => item.trim()).filter((item: string) => item.length > 0);
    };
    
    // Créer patient avec SEULEMENT les champs supportés par le backend
    const newPatient: any = {
      firstName: formData.firstName || '',
      lastName: formData.lastName || '',
      age: formData.dateOfBirth ? this.calculateAge(formData.dateOfBirth) : 0,
      gender: formData.gender || '',
      address: formData.address || '',
      phone: formData.phone || '',
      medicalHistory: '',
      allergies: formData.allergies || '',
      currentMedications: formData.currentMedications || '',
      emergencyContact: {
        name: formData.emergencyContactName || 'Non spécifié',
        phone: formData.emergencyContactPhone || '',
        relation: formData.emergencyContactRelation || ''
      }
    };

    this.saving = true;
    this.medicalRecordService.createPatient(newPatient).subscribe({
      next: (patient) => {
        this.patient = patient;
        this.patientId = patient.id || null;
        this.showPatientForm = false;
        this.saving = false;
        this.successMessage = 'Patient créé avec succès !';
        
        // Créer automatiquement un dossier médical vide pour ce patient
        this.createEmptyMedicalRecord(patient.id!);
        
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.message || 'Erreur lors de la création du patient';
        this.saving = false;
      }
    });
  }

  // Créer un dossier médical vide initial
  createEmptyMedicalRecord(patientId: string): void {
    const emptyRecord: MedicalRecord = {
      patientId: patientId,
      doctorId: this.doctorId || 'unknown',
      doctorName: this.doctorName || 'Médecin',
      consultationDate: new Date().toISOString().split('T')[0],
      reasonForVisit: 'Première consultation',
      symptoms: '',
      diagnosis: 'En attente d\'évaluation',
      severity: 'low'
    };
    
    this.medicalRecordService.createRecord(emptyRecord).subscribe({
      next: (record) => {
        console.log('Dossier médical créé:', record);
      },
      error: (err) => {
        console.error('Erreur création dossier médical:', err);
      }
    });
  }

  // Calculer l'âge
  // Mettre à jour un patient existant
  updateExistingPatient(): void {
    if (this.patientForm.invalid) {
      this.patientForm.markAllAsTouched();
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires du patient.';
      return;
    }

    const formData = this.patientForm.value;
    
    // Fonction utilitaire pour splitter en toute sécurité
    const safeSplit = (value: string | null | undefined): string[] => {
      if (!value || typeof value !== 'string') return [];
      return value.split(',').map((item: string) => item.trim()).filter((item: string) => item.length > 0);
    };
    
    const updatedPatient: Patient = {
      firstName: formData.firstName || '',
      lastName: formData.lastName || '',
      email: formData.email || '',
      phone: formData.phone || '',
      dateOfBirth: formData.dateOfBirth || '',
      gender: formData.gender || '',
      address: formData.address || '',
      city: formData.city || '',
      region: formData.region || '',
      postalCode: formData.postalCode || '',
      bloodType: formData.bloodType || 'UNKNOWN',
      height: formData.height || null,
      weight: formData.weight || null,
      allergies: safeSplit(formData.allergies),
      chronicDiseases: safeSplit(formData.chronicDiseases),
      currentMedications: safeSplit(formData.currentMedications),
      occupation: formData.occupation || '',
      employer: formData.employer || '',
      emergencyContact: {
        name: formData.emergencyContactName || '',
        phone: formData.emergencyContactPhone || '',
        relation: formData.emergencyContactRelation || '',
        email: formData.emergencyContactEmail || ''
      },
      insuranceInfo: formData.insuranceProvider ? {
        provider: formData.insuranceProvider,
        policyNumber: formData.insurancePolicyNumber || '',
        coverageType: 'Standard'
      } : undefined
    };

    if (!this.patientId) return;

    this.saving = true;
    this.medicalRecordService.updatePatient(this.patientId, updatedPatient).subscribe({
      next: (patient) => {
        this.patient = patient;
        this.saving = false;
        this.successMessage = 'Patient mis à jour avec succès !';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error) => {
        this.saving = false;
        this.errorMessage = 'Erreur lors de la mise à jour du patient.';
        console.error('Erreur:', error);
      }
    });
  }

  calculateAge(dateOfBirth: string): number {
    if (!dateOfBirth) return 0;
    const today = new Date();
    const birthDate = new Date(dateOfBirth);
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  }

  // Getters pour FormArray
  get medications(): FormArray {
    return this.recordForm.get('medications') as FormArray;
  }

  get labTests(): FormArray {
    return this.recordForm.get('labTests') as FormArray;
  }

  // Ajouter médicament
  addMedication(): void {
    const medication = this.fb.group({
      name: ['', Validators.required],
      dosage: ['', Validators.required],
      frequency: ['', Validators.required],
      duration: ['', Validators.required],
      instructions: ['']
    });
    this.medications.push(medication);
  }

  // Supprimer médicament
  removeMedication(index: number): void {
    this.medications.removeAt(index);
  }

  // Ajouter test
  addLabTest(): void {
    const test = this.fb.group({
      testName: ['', Validators.required],
      result: [''],
      normalRange: [''],
      notes: ['']
    });
    this.labTests.push(test);
  }

  // Supprimer test
  removeLabTest(index: number): void {
    this.labTests.removeAt(index);
  }

  onSubmit(): void {
    if (this.recordForm.invalid) {
      this.markFormGroupTouched(this.recordForm);
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const record: MedicalRecord = {
      ...this.recordForm.value,
      patientId: this.patientId || 'temp-id',
      doctorId: this.doctorId,
      doctorName: this.doctorName
    };

    if (this.recordId) {
      // Mise à jour du dossier existant
      this.medicalRecordService.updateRecord(this.recordId, record).subscribe({
        next: () => {
          this.saving = false;
          this.successMessage = 'Dossier médical mis à jour avec succès !';
          setTimeout(() => {
            this.router.navigate(['/doctor/patients']);
          }, 2000);
        },
        error: (error) => {
          this.saving = false;
          this.errorMessage = 'Erreur lors de la mise à jour. Veuillez réessayer.';
          console.error('Erreur:', error);
        }
      });
    } else {
      // Création d'un nouveau dossier
      this.medicalRecordService.createRecord(record).subscribe({
        next: () => {
          this.saving = false;
          this.successMessage = 'Dossier médical enregistré avec succès !';
          setTimeout(() => {
            this.router.navigate(['/doctor/patients']);
          }, 2000);
        },
        error: (error) => {
          this.saving = false;
          this.errorMessage = 'Erreur lors de l\'enregistrement. Veuillez réessayer.';
          console.error('Erreur:', error);
        }
      });
    }
  }

  // Helper pour afficher le nom complet du patient
  getPatientFullName(): string {
    if (!this.patient) return '';
    return `${this.patient.firstName} ${this.patient.lastName}`;
  }

  cancel(): void {
    this.router.navigate(['/doctor/patients']);
  }

  // Helper pour marquer tous les champs comme touchés
  private markFormGroupTouched(formGroup: FormGroup | FormArray): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup || control instanceof FormArray) {
        this.markFormGroupTouched(control);
      }
    });
  }

  // ==========================================
  // GESTION DE L'IRM - ANALYSE IA
  // ==========================================

  selectedIRMImage: string | null = null;
  analyzingIRM = false;
  irmAnalysisResult: IRMAnalysisResult | null = null;

  // Interface pour le résultat de l'analyse IRM

  onIRMFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();

      reader.onload = (e) => {
        this.selectedIRMImage = e.target?.result as string;
        this.irmAnalysisResult = null;
      };

      reader.readAsDataURL(file);
    }
  }

  clearIRMImage(): void {
    this.selectedIRMImage = null;
    this.irmAnalysisResult = null;
    this.analyzingIRM = false;
  }

  analyzeIRMImage(): void {
    if (!this.selectedIRMImage) return;

    this.analyzingIRM = true;
    this.irmAnalysisResult = null;

    // Simuler l'analyse IA (à remplacer par un appel API réel)
    setTimeout(() => {
      // Simulation de résultat d'analyse
      this.irmAnalysisResult = this.simulateIrmAnalysis();
      this.analyzingIRM = false;
    }, 3000);
  }

  // Simulation de l'analyse IA (à remplacer par un vrai service)
  private simulateIrmAnalysis(): IRMAnalysisResult {
    const isNormal = Math.random() > 0.3; // 70% de chance d'être normal pour la démo

    if (isNormal) {
      return {
        isNormal: true,
        diagnosis: 'IRM Normale',
        confidence: Math.floor(Math.random() * 15) + 85, // 85-100%
        detections: [],
        recommendations: 'Aucune anomalie détectée. Suite des soins standard recommandée.'
      };
    } else {
      return {
        isNormal: false,
        diagnosis: 'Anomalies détectées',
        confidence: Math.floor(Math.random() * 20) + 70, // 70-90%
        detections: [
          {
            type: 'Lésion',
            location: 'Lobe frontal gauche',
            confidence: Math.floor(Math.random() * 15) + 80,
            severity: 'medium'
          },
          {
            type: 'Anomalie de signal',
            location: 'Zone périventriculaire',
            confidence: Math.floor(Math.random() * 20) + 65,
            severity: 'low'
          }
        ],
        recommendations: 'Consultation spécialisée recommandée. IRM de contrôle suggérée dans 3 mois. Biopsie peut être nécessaire selon l\'avis du spécialiste.'
      };
    }
  }

  addIRMToClinicalNotes(): void {
    if (!this.irmAnalysisResult) return;

    const currentNotes = this.recordForm.get('clinicalNotes')?.value || '';
    const irmReport = this.generateIRMReport();

    const newNotes = currentNotes
      ? `${currentNotes}\n\n--- Analyse IRM ---\n${irmReport}`
      : `--- Analyse IRM ---\n${irmReport}`;

    this.recordForm.patchValue({ clinicalNotes: newNotes });
    this.successMessage = 'Rapport IRM ajouté aux notes cliniques !';
    setTimeout(() => this.successMessage = '', 3000);
  }

  private generateIRMReport(): string {
    if (!this.irmAnalysisResult) return '';

    const result = this.irmAnalysisResult;
    let report = `Date: ${new Date().toLocaleDateString()}\n`;
    report += `Diagnostic: ${result.diagnosis}\n`;
    report += `Confiance IA: ${result.confidence}%\n`;

    if (result.detections && result.detections.length > 0) {
      report += '\nDétections:\n';
      result.detections.forEach((det, index) => {
        report += `  ${index + 1}. ${det.type} - ${det.location} (${det.confidence}%)\n`;
      });
    }

    report += `\nRecommandations: ${result.recommendations}`;

    return report;
  }
}

// Interface pour le résultat de l'analyse IRM
export interface IRMDetection {
  type: string;
  location: string;
  confidence: number;
  severity: 'low' | 'medium' | 'high';
}

export interface IRMAnalysisResult {
  isNormal: boolean;
  diagnosis: string;
  confidence: number;
  detections: IRMDetection[];
  recommendations: string;
}
