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

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      
      this.authService.login(this.loginForm.value).subscribe({
        next: () => {
          this.isLoading = false;
          const user = this.authService.getCurrentUser();

          // Vérifier si l'utilisateur doit vérifier son compte
          if (user?.roles?.includes('DOCTOR') && !user?.verified) {
            // Médecin non vérifié -> redirection vers page de vérification
            this.router.navigate(['/doctor-verify']);
          } else if (user?.roles?.includes('ADMIN')) {
            this.router.navigate(['/admin']);
          } else if (user?.roles?.includes('DOCTOR') && user?.verified) {
            this.router.navigate(['/doctor']);
          } else {
            this.router.navigate(['/frontoffice']);
          }
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Erreur de connexion';
        }
      });
    }
  }
}
