import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { MedicalRecordService, MedicalRecord, Patient } from '../../../core/services/medical-record.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileService } from '../../../core/services/profile.service';
import { AiAnalysisService, AlzheimerAnalysisResponse, LungCancerAnalysisResponse, OrganValidationResponse } from '../../services/ai-analysis.service';
import {
  ALL_AI_MODELS,
  AiModelDefinition,
  buildSpecialtyRestrictionMessage,
  filterModelsBySpecialty as filterAiModelsBySpecialty,
  isOrganAllowedForSpecialty,
  normalizeSpecialty
} from '../../../core/config/specialty-organ.config';

// Interface for IRM analysis result
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
  stage?: number;
  stageLabel?: string;
  stageName?: string;
  gradcamImageBase64?: string;
  probabilities?: Record<string, number>;
  precision?: number;
}

export interface AlzheimerStageInfo {
  code: string;
  title: string;
  meaning: string;
  clinical: string;
}

export type AlzheimerReliabilityLevel = 'high' | 'moderate' | 'low';

export interface AlzheimerReliabilityFactor {
  id: string;
  label: string;
  detail: string;
  score: number;
  maxScore: number;
  status: 'good' | 'ok' | 'weak';
}

export interface AlzheimerReliabilityAssessment {
  level: AlzheimerReliabilityLevel;
  label: string;
  score: number;
  summary: string;
  advice: string;
  factors: AlzheimerReliabilityFactor[];
}

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
  recordId: string | null = null;
  editRecordId: string | null = null;
  isNewConsultation = false;
  patient: Patient | null = null;
  doctorId: string = '';
  doctorName: string = '';
  doctorSpecialty: string = '';
  loading = false;
  saving = false;
  successMessage = '';
  errorMessage = '';
  showPatientForm = false;

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

  readonly alzheimerStages: AlzheimerStageInfo[] = [
    {
      code: 'CN',
      title: 'Non Demented (CN)',
      meaning: 'Cognition normale — pas de démence',
      clinical: 'Profil sain, aucune atrophie significative détectée'
    },
    {
      code: 'VMCI',
      title: 'Very Mild MCI (VMCI)',
      meaning: 'Déclin cognitif très léger',
      clinical: 'Surveillance cognitive et IRM de suivi recommandées'
    },
    {
      code: 'EMCI',
      title: 'Mild / EMCI',
      meaning: 'Déclin cognitif léger à modéré',
      clinical: 'Consultation neurologique et bilan neuropsychologique'
    },
    {
      code: 'AD',
      title: 'Moderate AD',
      meaning: 'Maladie d\'Alzheimer modérée',
      clinical: 'Prise en charge spécialisée urgente'
    }
  ];

  readonly criteriaLabels: Record<string, string> = {
    taille_cerveau: 'Taille du cerveau',
    forme_ovale: 'Forme ovale',
    centre_ok: 'Centrage',
    fond_noir: 'Fond sombre',
    'ventricules_0.3': 'Ventricules',
    'symétrie_0.60': 'Symétrie',
    'texture_1.0': 'Texture IRM'
  };

  // ---- AI Section ----
  selectedIRMImage: string | null = null;
  analyzingIRM = false;
  irmAnalysisResult: IRMAnalysisResult | null = null;
  selectedAIModel: AiModelDefinition | null = null;
  validatingImage = false;
  imageValidationResult: OrganValidationResponse | null = null;
  imageValidationError: string = '';
  imageValidationBlocked = false;
  showModelSelection = true;
  showImageUpload = false;

  private readonly allAiModels = ALL_AI_MODELS;

  aiModels: AiModelDefinition[] = [];
  specialtyLocked = false;
  specialtyRestrictionMessage = '';
  allowedOrgansForSpecialty: string[] = [];
  uploadBlockedBySpecialty = false;

  constructor(
    private fb: FormBuilder,
    private medicalRecordService: MedicalRecordService,
    private authService: AuthService,
    private profileService: ProfileService,
    private router: Router,
    private route: ActivatedRoute,
    private aiAnalysisService: AiAnalysisService
  ) {
    this.recordForm = this.createForm();
    this.patientForm = this.createPatientForm();
    this.showModelSelection = true;
    this.showImageUpload = false;
    this.selectedAIModel = null;
    this.aiModels = [...this.allAiModels];
  }

  ngOnInit(): void {
    this.patientId = this.route.snapshot.paramMap.get('patientId');
    this.editRecordId = this.route.snapshot.queryParamMap.get('recordId');
    this.isNewConsultation = this.route.snapshot.queryParamMap.get('mode') === 'new';

    const user = this.authService.getCurrentUser();
    if (user) {
      this.doctorId = String(user.id || '');
      this.doctorName = user.firstName ? `Dr. ${user.firstName}` : 'Docteur';
    }

    if (this.patientId) {
      this.loadPatient(this.patientId);
    } else {
      this.showPatientForm = true;
      this.resetAIAnalysisState();
    }

    this.route.paramMap.subscribe(params => {
      const id = params.get('patientId');
      if (id && id !== this.patientId) {
        this.patientId = id;
        this.loadPatient(id);
      }
    });

    this.route.queryParamMap.subscribe(q => {
      this.editRecordId = q.get('recordId');
      this.isNewConsultation = q.get('mode') === 'new';
      if (this.patientId) {
        this.loadMedicalRecord(this.patientId);
      }
    });

    this.profileService.getMyProfile().subscribe({
      next: (profile) => {
        const specialty = normalizeSpecialty(profile.specialty, profile.specialtyCode);
        if (specialty) {
          this.applySpecialtyFilter(specialty);
        } else {
          this.aiModels = [...this.allAiModels];
          this.specialtyLocked = false;
          this.uploadBlockedBySpecialty = false;
        }
      },
      error: () => {
        this.aiModels = [...this.allAiModels];
        this.specialtyLocked = false;
        this.uploadBlockedBySpecialty = false;
      }
    });
  }

  applySpecialtyFilter(specialty: string): void {
    const result = filterAiModelsBySpecialty(specialty, this.allAiModels);
    this.doctorSpecialty = result.specialty;
    this.aiModels = result.models;
    this.specialtyLocked = result.locked;
    this.allowedOrgansForSpecialty = result.allowedOrgans;
    this.uploadBlockedBySpecialty = result.locked && result.allowedOrgans.length === 0;
    this.specialtyRestrictionMessage = buildSpecialtyRestrictionMessage(
      result.specialty,
      result.allowedOrgans
    );

    if (this.aiModels.length === 1) {
      this.selectedAIModel = this.aiModels[0];
      this.showModelSelection = false;
      this.showImageUpload = true;
    }
  }

  get canChangeModel(): boolean {
    return !this.specialtyLocked;
  }

  resetAIAnalysisState(): void {
    this.showModelSelection = true;
    this.showImageUpload = false;
    this.selectedAIModel = null;
    this.selectedIRMImage = null;
    this.imageValidationResult = null;
    this.imageValidationError = '';
    this.imageValidationBlocked = false;
    this.validatingImage = false;
    this.irmAnalysisResult = null;
  }

  get canAnalyzeImage(): boolean {
    return !!(
      this.selectedIRMImage &&
      !this.validatingImage &&
      !this.imageValidationBlocked &&
      this.imageValidationResult?.valid === true
    );
  }

  get imageValidationAccepted(): boolean {
    return this.imageValidationResult?.valid === true && !this.imageValidationBlocked;
  }

  get isAlzheimerSelected(): boolean {
    return this.isAlzheimerModel(this.selectedAIModel);
  }

  get isEditMode(): boolean {
    return !!this.recordId && !this.isNewConsultation;
  }

  get pageTitle(): string {
    if (this.isEditMode) {
      return 'Modifier le Dossier Médical';
    }
    if (this.patientId && this.isNewConsultation) {
      return 'Nouvelle Consultation';
    }
    if (this.patientId) {
      return 'Dossier Médical du Patient';
    }
    return 'Nouveau Dossier Médical';
  }

  createForm(): FormGroup {
    return this.fb.group({
      consultationDate: [new Date().toISOString().split('T')[0], Validators.required],
      reasonForVisit: ['', Validators.required],
      symptoms: ['', Validators.required],
      bloodPressure: [''],
      heartRate: [''],
      temperature: [''],
      weight: [''],
      height: [''],
      oxygenSaturation: [''],
      diagnosis: ['', Validators.required],
      severity: ['medium', Validators.required],
      medications: this.fb.array([]),
      labTests: this.fb.array([]),
      clinicalNotes: [''],
      recommendations: [''],
      followUpRequired: [false],
      followUpDate: ['']
    });
  }

  loadPatient(patientId: string): void {
    this.loading = true;
    this.errorMessage = '';
    this.medicalRecordService.getPatientById(patientId).subscribe({
      next: (patient) => {
        this.patient = patient;
        this.showPatientForm = true;
        this.populatePatientForm(patient);
        this.loadMedicalRecord(patientId);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Impossible de charger les informations du patient.';
      }
    });
  }

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
    if (this.isNewConsultation) {
      this.recordId = null;
      this.recordForm.reset({
        consultationDate: new Date().toISOString().split('T')[0],
        severity: 'medium',
        followUpRequired: false
      });
      this.medications.clear();
      this.labTests.clear();
      this.resetAIAnalysisState();
      this.loading = false;
      return;
    }

    const finish = (record: MedicalRecord | null) => {
      if (record) {
        this.applyRecordToForm(record);
      } else {
        this.recordId = null;
      }
      this.loading = false;
    };

    if (this.editRecordId) {
      this.medicalRecordService.getRecordById(this.editRecordId).subscribe({
        next: (record) => finish(record),
        error: () => this.loadLatestMedicalRecord(patientId, finish)
      });
      return;
    }

    this.loadLatestMedicalRecord(patientId, finish);
  }

  private loadLatestMedicalRecord(
    patientId: string,
    finish: (record: MedicalRecord | null) => void
  ): void {
    this.medicalRecordService.getRecordsByPatient(patientId).subscribe({
      next: (records) => {
        if (!records?.length) {
          finish(null);
          return;
        }
        const sorted = [...records].sort(
          (a, b) =>
            new Date(b.consultationDate || 0).getTime() - new Date(a.consultationDate || 0).getTime()
        );
        finish(sorted[0]);
      },
      error: () => finish(null)
    });
  }

  private applyRecordToForm(record: MedicalRecord): void {
    this.recordId = record.id || null;
    const clinicalNotes = record.clinicalNotes || (record as any).notes || '';

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
      clinicalNotes,
      recommendations: record.recommendations || '',
      followUpRequired: record.followUpRequired || false,
      followUpDate: record.followUpDate || ''
    });

    this.medications.clear();
    if (record.medications?.length) {
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

    this.labTests.clear();
    if (record.labTests?.length) {
      record.labTests.forEach((test: any) => {
        this.labTests.push(this.fb.group({
          testName: [test.testName || '', Validators.required],
          result: [test.result || ''],
          normalRange: [test.normalRange || ''],
          notes: [test.notes || '']
        }));
      });
    }

    this.restoreAiAnalysisFromRecord(record);
  }

  private restoreAiAnalysisFromRecord(record: MedicalRecord): void {
    if (record.imageUrl) {
      this.selectedIRMImage = record.imageUrl;
    }

    if (!record.aiResult) {
      return;
    }

    try {
      const parsed = JSON.parse(record.aiResult);
      const model = this.allAiModels.find(m => m.id === parsed.model);
      if (model) {
        this.selectedAIModel = model;
        this.showModelSelection = false;
        this.showImageUpload = true;
      }

      this.irmAnalysisResult = {
        isNormal: !!parsed.isNormal,
        diagnosis: parsed.diagnosis || record.diagnosis || '',
        confidence: Math.round(Number(parsed.confidence) || 0),
        detections: parsed.detections || [],
        recommendations: parsed.recommendations || record.recommendations || '',
        stageLabel: parsed.stageLabel,
        stageName: parsed.stageName,
        probabilities: parsed.probabilities,
        gradcamImageBase64: parsed.gradcamImageBase64
      };

      if (parsed.validation) {
        this.imageValidationResult = parsed.validation;
        this.imageValidationBlocked = !parsed.validation.valid;
        this.imageValidationError = parsed.validation.valid
          ? ''
          : (parsed.validation.rejection_reason || parsed.validation.message || '');
      } else if (this.selectedIRMImage && this.isAlzheimerModel(this.selectedAIModel)) {
        // Anciens dossiers : relancer la validation 7 points pour restaurer la fiabilité
        this.validateImageWithModel(this.selectedIRMImage);
      }
    } catch {
      this.irmAnalysisResult = {
        isNormal: false,
        diagnosis: record.aiResult,
        confidence: 0,
        detections: [],
        recommendations: record.recommendations || ''
      };
    }
  }

  createPatientForm(): FormGroup {
    return this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s-]{8,}$/)]],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],
      address: [''],
      city: [''],
      region: [''],
      postalCode: [''],
      bloodType: ['UNKNOWN'],
      height: [null],
      weight: [null],
      allergies: [''],
      chronicDiseases: [''],
      currentMedications: [''],
      occupation: [''],
      employer: [''],
      emergencyContactName: ['', Validators.required],
      emergencyContactPhone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s-]{8,}$/)]],
      emergencyContactRelation: ['', Validators.required],
      emergencyContactEmail: [''],
      insuranceProvider: [''],
      insurancePolicyNumber: ['']
    });
  }

  createNewPatient(): void {
    if (this.patientForm.invalid) {
      this.patientForm.markAllAsTouched();
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires du patient.';
      return;
    }

    const formData = this.patientForm.value;
    const safeSplit = (value: string | null | undefined): string[] => {
      if (!value || typeof value !== 'string') return [];
      return value.split(',').map((item: string) => item.trim()).filter((item: string) => item.length > 0);
    };

    const registrationRequest = {
      firstName: formData.firstName || '',
      lastName: formData.lastName || '',
      email: formData.email || '',
      phone: formData.phone || '',
      dateOfBirth: formData.dateOfBirth || '',
      age: formData.dateOfBirth ? this.calculateAge(formData.dateOfBirth) : 0,
      gender: formData.gender || '',
      address: formData.address || '',
      city: formData.city || '',
      region: formData.region || '',
      postalCode: formData.postalCode || '',
      country: formData.country || 'Tunisie',
      bloodType: formData.bloodType || 'UNKNOWN',
      height: formData.height || null,
      weight: formData.weight || null,
      allergies: safeSplit(formData.allergies),
      chronicDiseases: safeSplit(formData.chronicDiseases),
      currentMedications: safeSplit(formData.currentMedications),
      occupation: formData.occupation || '',
      employer: formData.employer || '',
      emergencyContactName: formData.emergencyContactName || '',
      emergencyContactPhone: formData.emergencyContactPhone || '',
      emergencyContactRelation: formData.emergencyContactRelation || '',
      emergencyContactEmail: formData.emergencyContactEmail || '',
      insuranceProvider: formData.insuranceProvider || '',
      insurancePolicyNumber: formData.insurancePolicyNumber || '',
      insuranceCoverageType: 'Standard',
      assignedDoctorId: this.doctorId || ''
    };

    this.saving = true;
    this.medicalRecordService.registerPatient(registrationRequest).subscribe({
      next: (response) => {
        this.patient = response;
        this.patientId = response.id || null;
        this.showPatientForm = false;
        this.saving = false;
        this.successMessage = 'Patient créé avec succès ! Un email de confirmation a été envoyé.';
        if (response.id) {
          this.createEmptyMedicalRecord(response.id);
        }
        setTimeout(() => this.successMessage = '', 4000);
      },
      error: (err) => {
        this.errorMessage = err.error?.error || err.message || 'Erreur lors de la création du patient';
        this.saving = false;
      }
    });
  }

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
    this.medicalRecordService.createRecord(emptyRecord).subscribe({ error: (e) => console.error(e) });
  }

  updateExistingPatient(): void {
    if (this.patientForm.invalid) {
      this.patientForm.markAllAsTouched();
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires du patient.';
      return;
    }

    const formData = this.patientForm.value;
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
      error: () => {
        this.saving = false;
        this.errorMessage = 'Erreur lors de la mise à jour du patient.';
      }
    });
  }

  calculateAge(dateOfBirth: string): number {
    if (!dateOfBirth) return 0;
    const today = new Date();
    const birthDate = new Date(dateOfBirth);
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) age--;
    return age;
  }

  get medications(): FormArray {
    return this.recordForm.get('medications') as FormArray;
  }

  get labTests(): FormArray {
    return this.recordForm.get('labTests') as FormArray;
  }

  addMedication(): void {
    this.medications.push(this.fb.group({
      name: ['', Validators.required],
      dosage: ['', Validators.required],
      frequency: ['', Validators.required],
      duration: ['', Validators.required],
      instructions: ['']
    }));
  }

  removeMedication(index: number): void { this.medications.removeAt(index); }

  addLabTest(): void {
    this.labTests.push(this.fb.group({
      testName: ['', Validators.required],
      result: [''],
      normalRange: [''],
      notes: ['']
    }));
  }

  removeLabTest(index: number): void { this.labTests.removeAt(index); }

  onSubmit(): void {
    if (this.recordForm.invalid) {
      this.markFormGroupTouched(this.recordForm);
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Build AI result string if scan was analyzed
    let aiResult = '';
    if (this.irmAnalysisResult && this.selectedAIModel) {
      aiResult = JSON.stringify({
        model: this.selectedAIModel.id,
        organ: this.selectedAIModel.organ,
        diagnosis: this.irmAnalysisResult.diagnosis,
        confidence: this.irmAnalysisResult.confidence,
        isNormal: this.irmAnalysisResult.isNormal,
        detections: this.irmAnalysisResult.detections,
        recommendations: this.irmAnalysisResult.recommendations,
        stageLabel: this.irmAnalysisResult.stageLabel,
        stageName: this.irmAnalysisResult.stageName,
        probabilities: this.irmAnalysisResult.probabilities,
        gradcamImageBase64: this.irmAnalysisResult.gradcamImageBase64,
        validation: this.imageValidationResult
      });
    }

    const formValue = this.recordForm.value;
    const record: MedicalRecord = {
      ...formValue,
      patientId: this.patientId || 'temp-id',
      doctorId: this.doctorId,
      doctorName: this.doctorName,
      notes: formValue.clinicalNotes || formValue.recommendations || '',
      imageUrl: this.selectedIRMImage || '',
      aiResult: aiResult
    };

    const save$ = this.recordId && !this.isNewConsultation
      ? this.medicalRecordService.updateRecord(this.recordId, record)
      : this.medicalRecordService.createRecord(record);

    save$.subscribe({
      next: (saved) => {
        this.saving = false;
        const wasUpdate = !!this.recordId && !this.isNewConsultation;
        if (saved?.id) {
          this.recordId = saved.id;
          this.isNewConsultation = false;
        }
        this.successMessage = wasUpdate
          ? 'Dossier médical mis à jour avec succès !'
          : 'Dossier médical enregistré avec succès !';
        setTimeout(() => { this.router.navigate(['/doctor/patients']); }, 2000);
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Erreur lors de l\'enregistrement. Veuillez réessayer.';
      }
    });
  }

  getPatientFullName(): string {
    if (!this.patient) return '';
    return `${this.patient.firstName} ${this.patient.lastName}`;
  }

  cancel(): void { this.router.navigate(['/doctor/patients']); }

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

  selectAIModel(model: AiModelDefinition): void {
    if (!this.aiModels.some((m) => m.id === model.id)) {
      this.errorMessage = this.specialtyRestrictionMessage ||
        'Ce modèle IA n\'est pas autorisé pour votre spécialité.';
      return;
    }
    this.selectedAIModel = model;
    this.imageValidationError = '';
    this.imageValidationBlocked = false;
    this.selectedIRMImage = null;
    this.imageValidationResult = null;
  }

  backToModelSelection(): void {
    if (this.canChangeModel && this.aiModels.length > 1) {
      this.selectedAIModel = null;
    }
    this.selectedIRMImage = null;
    this.imageValidationResult = null;
    this.imageValidationError = '';
    this.imageValidationBlocked = false;
  }

  onIRMFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (this.uploadBlockedBySpecialty) {
      this.imageValidationBlocked = true;
      this.imageValidationError = this.specialtyRestrictionMessage;
      return;
    }
    if (input.files && input.files[0] && this.selectedAIModel) {
      if (!isOrganAllowedForSpecialty(this.doctorSpecialty, this.selectedAIModel.organ)) {
        this.imageValidationBlocked = true;
        this.imageValidationError = this.specialtyRestrictionMessage;
        return;
      }
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = (e) => {
        const imageBase64 = e.target?.result as string;
        this.selectedIRMImage = imageBase64;
        this.irmAnalysisResult = null;
        this.validateImageWithModel(imageBase64);
      };
      reader.readAsDataURL(file);
    }
  }

  validateImageWithModel(imageBase64: string): void {
    const model = this.selectedAIModel;
    if (!model) {
      return;
    }

    this.validatingImage = true;
    this.imageValidationError = '';
    this.imageValidationBlocked = false;
    this.imageValidationResult = null;

    const imageFile = this.aiAnalysisService.base64ToFile(imageBase64, 'medical_image.jpg');
    const validation$ = this.isAlzheimerModel(model)
      ? this.aiAnalysisService.validateBrainMri(imageFile)
      : this.aiAnalysisService.validateImage(imageFile, model.organ, this.doctorSpecialty);

    validation$
      .subscribe({
        next: (result: OrganValidationResponse) => {
          this.imageValidationResult = result;
          this.validatingImage = false;

          if (result.valid) {
            this.imageValidationError = '';
            this.imageValidationBlocked = false;
            return;
          }

          this.imageValidationBlocked = true;
          const expectedName = result.expected_organ_name || model.organ;
          const detectedName = result.organ_name || 'Inconnu';
          const isBrainModel = model.organ === 'cerveau' || this.isAlzheimerModel(model);

          if (result.rejection_reason || result.message) {
            this.imageValidationError = result.rejection_reason || result.message || 'IRM cérébrale non validée.';
          } else if (result.reason) {
            this.imageValidationError = result.reason;
            if (isBrainModel && !result.valid && result.confidence < 55) {
              this.imageValidationError +=
                ' Importez une IRM cérébrale axiale (T1/T2) en niveaux de gris, avec le cerveau centré sur fond sombre.';
            }
          } else if (result.organ && result.organ !== 'unknown' && result.organ !== result.expected_organ) {
            this.imageValidationError =
              `Image refusée : organe détecté « ${detectedName} » (${Math.round(result.confidence)}%), ` +
              `attendu « ${expectedName} » pour le modèle ${model.name}. ` +
              `Veuillez importer une image de ${expectedName.toLowerCase()}.`;
          } else {
            this.imageValidationError =
              `Image refusée pour le modèle ${model.name}. ` +
              `Confiance insuffisante (${Math.round(result.confidence)}%). ` +
              (isBrainModel
                ? 'Importez une IRM cérébrale axiale (T1/T2) en niveaux de gris.'
                : `Importez une image de ${expectedName.toLowerCase()}.`);
          }
        },
        error: () => {
          this.validatingImage = false;
          this.imageValidationBlocked = true;
          this.imageValidationError =
            'Validation impossible : les services d\'analyse sont indisponibles. ' +
            'L\'analyse est bloquée tant que l\'image n\'est pas validée.';
        }
      });
  }

  chooseAnotherImage(): void {
    this.selectedIRMImage = null;
    this.imageValidationResult = null;
    this.imageValidationError = '';
    this.imageValidationBlocked = false;
  }

  clearIRMImage(): void {
    this.selectedIRMImage = null;
    this.irmAnalysisResult = null;
    this.analyzingIRM = false;
    this.imageValidationResult = null;
    this.imageValidationError = '';
    this.imageValidationBlocked = false;
  }

  analyzeIRMImage(): void {
    if (!this.canAnalyzeImage || !this.selectedIRMImage || this.uploadBlockedBySpecialty) return;
    if (this.selectedAIModel && !isOrganAllowedForSpecialty(this.doctorSpecialty, this.selectedAIModel.organ)) {
      this.errorMessage = this.specialtyRestrictionMessage;
      return;
    }
    this.analyzingIRM = true;
    this.irmAnalysisResult = null;
    this.errorMessage = '';

    const model = this.selectedAIModel;
    if (model?.id === 'pulmonary-nodule' || model?.organ === 'poumon') {
      const imageFile = this.aiAnalysisService.base64ToFile(this.selectedIRMImage, 'lung_scan.jpg');
      this.aiAnalysisService.analyzeLungCancer(imageFile, this.doctorSpecialty).subscribe({
        next: (result: LungCancerAnalysisResponse) => {
          this.irmAnalysisResult = this.mapLungCancerResult(result);
          this.analyzingIRM = false;
        },
        error: (err) => {
          this.analyzingIRM = false;
          const detail = err?.error?.detail;
          this.errorMessage = typeof detail === 'string'
            ? detail
            : 'Analyse pulmonaire indisponible. Vérifiez que le service IA est démarré et que les poids du modèle sont installés.';
        }
      });
      return;
    }

    if (this.isAlzheimerModel(model)) {
      const imageFile = this.aiAnalysisService.base64ToFile(this.selectedIRMImage, 'brain_mri.jpg');
      this.aiAnalysisService.analyzeAlzheimer(imageFile).subscribe({
        next: (result: AlzheimerAnalysisResponse) => {
          this.irmAnalysisResult = this.mapAlzheimerResult(result);
          this.applyAlzheimerValidationFromAnalysis(result);
          this.analyzingIRM = false;
        },
        error: (err) => {
          this.analyzingIRM = false;
          const detail = err?.error?.detail;
          if (typeof detail === 'object' && detail?.message) {
            this.errorMessage = detail.message;
          } else if (typeof detail === 'string') {
            this.errorMessage = detail;
          } else {
            this.errorMessage =
              'Analyse Alzheimer indisponible. Vérifiez que le service SipDetect (port 8087) est démarré.';
          }
        }
      });
      return;
    }

    setTimeout(() => {
      this.irmAnalysisResult = this.simulateIrmAnalysis();
      this.analyzingIRM = false;
    }, 3000);
  }

  private isAlzheimerModel(model?: AiModelDefinition | null): boolean {
    return model?.id === 'alzheimer-mri';
  }

  private mapAlzheimerResult(result: AlzheimerAnalysisResponse): IRMAnalysisResult {
    const isNormal = result.stage === 0 || result.stage_label === 'CN';
    const detections = (result.affected_zones || []).map((zone: any) => {
      const pct = zone.confidence_pct ?? zone.activation_pct ?? zone.intensity_pct ?? zone.confidence ?? 0;
      const region = zone.region || (typeof zone.zone === 'string' ? zone.zone.replace(/^Zone \d+:\s*/i, '') : 'Région corticale');
      return {
        type: region,
        location: zone.label || 'Activation Grad-CAM maximale',
        confidence: Math.round(Number(pct) || 0),
        severity: (zone.severity as 'low' | 'medium' | 'high') || (pct >= 60 ? 'high' : 'medium')
      };
    });

    return {
      isNormal,
      diagnosis: result.diagnosis,
      confidence: Math.round(result.confidence),
      precision: Math.round(result.confidence),
      detections,
      recommendations: result.recommendations,
      stage: result.stage,
      stageLabel: result.stage_label,
      stageName: result.stage_name,
      gradcamImageBase64: result.gradcam_image_base64,
      probabilities: result.probabilities
    };
  }

  private applyAlzheimerValidationFromAnalysis(result: AlzheimerAnalysisResponse): void {
    if (result.validation) {
      this.imageValidationResult = result.validation;
      this.imageValidationBlocked = !result.validation.valid;
      this.imageValidationError = result.validation.valid
        ? ''
        : (result.validation.rejection_reason || result.validation.message || '');
    }

  getAlzheimerStageInfo(code?: string): AlzheimerStageInfo | undefined {
    if (!code) return undefined;
    return this.alzheimerStages.find(s => s.code === code);
  }

  getOrderedProbabilities(): Array<{ code: string; value: number; info: AlzheimerStageInfo }> {
    const probs = this.irmAnalysisResult?.probabilities || {};
    return this.alzheimerStages
      .map(info => ({
        code: info.code,
        value: Number(probs[info.code] ?? 0),
        info
      }))
      .sort((a, b) => b.value - a.value);
  }

  isPredictedStage(code: string): boolean {
    return this.irmAnalysisResult?.stageLabel === code;
  }

  getAlzheimerReliability(): AlzheimerReliabilityAssessment | null {
    if (!this.isAlzheimerSelected || !this.irmAnalysisResult) {
      return null;
    }

    const confidence = this.irmAnalysisResult.confidence;
    const stageLabel = this.irmAnalysisResult.stageLabel || '';
    const ordered = this.getOrderedProbabilities();
    const hasProbData = ordered.some(entry => entry.value > 0);
    const topProb = hasProbData ? (ordered[0]?.value ?? confidence) : confidence;
    const secondProb = hasProbData ? (ordered[1]?.value ?? 0) : 0;
    const probGap = Math.max(0, topProb - secondProb);

    const validationScore = this.imageValidationResult?.score ?? 0;
    const validationTotal = this.imageValidationResult?.total_checks ?? 7;
    const validationRatio = validationTotal > 0 ? validationScore / validationTotal : 0;
    const hasValidationData = validationScore > 0 || this.imageValidationResult?.valid === true;

    let confidencePts = 0;
    let confidenceStatus: AlzheimerReliabilityFactor['status'] = 'weak';
    if (confidence >= 90) {
      confidencePts = 40;
      confidenceStatus = 'good';
    } else if (confidence >= 80) {
      confidencePts = 34;
      confidenceStatus = 'good';
    } else if (confidence >= 70) {
      confidencePts = 28;
      confidenceStatus = 'ok';
    } else if (confidence >= 60) {
      confidencePts = 20;
      confidenceStatus = 'ok';
    } else if (confidence >= 50) {
      confidencePts = 12;
      confidenceStatus = 'weak';
    }

    let validationPts = 0;
    let validationStatus: AlzheimerReliabilityFactor['status'] = 'weak';
    if (validationScore >= 7) {
      validationPts = 35;
      validationStatus = 'good';
    } else if (validationScore >= 6) {
      validationPts = 30;
      validationStatus = 'good';
    } else if (validationScore >= 5) {
      validationPts = 22;
      validationStatus = 'ok';
    } else if (validationScore >= 4) {
      validationPts = 14;
      validationStatus = 'ok';
    } else if (validationScore >= 3) {
      validationPts = 8;
      validationStatus = 'weak';
    }

    let gapPts = 0;
    let gapStatus: AlzheimerReliabilityFactor['status'] = 'weak';
    if (probGap >= 50) {
      gapPts = 25;
      gapStatus = 'good';
    } else if (probGap >= 35) {
      gapPts = 20;
      gapStatus = 'good';
    } else if (probGap >= 20) {
      gapPts = 14;
      gapStatus = 'ok';
    } else if (probGap >= 10) {
      gapPts = 8;
      gapStatus = 'weak';
    }

    let composite = confidencePts + validationPts + gapPts;

    if ((stageLabel === 'CN' || stageLabel === 'VMCI') && probGap < 25 && hasProbData) {
      composite -= 10;
    }
    if (stageLabel === 'VMCI' && confidence < 80) {
      composite -= 5;
    }
    if (hasValidationData && !this.imageValidationResult?.valid) {
      composite -= 15;
    }

    composite = Math.max(0, Math.min(100, composite));

    let level: AlzheimerReliabilityLevel;
    let label: string;
    let summary: string;
    let advice: string;

    if (composite >= 75) {
      level = 'high';
      label = 'Élevée';
      summary = 'La prédiction est cohérente : confiance forte, image validée et stade nettement dominant.';
      advice = 'Résultat exploitable en aide à la décision. Confirmer avec le contexte clinique du patient.';
    } else if (composite >= 50) {
      level = 'moderate';
      label = 'Modérée';
      summary = 'La prédiction est plausible mais comporte des signaux d\'incertitude.';
      advice = 'Interpréter avec prudence. Un avis neurologique ou un examen complémentaire est recommandé.';
    } else {
      level = 'low';
      label = 'Faible';
      summary = 'La prédiction est incertaine (confiance basse, image limite ou stades proches).';
      advice = 'Ne pas fonder de décision thérapeutique sur ce seul résultat. Refaire l\'acquisition ou demander un second avis.';
    }

    const factors: AlzheimerReliabilityFactor[] = [
      {
        id: 'confidence',
        label: 'Confiance modèle',
        detail: `${confidence} % pour ${stageLabel}`,
        score: confidencePts,
        maxScore: 40,
        status: confidenceStatus
      },
      {
        id: 'validation',
        label: 'Validation IRM (7 pts)',
        detail: validationScore > 0
          ? `${validationScore}/${validationTotal} critères (${Math.round(validationRatio * 100)} %)`
          : (this.validatingImage ? 'Validation en cours…' : 'Non disponible'),
        score: validationPts,
        maxScore: 35,
        status: validationStatus
      },
      {
        id: 'gap',
        label: 'Écart entre stades',
        detail: hasProbData
          ? `${probGap.toFixed(1)} pts (${stageLabel} vs ${ordered[1]?.code ?? '—'})`
          : 'Probabilités non enregistrées — ré-analyser pour recalculer',
        score: gapPts,
        maxScore: 25,
        status: gapStatus
      }
    ];

    return { level, label, score: composite, summary, advice, factors };
  }

  formatCriteriaTag(tag: string): string {
    const raw = tag.replace(/^[✓✗]\s*/, '');
    const label = this.criteriaLabels[raw] || raw.replace(/_/g, ' ');
    const icon = tag.startsWith('✓') ? '✓' : '✗';
    return `${icon} ${label}`;
  }

  private mapLungCancerResult(result: LungCancerAnalysisResponse): IRMAnalysisResult {
    return {
      isNormal: result.isNormal,
      diagnosis: result.diagnosis,
      confidence: Math.round(result.confidence),
      detections: (result.detections || []).map((det) => ({
        type: det.type,
        location: det.location,
        confidence: Math.round(det.confidence),
        severity: det.severity
      })),
      recommendations: result.recommendations,
      stage: result.stage,
      stageLabel: result.stage_label,
      stageName: result.stage_name,
      gradcamImageBase64: result.gradcam_image_base64,
      probabilities: result.probabilities
    };
  }

  private simulateIrmAnalysis(): IRMAnalysisResult {
    const specialty = this.doctorSpecialty || '';
    const model = this.selectedAIModel;

    // Specialty-specific simulation
    if (model?.id === 'pulmonary-nodule' || specialty === 'Pneumologie') {
      const hasNodule = Math.random() > 0.5;
      return hasNodule ? {
        isNormal: false,
        diagnosis: 'Nodule pulmonaire suspect détecté',
        confidence: Math.floor(Math.random() * 15) + 78,
        detections: [{ type: 'Nodule pulmonaire', location: 'Lobe supérieur droit', confidence: 84, severity: 'medium' }],
        recommendations: 'Scanner thoracique de contrôle dans 3 mois recommandé. Avis en pneumo-oncologie conseillé.'
      } : {
        isNormal: true,
        diagnosis: 'Scanner Pulmonaire Normal — Aucun Nodule Détecté',
        confidence: Math.floor(Math.random() * 10) + 88,
        detections: [],
        recommendations: 'Contrôle annuel recommandé. Surveillance tabagique le cas échéant.'
      };
    }

    if (model?.id === 'alzheimer-mri' || model?.id === 'brain-tumor-3d' || specialty === 'Neurologie') {
      const stage = Math.random();
      if (stage > 0.6) {
        return {
          isNormal: false,
          diagnosis: model?.id === 'alzheimer-mri' ? 'Signes modérés de démence Alzheimer (stade MCI)' : 'Lésion cérébrale détectée',
          confidence: Math.floor(Math.random() * 12) + 75,
          detections: [{ type: model?.id === 'alzheimer-mri' ? 'Atrophie hippocampique' : 'Masse suspecte', location: 'Région temporale gauche', confidence: 78, severity: 'high' }],
          recommendations: 'Consultation neurologique urgente recommandée. Tests cognitifs complémentaires nécessaires.'
        };
      }
      return {
        isNormal: true,
        diagnosis: 'IRM Cérébrale Normale',
        confidence: Math.floor(Math.random() * 8) + 90,
        detections: [],
        recommendations: 'Suivi annuel recommandé.'
      };
    }

    if (model?.id === 'cardiac-mri' || specialty === 'Cardiologie') {
      return {
        isNormal: Math.random() > 0.4,
        diagnosis: Math.random() > 0.4 ? 'Fonction cardiaque normale' : 'Fraction d\'éjection légèrement réduite (45%)',
        confidence: Math.floor(Math.random() * 10) + 82,
        detections: Math.random() > 0.4 ? [] : [{ type: 'Réduction FE', location: 'Ventricule gauche', confidence: 79, severity: 'medium' }],
        recommendations: Math.random() > 0.4 ? 'Contrôle cardiologique annuel.' : 'Echocardiographie complémentaire recommandée. Avis cardiologique.'
      };
    }

    // Default
    const isNormal = Math.random() > 0.3;
    return {
      isNormal,
      diagnosis: isNormal ? 'IRM Normale' : 'Anomalies détectées',
      confidence: Math.floor(Math.random() * 20) + 70,
      detections: isNormal ? [] : [{ type: 'Lésion', location: 'Zone à préciser', confidence: 75, severity: 'medium' }],
      recommendations: isNormal ? 'Aucune anomalie. Suite des soins standard.' : 'Consultation spécialisée recommandée.'
    };
  }

  addIRMToClinicalNotes(): void {
    if (!this.irmAnalysisResult) return;
    const currentNotes = this.recordForm.get('clinicalNotes')?.value || '';
    const irmReport = this.generateIRMReport();
    const newNotes = currentNotes ? `${currentNotes}\n\n--- Analyse IRM ---\n${irmReport}` : `--- Analyse IRM ---\n${irmReport}`;
    this.recordForm.patchValue({ clinicalNotes: newNotes });
    this.successMessage = 'Rapport IRM ajouté aux notes cliniques !';
    setTimeout(() => this.successMessage = '', 3000);
  }

  private generateIRMReport(): string {
    if (!this.irmAnalysisResult) return '';
    const result = this.irmAnalysisResult;
    let report = `Date: ${new Date().toLocaleDateString()}\n`;
    report += `Modèle IA: ${this.selectedAIModel?.name || 'Non spécifié'}\n`;
    report += `Spécialité: ${this.doctorSpecialty || 'Non spécifiée'}\n`;
    report += `Diagnostic: ${result.diagnosis}\n`;
    if (result.stageLabel) {
      report += `Stade: ${result.stageLabel} (${result.stageName})\n`;
    }
    report += `Confiance IA: ${result.confidence}%\n`;
    if (result.detections && result.detections.length > 0) {
      report += '\nDétections:\n';
      result.detections.forEach((det, i) => {
        report += `  ${i + 1}. ${det.type} - ${det.location} (${det.confidence}%)\n`;
      });
    }
    report += `\nRecommandations: ${result.recommendations}`;
    return report;
  }
}
