import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent {
  users = [
    { id: 1, name: 'Admin System', email: 'admin@smartmedical.fr', role: 'ADMIN', status: 'actif', lastLogin: '2024-04-18 09:30' },
    { id: 2, name: 'Jean Dupont', email: 'jean@email.com', role: 'PATIENT', status: 'actif', lastLogin: '2024-04-18 08:15' },
    { id: 3, name: 'Marie Curie', email: 'marie@email.com', role: 'PATIENT', status: 'actif', lastLogin: '2024-04-17 14:20' },
    { id: 4, name: 'Dr. Martin', email: 'martin@smartmedical.fr', role: 'DOCTOR', status: 'actif', lastLogin: '2024-04-18 07:45' },
    { id: 5, name: 'Pierre Lefebvre', email: 'pierre@email.com', role: 'PATIENT', status: 'inactif', lastLogin: '2024-04-10 11:00' }
  ];
}
