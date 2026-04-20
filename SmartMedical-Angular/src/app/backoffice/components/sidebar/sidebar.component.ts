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
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  @Input() collapsed = false;
  @Input() mobileOpen = false;

  menuItems: MenuItem[] = [
    { path: '/admin/dashboard', icon: 'bi-speedometer2', label: 'Tableau de bord' },
    { path: '/admin/patients', icon: 'bi-people', label: 'Patients' },
    { path: '/admin/appointments', icon: 'bi-calendar-check', label: 'Rendez-vous' },
    { path: '/admin/doctors', icon: 'bi-heart-pulse', label: 'Médecins' },
    { path: '/admin/users', icon: 'bi-person-gear', label: 'Utilisateurs' },
    { path: '/admin/admin-setup', icon: 'bi-shield-plus', label: 'Nouvel Admin' },
    { path: '/profile', icon: 'bi-person-circle', label: 'Mon Profil' },
    { path: '/admin/settings', icon: 'bi-gear', label: 'Paramètres' }
  ];

  constructor(public authService: AuthService) {}

  closeMobile() {
    this.mobileOpen = false;
  }
}
