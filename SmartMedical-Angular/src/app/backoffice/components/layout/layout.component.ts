import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-backoffice-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent, HeaderComponent],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class BackofficeLayoutComponent {
  sidebarCollapsed = false;
  mobileSidebarOpen = false;
  isMobile = false;

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.checkMobile();
  }

  ngOnInit() {
    this.checkMobile();
  }

  checkMobile() {
    this.isMobile = window.innerWidth < 992;
    if (!this.isMobile) {
      this.mobileSidebarOpen = false;
    }
  }

  toggleSidebar(): void {
    if (this.isMobile) {
      this.mobileSidebarOpen = !this.mobileSidebarOpen;
    } else {
      this.sidebarCollapsed = !this.sidebarCollapsed;
    }
  }

  closeMobileSidebar(): void {
    this.mobileSidebarOpen = false;
  }
}
