import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AdminService, AdminUser } from '../../../core/services/admin.service';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  admins: AdminUser[] = [];
  loading = false;
  showForm = false;
  errorMessage = '';
  successMessage = '';
  adminForm: FormGroup;

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder
  ) {
    this.adminForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  ngOnInit(): void {
    this.loadAdmins();
  }

  loadAdmins(): void {
    this.loading = true;
    this.adminService.getAllAdmins().subscribe({
      next: (admins) => {
        this.admins = admins;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.admins = [];
      }
    });
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.adminForm.reset();
      this.errorMessage = '';
    }
  }

  onSubmit(): void {
    if (this.adminForm.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.adminService.createAdmin(this.adminForm.value).subscribe({
      next: (res: any) => {
        this.successMessage = res.message || 'Admin créé. Un code de vérification a été envoyé par email.';
        this.loading = false;
        this.adminForm.reset();
        this.showForm = false;
        this.loadAdmins();
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Erreur lors de la création';
        this.loading = false;
      }
    });
  }

  getStatusLabel(admin: AdminUser): string {
    return admin.verified ? 'actif' : 'en attente';
  }
}
