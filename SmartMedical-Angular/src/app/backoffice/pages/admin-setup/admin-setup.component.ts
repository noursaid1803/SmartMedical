import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-admin-setup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-setup.component.html',
  styleUrl: './admin-setup.component.scss'
})
export class AdminSetupComponent {
  message: string = '';
  error: string = '';
  loading: boolean = false;
  setupComplete: boolean = false;

  // Pour la vérification
  verificationEmail: string = 'arijhedhri4@gmail.com';
  verificationCode: string = '';
  verifying: boolean = false;
  verified: boolean = false;

  constructor(private http: HttpClient) {}

  setupPrimaryAdmin() {
    this.loading = true;
    this.error = '';
    this.message = '';

    this.http.post(`${environment.gatewayUrl}/admin/setup-primary`, {})
      .subscribe({
        next: (response: any) => {
          this.message = response.message;
          this.setupComplete = true;
          this.loading = false;
          console.log('Admin créé:', response);
        },
        error: (err) => {
          this.error = err.error?.error || 'Erreur lors de la création de l\'admin';
          this.loading = false;
          console.error('Erreur:', err);
        }
      });
  }

  // Créer un admin personnalisé
  createAdmin(name: string, email: string, password: string) {
    this.loading = true;
    this.error = '';
    this.message = '';

    const request = { name, email, password };

    this.http.post(`${environment.apiUrl}/admin/create`, request)
      .subscribe({
        next: (response: any) => {
          this.message = response.message;
          this.verificationEmail = email;
          this.setupComplete = true;
          this.loading = false;
        },
        error: (err) => {
          this.error = err.error?.error || 'Erreur lors de la création';
          this.loading = false;
        }
      });
  }

  // Vérifier le code
  verifyCode() {
    if (!this.verificationCode) {
      this.error = 'Veuillez saisir le code de vérification';
      return;
    }

    this.verifying = true;
    this.error = '';

    const request = {
      email: this.verificationEmail,
      code: this.verificationCode
    };

    this.http.post(`${environment.apiUrl}/admin/verify`, request)
      .subscribe({
        next: (response: any) => {
          this.message = response.message;
          this.verified = true;
          this.verifying = false;
        },
        error: (err) => {
          this.error = err.error?.error || 'Code invalide';
          this.verifying = false;
        }
      });
  }

  // Renvoyer le code
  resendCode() {
    this.loading = true;
    this.error = '';

    this.http.post(`${environment.apiUrl}/admin/resend-code`, { email: this.verificationEmail })
      .subscribe({
        next: (response: any) => {
          this.message = response.message;
          this.loading = false;
        },
        error: (err) => {
          this.error = err.error?.error || 'Erreur lors du renvoi';
          this.loading = false;
        }
      });
  }
}
