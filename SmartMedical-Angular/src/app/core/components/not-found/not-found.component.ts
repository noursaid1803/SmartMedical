import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="not-found-container text-center py-5">
      <div class="container">
        <h1 class="display-1 text-primary">404</h1>
        <h2 class="mb-4">Page non trouvée</h2>
        <p class="text-muted mb-4">La page que vous recherchez n'existe pas.</p>
        <a routerLink="/frontoffice" class="btn btn-primary">
          <i class="bi bi-house-door"></i> Retour à l'accueil
        </a>
      </div>
    </div>
  `,
  styles: [`
    .not-found-container {
      min-height: 60vh;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .display-1 {
      font-size: 6rem;
      font-weight: 700;
    }
  `]
})
export class NotFoundComponent {}
