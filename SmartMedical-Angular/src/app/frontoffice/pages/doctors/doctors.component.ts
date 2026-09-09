import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { IMAGES } from '../../../core/constants/images.constant';

@Component({
  selector: 'app-doctors',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './doctors.component.html',
  styleUrl: './doctors.component.scss'
})
export class DoctorsComponent {
  doctors = [
    { 
      id: 1, 
      name: 'Dr. Martin Dupont', 
      specialty: 'Cardiologie', 
      email: 'martin@smartmedical.fr', 
      phone: '0123456781', 
      image: IMAGES.doctors[0],
      description: '15 ans d\'expérience en cardiologie interventionnelle. Spécialiste des maladies cardiovasculaires.'
    },
    { 
      id: 2, 
      name: 'Dr. Sophie Bernard', 
      specialty: 'Neurologie', 
      email: 'sophie@smartmedical.fr', 
      phone: '0123456782', 
      image: IMAGES.doctors[1],
      description: 'Spécialiste en troubles du sommeil et épilepsie. Expert en neurophysiologie.'
    },
    { 
      id: 3, 
      name: 'Dr. Pierre Lefebvre', 
      specialty: 'Orthopédie', 
      email: 'pierre@smartmedical.fr', 
      phone: '0123456783', 
      image: IMAGES.doctors[2],
      description: 'Expert en chirurgie du genou et de la hanche. Spécialiste en traumatologie du sport.'
    },
    { 
      id: 4, 
      name: 'Dr. Marie Moreau', 
      specialty: 'Ophtalmologie', 
      email: 'marie@smartmedical.fr', 
      phone: '0123456784', 
      image: IMAGES.doctors[3],
      description: 'Spécialiste en chirurgie réfractive et cataracte. Expert en chirurgie laser.'
    }
  ];
}
