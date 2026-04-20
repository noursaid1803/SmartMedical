import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-doctor-verify',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctor-verify.component.html',
  styleUrl: './doctor-verify.component.scss'
})
export class DoctorVerifyComponent implements OnInit {
  step: 'verify' | 'change-password' = 'verify';
  verifyForm: FormGroup;
  passwordForm: FormGroup;
  loading = false;
  errorMessage = '';
  successMessage = '';
  doctorEmail = '';
  verificationCode = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private http: HttpClient,
    private router: Router
  ) {
    this.verifyForm = this.fb.group({
      code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validator: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (!user) {
      this.router.navigate(['/login']);
      return;
    }

    // Si déjà vérifié, rediriger vers le dashboard
    if (user.verified) {
      this.router.navigate(['/doctor']);
      return;
    }

    this.doctorEmail = user.email || '';
  }

  passwordMatchValidator(form: FormGroup): { [key: string]: boolean } | null {
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    if (newPassword !== confirmPassword) {
      return { 'passwordMismatch': true };
    }
    return null;
  }

  onVerify(): void {
    if (this.verifyForm.invalid) {
      this.errorMessage = 'Veuillez entrer un code de vérification valide (6 chiffres)';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const code = this.verifyForm.get('code')?.value;

    // Appel API pour vérifier le code
    this.http.post(`${environment.gatewayUrl}/doctors/verify`, {
      email: this.doctorEmail,
      code: code
    }).subscribe({
      next: (response: any) => {
        this.loading = false;
        if (response.success) {
          this.verificationCode = code;
          this.successMessage = 'Code vérifié avec succès ! Veuillez maintenant changer votre mot de passe.';
          this.step = 'change-password';
        } else {
          this.errorMessage = response.message || 'Code de vérification invalide';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Erreur lors de la vérification';
      }
    });
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) {
      if (this.passwordForm.hasError('passwordMismatch')) {
        this.errorMessage = 'Les mots de passe ne correspondent pas';
      } else {
        this.errorMessage = 'Veuillez remplir tous les champs correctement';
      }
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { currentPassword, newPassword } = this.passwordForm.value;

    // Appel API pour changer le mot de passe
    this.http.post(`${environment.gatewayUrl}/doctors/change-password`, {
      email: this.doctorEmail,
      currentPassword: currentPassword,
      newPassword: newPassword
    }).subscribe({
      next: (response: any) => {
        this.loading = false;
        if (response.success) {
          this.successMessage = 'Mot de passe changé avec succès ! Redirection...';

          // Mettre à jour le statut verified dans le localStorage
          const user = this.authService.getCurrentUser();
          if (user) {
            user.verified = true;
            localStorage.setItem('user', JSON.stringify(user));
          }

          // Rediriger vers le dashboard médecin après 2 secondes
          setTimeout(() => {
            this.router.navigate(['/doctor']);
          }, 2000);
        } else {
          this.errorMessage = response.message || 'Erreur lors du changement de mot de passe';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Erreur lors du changement de mot de passe';
      }
    });
  }

  resendCode(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post(`${environment.gatewayUrl}/doctors/resend-code`, {
      email: this.doctorEmail
    }).subscribe({
      next: (response: any) => {
        this.loading = false;
        if (response.success) {
          this.successMessage = 'Un nouveau code a été envoyé à votre email !';
        } else {
          this.errorMessage = response.message || 'Erreur lors du renvoi du code';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Erreur lors du renvoi du code';
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
