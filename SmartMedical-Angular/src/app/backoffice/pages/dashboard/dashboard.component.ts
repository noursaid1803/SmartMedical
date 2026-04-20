import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  today = new Date();
  stats = [
    { title: 'Total Patients', value: '1,234', icon: 'bi-people', color: 'primary', trend: '+12%' },
    { title: 'Rendez-vous', value: '89', icon: 'bi-calendar-check', color: 'success', trend: '+5%' },
    { title: 'Médecins', value: '24', icon: 'bi-heart-pulse', color: 'info', trend: '+2' },
    { title: 'Revenus', value: '45,678 €', icon: 'bi-currency-euro', color: 'warning', trend: '+8%' }
  ];

  recentAppointments = [
    { patient: 'Jean Dupont', doctor: 'Dr. Martin', date: '2024-04-18', time: '09:00', status: 'confirmé' },
    { patient: 'Marie Curie', doctor: 'Dr. Sophie', date: '2024-04-18', time: '10:30', status: 'en attente' },
    { patient: 'Pierre Lefebvre', doctor: 'Dr. Bernard', date: '2024-04-18', time: '14:00', status: 'confirmé' },
    { patient: 'Sophie Martin', doctor: 'Dr. Martin', date: '2024-04-19', time: '09:30', status: 'confirmé' }
  ];
}
