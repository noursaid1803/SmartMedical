import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-appointment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './appointment.component.html',
  styleUrl: './appointment.component.scss'
})
export class AppointmentComponent {
  appointmentForm: FormGroup;
  isSubmitting = false;
  submitSuccess = false;
  
  doctors = [
    { id: 1, name: 'Dr. Martin Dupont', specialty: 'Cardiologue' },
    { id: 2, name: 'Dr. Sophie Bernard', specialty: 'Neurologue' },
    { id: 3, name: 'Dr. Pierre Lefebvre', specialty: 'Orthopédiste' },
    { id: 4, name: 'Dr. Marie Moreau', specialty: 'Ophtalmologue' },
    { id: 5, name: 'Dr. Jean Petit', specialty: 'Radiologue' },
    { id: 6, name: 'Dr. Claire Roux', specialty: 'Médecin généraliste' }
  ];
  
  timeSlots = [
    '08:00', '08:30', '09:00', '09:30', '10:00', '10:30',
    '11:00', '11:30', '14:00', '14:30', '15:00', '15:30',
    '16:00', '16:30', '17:00', '17:30'
  ];

  constructor(private fb: FormBuilder) {
    this.appointmentForm = this.fb.group({
      patientName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.required],
      doctorId: ['', Validators.required],
      date: ['', Validators.required],
      time: ['', Validators.required],
      reason: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  onSubmit(): void {
    if (this.appointmentForm.valid) {
      this.isSubmitting = true;
      // TODO: Implement appointment submission
      setTimeout(() => {
        this.isSubmitting = false;
        this.submitSuccess = true;
        this.appointmentForm.reset();
      }, 2000);
    }
  }
}
