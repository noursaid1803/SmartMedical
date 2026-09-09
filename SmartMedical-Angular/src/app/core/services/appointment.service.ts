import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Appointment {
  id?: string;
  patientId?: string;
  patientName?: string;
  patientEmail?: string;
  patientPhone?: string;
  doctorId?: string;
  doctorName?: string;
  doctorSpecialty?: string;
  appointmentDate?: string;
  appointmentTime?: string;
  reason?: string;
  status?: string;
  createdAt?: string;
}

export interface AppointmentRequest {
  patientId?: string;
  patientName: string;
  patientEmail: string;
  patientPhone: string;
  doctorId: string;
  appointmentDate: string;
  appointmentTime: string;
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private apiUrl = `${environment.gatewayUrl}/appointments`;

  constructor(private http: HttpClient) {}

  create(request: AppointmentRequest): Observable<any> {
    return this.http.post(this.apiUrl, request);
  }

  getAll(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(this.apiUrl);
  }

  getByPatientEmail(email: string): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apiUrl}/patient/${encodeURIComponent(email)}`);
  }

  updateStatus(id: string, status: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apiUrl}/${id}/status`, { status });
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
