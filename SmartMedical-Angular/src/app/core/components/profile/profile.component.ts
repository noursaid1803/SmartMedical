import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ProfileService, ProfileDTO } from '../../services/profile.service';
import { User } from '../../models/user.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  profileForm: FormGroup;
  passwordForm: FormGroup;
  
  loading = false;
  savingProfile = false;
  savingPassword = false;
  uploadingPhoto = false;
  
  successMessage = '';
  errorMessage = '';
  
  activeTab: 'info' | 'password' | 'security' = 'info';
  
  // Photo de profil
  profileImage: string | null = null;
  defaultAvatar = 'assets/images/default-avatar.png';
  
  // Pour afficher les rôles en français
  roleLabels: { [key: string]: string } = {
    'ADMIN': 'Administrateur',
    'DOCTOR': 'Médecin',
    'USER': 'Utilisateur'
  };
  
  // Spécialités médicales (pour les médecins)
  specialties = [
    'Médecine générale',
    'Cardiologie',
    'Dermatologie',
    'Pédiatrie',
    'Gynécologie',
    'Neurologie',
    'Ophtalmologie',
    'ORL',
    'Psychiatrie',
    'Radiologie',
    'Chirurgie',
    'Urgences'
  ];

  private apiUrl = `${environment.apiUrl}/profile`;

  constructor(
    private fb: FormBuilder,
    public authService: AuthService,
    public router: Router,
    private http: HttpClient,
    private profileService: ProfileService
  ) {
    this.profileForm = this.createProfileForm();
    this.passwordForm = this.createPasswordForm();
  }

  ngOnInit(): void {
    // Charger la photo du localStorage
    const storedPhoto = localStorage.getItem('profilePhoto');
    if (storedPhoto) {
      this.profileImage = storedPhoto;
    }
    this.loadUserInfo();
  }

  createProfileForm(): FormGroup {
    return this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.pattern(/^[+]?[0-9\s-]{8,}$/)],
      address: [''],
      city: [''],
      postalCode: [''],
      bio: [''],
      // Champs spécifiques médecin
      specialty: [''],
      licenseNumber: [''],
      yearsExperience: [''],
      consultationFee: [''],
      // Réseaux sociaux
      linkedin: [''],
      website: ['']
    });
  }

  createPasswordForm(): FormGroup {
    return this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup): { [key: string]: boolean } | null {
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    if (newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  loadUserInfo(): void {
    // Essayer d'abord de récupérer depuis le localStorage pour l'email
    const storedUser = localStorage.getItem('user');
    let userData: any = null;
    
    if (storedUser) {
      userData = JSON.parse(storedUser);
    }
    
    if (!userData && !this.authService.getCurrentUser()) {
      this.router.navigate(['/login']);
      return;
    }

    // Récupérer les données complètes depuis l'API backend
    this.loading = true;
    this.profileService.getMyProfile().subscribe({
      next: (profile: ProfileDTO) => {
        console.log('Profile loaded from API:', profile);
        this.user = profile as User;
        
        // Mettre à jour le localStorage avec les données complètes
        localStorage.setItem('user', JSON.stringify(profile));
        
        // Pré-remplir le formulaire avec les infos du backend
        this.profileForm.patchValue({
          firstName: profile.firstName || '',
          lastName: profile.lastName || '',
          email: profile.email || '',
          phone: profile.phone || '',
          address: profile.address || '',
          city: profile.city || '',
          postalCode: profile.postalCode || '',
          bio: profile.bio || '',
          specialty: profile.specialty || '',
          licenseNumber: profile.licenseNumber || '',
          yearsExperience: profile.yearsExperience || '',
          consultationFee: profile.consultationFee || '',
          linkedin: profile.linkedin || '',
          website: profile.website || ''
        });
        
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading profile:', error);
        // Fallback sur les données du localStorage
        if (userData) {
          this.user = userData;
          this.profileForm.patchValue({
            firstName: userData.firstName || userData.firstname || '',
            lastName: userData.lastName || userData.lastname || '',
            email: userData.email || ''
          });
        }
        this.loading = false;
      }
    });
  }

  // Méthode pour obtenir le nom complet pour l'affichage
  getDisplayName(): string {
    // D'abord essayer le formulaire (valeurs pré-remplies)
    const formFirstName = this.profileForm?.get('firstName')?.value;
    const formLastName = this.profileForm?.get('lastName')?.value;
    if (formFirstName || formLastName) {
      const fullName = `${formFirstName || ''} ${formLastName || ''}`.trim();
      if (fullName) return fullName;
    }
    
    // Sinon essayer l'objet user
    if (!this.user) return 'Administrateur';
    const userAny: any = this.user;
    const firstName = userAny.firstName || userAny.firstname || '';
    const lastName = userAny.lastName || userAny.lastname || '';
    const fullName = `${firstName} ${lastName}`.trim();
    return fullName || userAny.username || userAny.email?.split('@')[0] || 'Administrateur';
  }

  getUserRole(): string {
    // Gérer role (string) ou roles (tableau)
    const userAny: any = this.user;
    let role: string | undefined;
    
    if (userAny?.role) {
      role = userAny.role; // Format string du backend
    } else if (userAny?.roles?.length) {
      role = userAny.roles[0]; // Format tableau legacy
    }
    
    if (!role) return 'Utilisateur';
    return this.roleLabels[role] || role;
  }

  isAdmin(): boolean {
    const userAny: any = this.user;
    if (userAny?.role === 'ADMIN') return true;
    return userAny?.roles?.includes('ADMIN') || false;
  }

  isDoctor(): boolean {
    const userAny: any = this.user;
    if (userAny?.role === 'DOCTOR') return true;
    return userAny?.roles?.includes('DOCTOR') || false;
  }

  onUpdateProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      this.errorMessage = 'Veuillez corriger les erreurs du formulaire.';
      return;
    }

    this.savingProfile = true;
    this.errorMessage = '';
    this.successMessage = '';

    const profileData: ProfileDTO = {
      ...this.profileForm.value,
      email: this.user?.email // Garder l'email original
    };

    this.profileService.updateMyProfile(profileData).subscribe({
      next: (updatedProfile) => {
        this.savingProfile = false;
        this.user = updatedProfile as User;
        localStorage.setItem('user', JSON.stringify(updatedProfile));
        this.successMessage = 'Profil mis à jour avec succès !';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error) => {
        this.savingProfile = false;
        this.errorMessage = 'Erreur lors de la mise à jour du profil.';
        console.error('Update profile error:', error);
      }
    });
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      this.errorMessage = 'Veuillez corriger les erreurs du formulaire.';
      return;
    }

    this.savingPassword = true;
    this.errorMessage = '';
    this.successMessage = '';

    // TODO: Appeler l'API pour changer le mot de passe
    setTimeout(() => {
      this.savingPassword = false;
      this.successMessage = 'Mot de passe changé avec succès !';
      this.passwordForm.reset();
      setTimeout(() => this.successMessage = '', 3000);
    }, 1000);
  }

  getBackRoute(): string {
    if (this.isAdmin()) return '/admin';
    if (this.isDoctor()) return '/doctor';
    return '/frontoffice';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // Gestion de la photo de profil
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      // Vérifier le type et la taille
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Veuillez sélectionner une image';
        return;
      }
      
      if (file.size > 5 * 1024 * 1024) {
        this.errorMessage = 'L\'image ne doit pas dépasser 5 Mo';
        return;
      }

      this.uploadingPhoto = true;
      
      const reader = new FileReader();
      reader.onload = (e) => {
        this.profileImage = e.target?.result as string;
        // Sauvegarder la photo dans localStorage
        localStorage.setItem('profilePhoto', this.profileImage);
        this.uploadingPhoto = false;
        this.successMessage = 'Photo mise à jour avec succès !';
        setTimeout(() => this.successMessage = '', 3000);
      };
      reader.readAsDataURL(file);
    }
  }

  removePhoto(): void {
    this.profileImage = null;
    localStorage.removeItem('profilePhoto');
    this.successMessage = 'Photo supprimée';
    setTimeout(() => this.successMessage = '', 3000);
  }

  getInitials(): string {
    if (!this.user) return '??';
    const first = this.user.firstName?.[0] || '';
    const last = this.user.lastName?.[0] || '';
    return (first + last).toUpperCase() || '??';
  }

  getAvatarUrl(): string {
    return this.profileImage || this.defaultAvatar;
  }

  hasPhoto(): boolean {
    return !!this.profileImage;
  }

  // Indicateur de force du mot de passe
  getPasswordStrength(): number {
    const password = this.passwordForm.get('newPassword')?.value || '';
    let strength = 0;
    
    if (password.length >= 8) strength += 25;
    if (password.match(/[a-z]+/)) strength += 25;
    if (password.match(/[A-Z]+/)) strength += 25;
    if (password.match(/[0-9]+/)) strength += 25;
    
    return strength;
  }

  getPasswordStrengthText(): string {
    const strength = this.getPasswordStrength();
    if (strength === 0) return 'Très faible';
    if (strength <= 25) return 'Faible';
    if (strength <= 50) return 'Moyen';
    if (strength <= 75) return 'Fort';
    return 'Très fort';
  }
}
