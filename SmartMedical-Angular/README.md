# SmartMedical - Angular Frontend

Projet Angular complet avec Frontoffice (site public) et Backoffice (administration) pour la gestion médicale.

## Structure du Projet

```
SmartMedical-Angular/
├── src/
│   ├── app/
│   │   ├── app.component.ts          # Composant racine
│   │   ├── app.config.ts             # Configuration app (standalone)
│   │   ├── app.routes.ts             # Routes principales
│   │   ├── core/                     # Services, Guards, Interceptors, Models
│   │   │   ├── components/           # Login, Register, 404
│   │   │   ├── guards/               # AuthGuard, AdminGuard
│   │   │   ├── interceptors/         # AuthInterceptor, ErrorInterceptor
│   │   │   ├── models/               # User, Patient interfaces
│   │   │   └── services/             # AuthService, ApiService, PatientService
│   │   ├── frontoffice/              # Site public (patients)
│   │   │   ├── components/           # Navbar, Footer, Layout
│   │   │   ├── pages/                # Home, Services, Doctors, About, Contact, Appointment
│   │   │   └── frontoffice.routes.ts # Routes frontoffice
│   │   └── backoffice/               # Administration
│   │       ├── components/           # Sidebar, Header, Layout
│   │       ├── pages/                # Dashboard, Patients, Appointments, Doctors, Users, Settings
│   │       └── backoffice.routes.ts  # Routes backoffice
│   ├── environments/                 # Configuration environnements
│   └── styles.scss                   # Styles globaux
├── angular.json                      # Configuration Angular
├── package.json                      # Dépendances
├── proxy.conf.json                   # Proxy pour backend
└── tsconfig.json                     # Configuration TypeScript
```

## Fonctionnalités

### Frontoffice (Site Public)
- **Page d'accueil** : Présentation des services et statistiques
- **Services** : Liste des spécialités médicales
- **Médecins** : Présentation de l'équipe médicale
- **À propos** : Informations sur la clinique
- **Contact** : Formulaire de contact
- **Rendez-vous** : Prise de rendez-vous en ligne
- **Authentification** : Connexion / Inscription

### Backoffice (Administration)
- **Tableau de bord** : Statistiques et aperçu
- **Gestion des patients** : Liste, recherche, CRUD
- **Gestion des rendez-vous** : Planning et validation
- **Gestion des médecins** : Équipe médicale
- **Gestion des utilisateurs** : Administration des comptes
- **Paramètres** : Profil et sécurité

## Prérequis

- Node.js (v18+)
- npm ou yarn
- Angular CLI

## Installation

1. **Installer les dépendances** :
```bash
cd SmartMedical-Angular
npm install
```

2. **Configurer le backend** :
Modifiez `src/environments/environment.ts` pour pointer vers votre backend :
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  gatewayUrl: 'http://localhost:8080',
  // ... autres URLs des microservices
};
```

3. **Lancer l'application** :
```bash
# Mode développement
npm start

# Ou avec le proxy
npm run serve
```

L'application sera disponible sur `http://localhost:4200`

## Architecture Backend

Le projet est configuré pour fonctionner avec votre architecture microservices Spring Boot :

- **Gateway** : `localhost:8080`
- **Auth Service** : `localhost:8081`
- **Patient Service** : `localhost:8083`
- **Medical Service** : `localhost:8084`
- **Scan Service** : `localhost:8085`
- **User Service** : `localhost:8086`

## Routes Principales

| Route | Description |
|-------|-------------|
| `/` | Redirection vers frontoffice |
| `/login` | Page de connexion |
| `/register` | Page d'inscription |
| `/frontoffice` | Site public (accueil) |
| `/frontoffice/services` | Services médicaux |
| `/frontoffice/doctors` | Équipe médicale |
| `/frontoffice/appointment` | Prendre rendez-vous |
| `/admin` | Dashboard administration (protégé) |
| `/admin/patients` | Gestion patients |
| `/admin/appointments` | Gestion rendez-vous |
| `/admin/doctors` | Gestion médecins |
| `/admin/users` | Gestion utilisateurs |

## Guards et Sécurité

- **AuthGuard** : Vérifie si l'utilisateur est authentifié
- **AdminGuard** : Vérifie si l'utilisateur a le rôle ADMIN
- **AuthInterceptor** : Ajoute le token JWT aux requêtes HTTP
- **ErrorInterceptor** : Gère les erreurs 401/403

## Technologies Utilisées

- Angular 19 (Standalone Components)
- Bootstrap 5
- Bootstrap Icons
- RxJS
- TypeScript
- SCSS

## Build Production

```bash
npm run build
```

Les fichiers seront générés dans `dist/smartmedical-angular/`.

## Intégration avec Templates Existants

Vous pouvez intégrer vos templates existants :
- **Frontoffice** : Template MediLab dans `frontoffice/MediLab-1.0.0/`
- **Backoffice** : Template Mazer dans `Bckoffice/mazer-1.0.0/`

Copiez les assets (CSS, images, fonts) dans `src/assets/` et adaptez les composants Angular.

## Développement

### Ajouter une nouvelle page

1. Créer le composant dans le dossier approprié (`frontoffice/pages/` ou `backoffice/pages/`)
2. Ajouter la route dans le fichier de routes correspondant
3. Le composant sera automatiquement chargé via lazy loading

### Connecter un nouveau service backend

1. Créer le service dans `src/app/core/services/`
2. Injecter `HttpClient` et utiliser l'`ApiService` comme référence
3. Utiliser le service dans les composants avec injection de dépendances

## Support

Pour toute question ou problème, consultez la documentation Angular ou contactez l'équipe de développement.
