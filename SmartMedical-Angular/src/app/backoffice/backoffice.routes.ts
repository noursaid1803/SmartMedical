import { Routes } from '@angular/router';
import { BackofficeLayoutComponent } from './components/layout/layout.component';

export const BACKOFFICE_ROUTES: Routes = [
  {
    path: '',
    component: BackofficeLayoutComponent,
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'patients',
        loadComponent: () => import('./pages/patients/patients.component').then(m => m.PatientsComponent)
      },
      {
        path: 'appointments',
        loadComponent: () => import('./pages/appointments/appointments.component').then(m => m.AppointmentsComponent)
      },
      {
        path: 'doctors',
        loadComponent: () => import('./pages/doctors-management/doctors-management.component').then(m => m.DoctorsManagementComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./pages/users/users.component').then(m => m.UsersComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent)
      },
      {
        path: 'admin-setup',
        loadComponent: () => import('./pages/admin-setup/admin-setup.component').then(m => m.AdminSetupComponent)
      }
    ]
  }
];
