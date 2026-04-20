import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { IMAGES } from '../../../core/constants/images.constant';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  images = IMAGES;

  services = [
    { icon: 'bi-heart-pulse', title: 'Cardiologie', description: 'Diagnostic et traitement des maladies cardiaques' },
    { icon: 'bi-brain', title: 'Neurologie', description: 'Soins spécialisés du système nerveux' },
    { icon: 'bi-bone', title: 'Orthopédie', description: 'Traitement des troubles musculo-squelettiques' },
    { icon: 'bi-eye', title: 'Ophtalmologie', description: 'Soins complets de la vision' },
    { icon: 'bi-activity', title: 'Radiologie', description: 'Imagerie médicale de pointe' },
    { icon: 'bi-droplet', title: 'Laboratoire', description: 'Analyses biologiques complètes' }
  ];

  testimonials = [
    { name: 'Jean Dupont', role: 'Patient', image: IMAGES.testimonials[0], text: 'Excellent service médical, personnel très professionnel et attentionné.' },
    { name: 'Marie Curie', role: 'Patient', image: IMAGES.testimonials[1], text: 'Une expérience exceptionnelle, les médecins sont à l\'écoute et compétents.' },
    { name: 'Pierre Martin', role: 'Patient', image: IMAGES.testimonials[2], text: 'Modernité et efficacité, je recommande vivement cette clinique.' }
  ];

  gallery = IMAGES.gallery;
}
