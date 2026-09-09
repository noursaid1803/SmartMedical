import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Appointment, AppointmentService } from '../../../core/services/appointment.service';

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.scss'
})
export class AppointmentsComponent implements OnInit {
  appointments: Appointment[] = [];
  loading = false;

  constructor(private appointmentService: AppointmentService) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.loading = true;
    this.appointmentService.getAll().subscribe({
      next: (list) => {
        this.appointments = list;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  updateStatus(id: string | undefined, status: string): void {
    if (!id) return;
    this.appointmentService.updateStatus(id, status).subscribe({
      next: () => this.loadAppointments()
    });
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'CONFIRMED': return 'badge-medical-success';
      case 'CANCELLED': return 'badge-medical-danger';
      case 'COMPLETED': return 'badge-medical-info';
      default: return 'badge-medical-warning';
    }
  }

  getStatusLabel(status?: string): string {
    const map: Record<string, string> = {
      PENDING: 'En attente',
      CONFIRMED: 'Confirmé',
      CANCELLED: 'Annulé',
      COMPLETED: 'Terminé'
    };
    return map[status || 'PENDING'] || status || 'En attente';
  }
}
