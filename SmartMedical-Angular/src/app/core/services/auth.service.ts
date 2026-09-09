import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.gatewayUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    const token = localStorage.getItem('token');
    const user = localStorage.getItem('user');
    if (token && user) {
      this.currentUserSubject.next(JSON.parse(user));
    }
  }

  login(credentials: LoginRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap(response => {
          if (response?.needsVerification) {
            return;
          }
          if (response?.needsPasswordChange) {
            if (response.tempToken) {
              localStorage.setItem('token', response.tempToken);
            }
            const pendingUser = {
              email: response.email,
              roles: ['DOCTOR'],
              verified: true,
              needsPasswordChange: true
            };
            localStorage.setItem('user', JSON.stringify(pendingUser));
            this.currentUserSubject.next(pendingUser as User);
            return;
          }
          if (!response?.token) {
            return;
          }
          localStorage.setItem('token', response.token);
          localStorage.setItem('user', JSON.stringify(response.user));
          this.currentUserSubject.next(response.user);
        })
      );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data)
      .pipe(
        tap(response => {
          localStorage.setItem('token', response.token);
          localStorage.setItem('user', JSON.stringify(response.user));
          this.currentUserSubject.next(response.user);
        })
      );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const user: any = this.currentUserSubject.value;
    if (user?.role === 'ADMIN') return true;
    return user?.roles?.includes('ADMIN') ?? false;
  }

  isDoctor(): boolean {
    const user: any = this.currentUserSubject.value;
    if (user?.role === 'DOCTOR') return true;
    return user?.roles?.includes('DOCTOR') ?? false;
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  // === Password Reset Methods ===

  requestPasswordReset(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/password-reset/request`, { email });
  }

  verifyResetCode(email: string, code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/password-reset/verify`, { email, code });
  }

  resetPassword(email: string, code: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/password-reset/reset`, { email, code, newPassword });
  }
}
