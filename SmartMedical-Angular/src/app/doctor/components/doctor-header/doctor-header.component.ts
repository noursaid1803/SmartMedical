import { Component, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-doctor-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './doctor-header.component.html',
  styleUrls: ['./doctor-header.component.scss']
})
export class DoctorHeaderComponent implements OnInit {
  @Output() toggleSidebar = new EventEmitter<void>();
  
  doctorName = 'Docteur';
  currentDate = new Date();
  notifications = [
    { id: 1, message: 'Nouveau rendez-vous confirmé', time: '10 min', type: 'success' },
    { id: 2, message: 'Dossier patient mis à jour', time: '1h', type: 'info' },
    { id: 3, message: 'Rappel: Consultation à 14h', time: '2h', type: 'warning' }
  ];
  showNotifications = false;
  unreadCount = 2;

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.doctorName = `Dr. ${user.firstName || ''} ${user.lastName || ''}`.trim();
    }
  }

  onToggleSidebar(): void {
    this.toggleSidebar.emit();
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.unreadCount = 0;
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
