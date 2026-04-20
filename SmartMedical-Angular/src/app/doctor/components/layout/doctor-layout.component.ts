import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { DoctorSidebarComponent } from '../doctor-sidebar/doctor-sidebar.component';
import { DoctorHeaderComponent } from '../doctor-header/doctor-header.component';

@Component({
  selector: 'app-doctor-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, DoctorSidebarComponent, DoctorHeaderComponent],
  templateUrl: './doctor-layout.component.html',
  styleUrls: ['./doctor-layout.component.scss']
})
export class DoctorLayoutComponent {
  sidebarCollapsed = false;

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }
}
