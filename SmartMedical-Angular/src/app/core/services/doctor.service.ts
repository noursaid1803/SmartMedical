import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Doctor {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  address?: string;
  specialization: string;
  specialtyCode?: string;
  licenseNumber: string;
  education?: string;
  experience?: string;
  hospital?: string;
  department?: string;
  consultationFee?: string;
  consultationDuration?: string;
  availableDays?: string[];
  startTime?: string;
  endTime?: string;
  bio?: string;
  languages?: string;
  profileImage?: string;
  active?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface DoctorRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  address?: string;
  specialization: string;
  specialtyCode?: string;
  licenseNumber: string;
  education?: string;
  experience?: string;
  hospital?: string;
  department?: string;
  consultationFee?: string;
  consultationDuration?: string;
  availableDays?: string[];
  startTime?: string;
  endTime?: string;
  bio?: string;
  languages?: string;
  password?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DoctorService {
  private apiUrl = `${environment.gatewayUrl}/doctors`;

  constructor(private http: HttpClient) {}

  getAllDoctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(this.apiUrl);
  }

  getActiveDoctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(`${this.apiUrl}/active`);
  }

  getDoctorById(id: string): Observable<Doctor> {
    return this.http.get<Doctor>(`${this.apiUrl}/${id}`);
  }

  createDoctor(doctor: DoctorRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}`, doctor);
  }

  updateDoctor(id: string, doctor: DoctorRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, doctor);
  }

  toggleDoctorStatus(id: string): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  deleteDoctor(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  resendCredentials(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/resend-credentials`, { email });
  }

  getDoctorsBySpecialization(specialization: string): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(`${this.apiUrl}/specialization/${specialization}`);
  }

  getDoctorsByHospital(hospital: string): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(`${this.apiUrl}/hospital/${hospital}`);
  }
}
