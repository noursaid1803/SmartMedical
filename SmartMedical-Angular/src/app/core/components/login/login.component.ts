import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = '';
  isLoading: boolean = false;
  showPassword: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      
      this.authService.login(this.loginForm.value).subscribe({
        next: (response: any) => {
          this.isLoading = false;

          if (response?.needsVerification) {
            this.router.navigate(['/doctor-verify'], { queryParams: { email: response.email } });
            return;
          }

          if (response?.needsPasswordChange) {
            this.router.navigate(['/doctor-verify'], {
              queryParams: { email: response.email, step: 'change-password' }
            });
            return;
          }

          const user = this.authService.getCurrentUser();
          const roles = user?.roles || [(user as any)?.role].filter(Boolean);

          if (roles.includes('ADMIN')) {
            this.router.navigate(['/admin']);
          } else if (roles.includes('DOCTOR') && !user?.verified) {
            this.router.navigate(['/doctor-verify']);
          } else if (roles.includes('DOCTOR')) {
            this.router.navigate(['/doctor']);
          } else if (roles.includes('PATIENT')) {
            this.router.navigate(['/frontoffice/dashboard']);
          } else {
            this.router.navigate(['/frontoffice/dashboard']);
          }
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || error.error?.error || 'Email ou mot de passe incorrect';
        }
      });
    }
  }
}
