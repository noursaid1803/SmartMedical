import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdminUser {
  id?: string;
  name?: string;
  email: string;
  role?: string;
  verified?: boolean;
  createdAt?: string;
}

export interface CreateAdminRequest {
  name: string;
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private apiUrl = `${environment.gatewayUrl}/admin`;

  constructor(private http: HttpClient) {}

  getAllAdmins(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.apiUrl}/list`);
  }

  createAdmin(request: CreateAdminRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/create`, request);
  }

  verifyAdmin(email: string, code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/verify`, { email, code });
  }

  resendCode(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/resend-code`, { email });
  }
}
