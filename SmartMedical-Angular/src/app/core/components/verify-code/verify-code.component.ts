import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-verify-code',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './verify-code.component.html',
  styleUrl: './verify-code.component.scss'
})
export class VerifyCodeComponent implements OnInit {
  verifyForm: FormGroup;
  isLoading: boolean = false;
  errorMessage: string = '';
  email: string = '';
  countdown: number = 30 * 60; // 30 minutes in seconds
  timerInterval: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.verifyForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      if (!this.email) {
        this.router.navigate(['/forgot-password']);
      }
    });

    this.startCountdown();
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  startCountdown(): void {
    this.timerInterval = setInterval(() => {
      if (this.countdown > 0) {
        this.countdown--;
      } else {
        clearInterval(this.timerInterval);
      }
    }, 1000);
  }

  getFormattedTime(): string {
    const minutes = Math.floor(this.countdown / 60);
    const seconds = this.countdown % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  onSubmit(): void {
    if (this.verifyForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';

      const code = this.verifyForm.value.code;

      this.authService.verifyResetCode(this.email, code).subscribe({
        next: (response) => {
          this.isLoading = false;
          // Rediriger vers la page de réinitialisation
          this.router.navigate(['/reset-password'], {
            queryParams: { email: this.email, code: code }
          });
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Code invalide ou expiré.';
        }
      });
    }
  }

  resendCode(): void {
    this.authService.requestPasswordReset(this.email).subscribe({
      next: () => {
        this.countdown = 30 * 60; // Reset countdown
        this.errorMessage = '';
        alert('Un nouveau code a été envoyé à votre email.');
      },
      error: (error) => {
        this.errorMessage = 'Erreur lors de l\'envoi du code. Veuillez réessayer.';
      }
    });
  }
}
