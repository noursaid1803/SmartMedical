import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Doctor, DoctorRequest, DoctorService } from '../../../core/services/doctor.service';

@Component({
  selector: 'app-doctors-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctors-management.component.html',
  styleUrl: './doctors-management.component.scss'
})
export class DoctorsManagementComponent implements OnInit {
  doctors: Doctor[] = [];
  doctorForm: FormGroup;
  isEditing = false;
  editingId: string | null = null;
  showForm = false;
  loading = false;
  errorMessage = '';
  successMessage = '';

  // Options pour les champs
  specializations = [
    'Cardiologie', 'Dermatologie', 'Endocrinologie', 'Gastroentérologie',
    'Gynécologie', 'Neurologie', 'Ophtalmologie', 'Orthopédie',
    'Pédiatrie', 'Psychiatrie', 'Radiologie', 'Urologie', 'Autre'
  ];

  daysOfWeek = [
    { value: 'Lundi', label: 'Lundi' },
    { value: 'Mardi', label: 'Mardi' },
    { value: 'Mercredi', label: 'Mercredi' },
    { value: 'Jeudi', label: 'Jeudi' },
    { value: 'Vendredi', label: 'Vendredi' },
    { value: 'Samedi', label: 'Samedi' },
    { value: 'Dimanche', label: 'Dimanche' }
  ];

  constructor(
    private doctorService: DoctorService,
    private fb: FormBuilder
  ) {
    this.doctorForm = this.createForm();
  }

  ngOnInit(): void {
    this.loadDoctors();
  }

  createForm(): FormGroup {
    return this.fb.group({
      // Informations personnelles
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      address: [''],

      // Informations professionnelles
      specialization: ['', Validators.required],
      licenseNumber: ['', Validators.required],
      education: [''],
      experience: [''],
      hospital: [''],
      department: [''],

      // Détails du cabinet
      consultationFee: [''],
      consultationDuration: ['30'],
      availableDays: [[]],
      startTime: ['09:00'],
      endTime: ['17:00'],

      // Bio et description
      bio: [''],
      languages: ['Français'],

      // Mot de passe (optionnel pour l'édition)
      password: ['']
    });
  }

  loadDoctors(): void {
    this.loading = true;
    this.doctorService.getAllDoctors().subscribe({
      next: (doctors) => {
        this.doctors = doctors;
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = 'Erreur lors du chargement des médecins';
        this.loading = false;
        console.error('Erreur:', error);
      }
    });
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.resetForm();
    }
  }

  onSubmit(): void {
    if (this.doctorForm.invalid) {
      this.markFormGroupTouched(this.doctorForm);
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const doctorData: DoctorRequest = this.doctorForm.value;

    if (this.isEditing && this.editingId) {
      // Mise à jour
      this.doctorService.updateDoctor(this.editingId, doctorData).subscribe({
        next: (response) => {
          this.successMessage = 'Médecin mis à jour avec succès !';
          this.loading = false;
          this.resetForm();
          this.loadDoctors();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          this.errorMessage = error.error?.error || 'Erreur lors de la mise à jour';
          this.loading = false;
        }
      });
    } else {
      // Création
      this.doctorService.createDoctor(doctorData).subscribe({
        next: (response) => {
          this.successMessage = 'Médecin créé avec succès ! Un email de confirmation a été envoyé.';
          this.loading = false;
          this.resetForm();
          this.loadDoctors();
          setTimeout(() => this.successMessage = '', 5000);
        },
        error: (error) => {
          this.errorMessage = error.error?.error || 'Erreur lors de la création';
          this.loading = false;
        }
      });
    }
  }

  editDoctor(doctor: Doctor): void {
    this.isEditing = true;
    this.editingId = doctor.id || null;
    this.showForm = true;

    this.doctorForm.patchValue({
      firstName: doctor.firstName,
      lastName: doctor.lastName,
      email: doctor.email,
      phone: doctor.phone,
      address: doctor.address,
      specialization: doctor.specialization,
      licenseNumber: doctor.licenseNumber,
      education: doctor.education,
      experience: doctor.experience,
      hospital: doctor.hospital,
      department: doctor.department,
      consultationFee: doctor.consultationFee,
      consultationDuration: doctor.consultationDuration,
      availableDays: doctor.availableDays || [],
      startTime: doctor.startTime,
      endTime: doctor.endTime,
      bio: doctor.bio,
      languages: doctor.languages,
      password: '' // Ne pas pré-remplir le mot de passe
    });

    // Faire défiler vers le formulaire
    setTimeout(() => {
      document.getElementById('doctorForm')?.scrollIntoView({ behavior: 'smooth' });
    }, 100);
  }

  deleteDoctor(id: string): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce médecin ?')) {
      this.loading = true;
      this.doctorService.deleteDoctor(id).subscribe({
        next: () => {
          this.successMessage = 'Médecin supprimé avec succès !';
          this.loading = false;
          this.loadDoctors();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          this.errorMessage = 'Erreur lors de la suppression';
          this.loading = false;
        }
      });
    }
  }

  toggleStatus(id: string): void {
    this.loading = true;
    this.doctorService.toggleDoctorStatus(id).subscribe({
      next: () => {
        this.successMessage = 'Statut mis à jour !';
        this.loading = false;
        this.loadDoctors();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.errorMessage = 'Erreur lors du changement de statut';
        this.loading = false;
      }
    });
  }

  resetForm(): void {
    this.doctorForm.reset({
      consultationDuration: '30',
      startTime: '09:00',
      endTime: '17:00',
      languages: 'Français',
      availableDays: []
    });
    this.isEditing = false;
    this.editingId = null;
  }

  cancelEdit(): void {
    this.resetForm();
    this.showForm = false;
  }

  // Helper pour marquer tous les champs comme touchés
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }

  // Toggle jour disponible
  toggleDay(day: string): void {
    const currentDays = this.doctorForm.get('availableDays')?.value || [];
    const index = currentDays.indexOf(day);

    if (index > -1) {
      currentDays.splice(index, 1);
    } else {
      currentDays.push(day);
    }

    this.doctorForm.get('availableDays')?.setValue([...currentDays]);
  }

  isDaySelected(day: string): boolean {
    const days = this.doctorForm.get('availableDays')?.value || [];
    return days.includes(day);
  }
}
