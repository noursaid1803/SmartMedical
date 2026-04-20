import { Routes } from '@angular/router';
import { DoctorDashboardComponent } from './pages/doctor-dashboard/doctor-dashboard.component';
import { PatientRecordsComponent } from './pages/patient-records/patient-records.component';
import { MedicalRecordFormComponent } from './pages/medical-record-form/medical-record-form.component';
import { DoctorLayoutComponent } from './components/layout/doctor-layout.component';

export const DOCTOR_ROUTES: Routes = [
  {
    path: '',
    component: DoctorLayoutComponent,
    children: [
      {
        path: '',
        component: DoctorDashboardComponent
      },
      {
        path: 'patients',
        component: PatientRecordsComponent
      },
      {
        path: 'medical-record/:patientId',
        component: MedicalRecordFormComponent
      },
      {
        path: 'medical-record',
        component: MedicalRecordFormComponent
      }
    ]
  }
];
