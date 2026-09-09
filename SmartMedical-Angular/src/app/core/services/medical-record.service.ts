import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Patient {
  // Informations de base
  id?: string;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  age?: number;
  gender?: 'M' | 'F' | 'OTHER';

  // Adresse et localisation
  address?: string;
  city?: string;
  region?: string;
  postalCode?: string;
  country?: string;

  // Informations médicales
  bloodType?: 'A+' | 'A-' | 'B+' | 'B-' | 'AB+' | 'AB-' | 'O+' | 'O-' | 'UNKNOWN';
  height?: number; // en cm
  weight?: number; // en kg
  allergies?: string[];
  chronicDiseases?: string[];
  currentMedications?: string[];

  // Informations professionnelles
  occupation?: string;
  employer?: string;

  // Contact d'urgence (obligatoire)
  emergencyContact: {
    name: string;
    phone: string;
    relation: string;
    email?: string;
  };

  // Assurance
  insuranceInfo?: {
    provider: string;
    policyNumber: string;
    coverageType?: string;
  };

  // Historique
  createdAt?: Date;
  updatedAt?: Date;
  lastVisitDate?: Date;
}

export interface MedicalRecord {
  id?: string;
  patientId: string;
  doctorId: string;
  doctorName?: string;

  // Consultation
  consultationDate: string;
  reasonForVisit: string;
  symptoms: string;

  // Examen physique
  bloodPressure?: string;
  heartRate?: string | number;
  temperature?: string | number;
  weight?: string | number;
  height?: string | number;
  oxygenSaturation?: string | number;

  // Diagnostic
  diagnosis: string;
  severity: 'low' | 'medium' | 'high' | 'critical';

  // Traitement
  medications?: {
    name: string;
    dosage: string;
    frequency: string;
    duration: string;
    instructions?: string;
  }[];

  // Tests et examens
  labTests?: {
    testName: string;
    result?: string;
    normalRange?: string;
    notes?: string;
  }[];

  // Notes
  clinicalNotes?: string;
  notes?: string;
  recommendations?: string;
  followUpRequired?: boolean;
  followUpDate?: string;

  // Documents
  imageUrl?: string;
  aiResult?: string;
  attachments?: string[];

  // Timestamps
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MedicalRecordService {
  private apiUrl = `${environment.gatewayUrl}/medical`;
  private patientsUrl = `${environment.gatewayUrl}/patients`;

  constructor(private http: HttpClient) {}

  // Patients
  getAllPatients(): Observable<Patient[]> {
    return this.http.get<Patient[]>(this.patientsUrl);
  }

  getPatientById(id: string): Observable<Patient> {
    return this.http.get<Patient>(`${this.patientsUrl}/${id}`);
  }

  createPatient(patient: Patient): Observable<Patient> {
    return this.http.post<Patient>(this.patientsUrl, patient);
  }

  registerPatient(patientData: any): Observable<any> {
    return this.http.post<any>(`${this.patientsUrl}/register`, patientData);
  }

  searchPatients(query: string): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.patientsUrl}/search?q=${query}`);
  }

  // Medical Records
  getAllRecords(): Observable<MedicalRecord[]> {
    return this.http.get<MedicalRecord[]>(this.apiUrl);
  }

  getRecordById(id: string): Observable<MedicalRecord> {
    return this.http.get<MedicalRecord>(`${this.apiUrl}/${id}`);
  }

  getRecordsByPatient(patientId: string): Observable<MedicalRecord[]> {
    return this.http.get<MedicalRecord[]>(`${this.apiUrl}/patient/${patientId}`);
  }

  getRecordsByDoctor(doctorId: string): Observable<MedicalRecord[]> {
    return this.http.get<MedicalRecord[]>(`${this.apiUrl}/doctor/${doctorId}`);
  }

  createRecord(record: MedicalRecord): Observable<MedicalRecord> {
    return this.http.post<MedicalRecord>(this.apiUrl, record);
  }

  updateRecord(id: string, record: MedicalRecord): Observable<MedicalRecord> {
    return this.http.put<MedicalRecord>(`${this.apiUrl}/${id}`, record);
  }

  deleteRecord(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ==========================================
  // GESTION DES PATIENTS - MISE À JOUR
  // ==========================================

  updatePatient(id: string, patient: Patient): Observable<Patient> {
    return this.http.put<any>(`${this.patientsUrl}/${id}`, patient).pipe(
      map(res => (res?.patient ? res.patient : res) as Patient)
    );
  }

  deletePatient(id: string): Observable<void> {
    return this.http.delete<void>(`${this.patientsUrl}/${id}`);
  }
}
