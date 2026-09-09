import { Routes } from '@angular/router';
import { FrontofficeLayoutComponent } from './components/layout/layout.component';
import { authGuard } from '../core/guards/auth.guard';

export const FRONTOFFICE_ROUTES: Routes = [
  {
    path: '',
    component: FrontofficeLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent)
      },
      {
        path: 'services',
        loadComponent: () => import('./pages/services/services.component').then(m => m.ServicesComponent)
      },
      {
        path: 'doctors',
        loadComponent: () => import('./pages/doctors/doctors.component').then(m => m.DoctorsComponent)
      },
      {
        path: 'about',
        loadComponent: () => import('./pages/about/about.component').then(m => m.AboutComponent)
      },
      {
        path: 'contact',
        loadComponent: () => import('./pages/contact/contact.component').then(m => m.ContactComponent)
      },
      {
        path: 'appointment',
        loadComponent: () => import('./pages/appointment/appointment.component').then(m => m.AppointmentComponent)
      },
      {
        path: 'dashboard',
        canActivate: [authGuard],
        loadComponent: () => import('./pages/patient-dashboard/patient-dashboard.component').then(m => m.PatientDashboardComponent)
      }
    ]
  }
];
