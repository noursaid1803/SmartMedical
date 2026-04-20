import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface MenuItem {
  path: string;
  icon: string;
  label: string;
  badge?: number;
}

@Component({
  selector: 'app-doctor-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './doctor-sidebar.component.html',
  styleUrls: ['./doctor-sidebar.component.scss']
})
export class DoctorSidebarComponent {
  @Input() collapsed = false;

  menuItems: MenuItem[] = [
    { path: '/doctor', icon: 'bi-speedometer2', label: 'Tableau de bord' },
    { path: '/doctor/patients', icon: 'bi-people', label: 'Mes Patients', badge: 0 },
    { path: '/doctor/medical-record', icon: 'bi-file-medical', label: 'Nouveau Dossier' },
    { path: '/doctor/appointments', icon: 'bi-calendar-check', label: 'Rendez-vous', badge: 0 },
    { path: '/doctor/medical-history', icon: 'bi-clock-history', label: 'Historique' },
    { path: '/profile', icon: 'bi-person-circle', label: 'Mon Profil' }
  ];

  doctorName = 'Docteur';
  doctorSpecialty = 'Médecin';

  constructor(public authService: AuthService) {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.doctorName = `Dr. ${user.firstName || ''} ${user.lastName || ''}`.trim();
    }
  }
}
