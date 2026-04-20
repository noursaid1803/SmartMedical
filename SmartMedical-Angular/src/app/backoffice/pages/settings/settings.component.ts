import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent {
  profileForm: FormGroup;
  passwordForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.profileForm = this.fb.group({
      firstName: ['Admin'],
      lastName: ['System'],
      email: ['admin@smartmedical.fr'],
      phone: ['0123456789']
    });

    this.passwordForm = this.fb.group({
      currentPassword: [''],
      newPassword: [''],
      confirmPassword: ['']
    });
  }

  onProfileSubmit(): void {
    // TODO: Implement profile update
    console.log('Profile updated:', this.profileForm.value);
  }

  onPasswordSubmit(): void {
    // TODO: Implement password change
    console.log('Password changed');
  }
}
