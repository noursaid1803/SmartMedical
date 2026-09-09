import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ProfileDTO {
  id?: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  verified?: boolean;
  phone?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  bio?: string;
  linkedin?: string;
  website?: string;
  specialty?: string;
  specialtyCode?: string;
  licenseNumber?: string;
  yearsExperience?: number;
  consultationFee?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private apiUrl = `${environment.gatewayUrl}/profile`;

  constructor(private http: HttpClient) {}

  getMyProfile(): Observable<ProfileDTO> {
    return this.http.get<ProfileDTO>(`${this.apiUrl}/me`);
  }

  updateMyProfile(profile: ProfileDTO): Observable<ProfileDTO> {
    return this.http.put<ProfileDTO>(`${this.apiUrl}/me`, profile);
  }
}
