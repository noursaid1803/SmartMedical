import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { doctorGuard } from './core/guards/doctor.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/frontoffice',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./core/components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'doctor-verify',
    loadComponent: () => import('./core/components/doctor-verify/doctor-verify.component').then(m => m.DoctorVerifyComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./core/components/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./core/components/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'verify-code',
    loadComponent: () => import('./core/components/verify-code/verify-code.component').then(m => m.VerifyCodeComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./core/components/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'frontoffice',
    loadChildren: () => import('./frontoffice/frontoffice.routes').then(m => m.FRONTOFFICE_ROUTES)
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadChildren: () => import('./backoffice/backoffice.routes').then(m => m.BACKOFFICE_ROUTES)
  },
  {
    path: 'doctor',
    canActivate: [authGuard, doctorGuard],
    loadChildren: () => import('./doctor/doctor.routes').then(m => m.DOCTOR_ROUTES)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./core/components/profile/profile.component').then(m => m.ProfileComponent)
  },
  {
    path: '**',
    loadComponent: () => import('./core/components/not-found/not-found.component').then(m => m.NotFoundComponent)
  }
];
