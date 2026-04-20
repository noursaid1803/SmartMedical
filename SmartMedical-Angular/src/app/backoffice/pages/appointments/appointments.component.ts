import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.scss'
})
export class AppointmentsComponent {
  appointments = [
    { id: 1, patient: 'Jean Dupont', doctor: 'Dr. Martin', date: '2024-04-18', time: '09:00', status: 'confirmé', type: 'Consultation' },
    { id: 2, patient: 'Marie Curie', doctor: 'Dr. Sophie', date: '2024-04-18', time: '10:30', status: 'en attente', type: 'Contrôle' },
    { id: 3, patient: 'Pierre Lefebvre', doctor: 'Dr. Bernard', date: '2024-04-18', time: '14:00', status: 'confirmé', type: 'Radiographie' },
    { id: 4, patient: 'Sophie Martin', doctor: 'Dr. Martin', date: '2024-04-19', time: '09:30', status: 'confirmé', type: 'Consultation' },
    { id: 5, patient: 'Lucas Petit', doctor: 'Dr. Sophie', date: '2024-04-19', time: '11:00', status: 'annulé', type: 'Bilan' }
  ];
}
