import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DoctorService, Doctor } from '../../../core/services/doctor.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-appointment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './appointment.component.html',
  styleUrl: './appointment.component.scss'
})
export class AppointmentComponent implements OnInit {
  appointmentForm: FormGroup;
  isSubmitting = false;
  submitSuccess = false;
  errorMessage = '';
  doctors: { id: string; name: string; specialty: string }[] = [];

  timeSlots = [
    '08:00', '08:30', '09:00', '09:30', '10:00', '10:30',
    '11:00', '11:30', '14:00', '14:30', '15:00', '15:30',
    '16:00', '16:30', '17:00', '17:30'
  ];

  minDate = new Date().toISOString().split('T')[0];

  constructor(
    private fb: FormBuilder,
    private doctorService: DoctorService,
    private appointmentService: AppointmentService,
    private authService: AuthService
  ) {
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

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user?.email) {
      const name = [user.firstName, user.lastName].filter(Boolean).join(' ');
      this.appointmentForm.patchValue({
        email: user.email,
        patientName: name || ''
      });
    }
    this.loadDoctors();
  }

  loadDoctors(): void {
    this.doctorService.getAllDoctors().subscribe({
      next: (docs: Doctor[]) => {
        this.doctors = docs
          .filter(d => d.active !== false)
          .map(d => ({
            id: d.id || '',
            name: `Dr. ${d.firstName} ${d.lastName}`,
            specialty: d.specialization
          }));
      },
      error: () => {
        this.doctors = [];
      }
    });
  }

  onSubmit(): void {
    if (this.appointmentForm.invalid) return;
    this.isSubmitting = true;
    this.errorMessage = '';
    this.submitSuccess = false;

    const v = this.appointmentForm.value;
    this.appointmentService.create({
      patientName: v.patientName,
      patientEmail: v.email,
      patientPhone: v.phone,
      doctorId: v.doctorId,
      appointmentDate: v.date,
      appointmentTime: v.time,
      reason: v.reason
    }).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.submitSuccess = true;
        this.appointmentForm.reset();
        const user = this.authService.getCurrentUser();
        if (user?.email) {
          this.appointmentForm.patchValue({ email: user.email });
        }
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.error || 'Erreur lors de la réservation';
      }
    });
  }
}
