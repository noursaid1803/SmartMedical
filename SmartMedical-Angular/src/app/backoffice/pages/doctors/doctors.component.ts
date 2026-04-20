import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-doctors',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './doctors.component.html',
  styleUrl: './doctors.component.scss'
})
export class DoctorsComponent {
  doctors = [
    { id: 1, name: 'Dr. Martin Dupont', specialty: 'Cardiologie', email: 'martin@smartmedical.fr', phone: '0123456781', patients: 156 },
    { id: 2, name: 'Dr. Sophie Bernard', specialty: 'Neurologie', email: 'sophie@smartmedical.fr', phone: '0123456782', patients: 98 },
    { id: 3, name: 'Dr. Pierre Lefebvre', specialty: 'Orthopédie', email: 'pierre@smartmedical.fr', phone: '0123456783', patients: 134 },
    { id: 4, name: 'Dr. Marie Moreau', specialty: 'Ophtalmologie', email: 'marie@smartmedical.fr', phone: '0123456784', patients: 87 },
    { id: 5, name: 'Dr. Jean Petit', specialty: 'Radiologie', email: 'jean@smartmedical.fr', phone: '0123456785', patients: 45 }
  ];
}
