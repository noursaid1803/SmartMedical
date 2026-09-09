import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { ProfileService } from '../../../core/services/profile.service';
import {
  ALL_AI_MODELS,
  AiModelDefinition,
  buildSpecialtyRestrictionMessage,
  filterModelsBySpecialty,
  normalizeSpecialty
} from '../../../core/config/specialty-organ.config';

interface AIModel extends AiModelDefinition {
  status: 'active' | 'inactive';
  reference: {
    title: string;
    journal: string;
    year: number;
    doi: string;
  };
  accuracy?: number;
}

@Component({
  selector: 'app-ai-models-selection',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ai-models-selection.component.html',
  styleUrl: './ai-models-selection.component.scss'
})
export class AiModelsSelectionComponent implements OnInit {
  patientId = '';
  selectedModel: AIModel | null = null;
  selectedImage: string | null = null;
  selectedFileName = '';
  detectedOrgan = '';
  compatibilityWarning = false;
  forceContinue = false;

  doctorSpecialty = '';
  specialtyLocked = false;
  specialtyRestrictionMessage = '';
  uploadBlockedBySpecialty = false;

  aiModels: AIModel[] = [];

  organKeywords: Record<string, string[]> = {
    cerveau: ['brain', 'cerveau', 'crane', 'skull', 'tete', 'head', 'mri_brain', 'irm_cerveau'],
    sein: ['breast', 'sein', 'mammographie', 'mammo', 'mammography', 'thorax_femme'],
    peau: ['skin', 'peau', 'derma', 'cutanee', 'lesion', 'melanome'],
    oeil: ['eye', 'oeil', 'retina', 'retine', 'fundus', 'fond_oeil', 'ophtalmo'],
    poumon: ['lung', 'poumon', 'thorax', 'chest', 'pulmonary', 'pneumo', 'ct_chest'],
    foie: ['liver', 'foie', 'hepatic', 'abdomen', 'ct_abdomen', 'tumeur_foie'],
    coeur: ['heart', 'coeur', 'cardiac', 'cardiaque', 'ecg', 'echocardiogram']
  };

  private readonly modelMeta: Record<string, Omit<AIModel, keyof AiModelDefinition>> = {
    'brain-tumor-3d': {
      status: 'active',
      reference: {
        title: '3D U-Net for Brain Tumor Segmentation',
        journal: 'BrainLesion Workshop (MICCAI 2019)',
        year: 2019,
        doi: '10.1007/978-3-030-46640-4_12'
      },
      accuracy: 94.2
    },
    'breast-cancer-mammo': {
      status: 'active',
      reference: {
        title: 'Deep Learning for Breast Cancer Detection on Mammography',
        journal: 'Radiology: Artificial Intelligence',
        year: 2021,
        doi: '10.1148/ryai.2020190227'
      },
      accuracy: 91.8
    },
    'skin-lesion': {
      status: 'active',
      reference: {
        title: 'Dermatologist-level classification of skin cancer with deep neural networks',
        journal: 'Nature',
        year: 2017,
        doi: '10.1038/nature21056'
      },
      accuracy: 96.3
    },
    'diabetic-retinopathy': {
      status: 'active',
      reference: {
        title: 'Development and Validation of a Deep Learning Algorithm for Detection of Diabetic Retinopathy',
        journal: 'JAMA',
        year: 2016,
        doi: '10.1001/jama.2016.17216'
      },
      accuracy: 90.3
    },
    'alzheimer-mri': {
      status: 'active',
      reference: {
        title: 'Deep Learning for Alzheimer\'s Disease Diagnosis',
        journal: 'NeuroImage',
        year: 2020,
        doi: '10.1016/j.neuroimage.2019.02.048'
      },
      accuracy: 88.7
    },
    'pulmonary-nodule': {
      status: 'active',
      reference: {
        title: 'Pulmonary Nodule Detection in CT Scans using a 3D Convolutional Neural Network',
        journal: 'Medical Physics',
        year: 2020,
        doi: '10.1002/mp.13045'
      },
      accuracy: 92.5
    },
    'liver-segmentation': {
      status: 'inactive',
      reference: {
        title: 'Automatic Liver and Lesion Segmentation in CT using Cascaded Fully Convolutional Networks',
        journal: 'MICCAI 2017',
        year: 2017,
        doi: '10.1007/978-3-319-66182-7_4'
      },
      accuracy: 89.1
    },
    'cardiac-mri': {
      status: 'inactive',
      reference: {
        title: 'Automatic Cardiac MR Segmentation with Dilated Convolutional Networks',
        journal: 'Medical Image Analysis',
        year: 2018,
        doi: '10.1016/j.media.2018.05.005'
      },
      accuracy: 93.4
    }
  };

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private profileService: ProfileService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.patientId = params['patientId'] || '';
    });

    this.selectedImage = localStorage.getItem('selectedIRMImage');
    this.selectedFileName = localStorage.getItem('selectedIRMFileName') || 'image.jpg';
    this.detectedOrgan = this.detectOrganFromFilename(this.selectedFileName);

    this.loadSpecialtyFilteredModels();
  }

  private buildCatalogModels(): AIModel[] {
    return ALL_AI_MODELS.map((model) => ({
      ...model,
      ...(this.modelMeta[model.id] || {
        status: 'active' as const,
        reference: { title: model.name, journal: 'N/A', year: 2024, doi: '' }
      })
    }));
  }

  private loadSpecialtyFilteredModels(): void {
    const catalog = this.buildCatalogModels();
    this.profileService.getMyProfile().subscribe({
      next: (profile) => {
        const specialty = normalizeSpecialty(profile.specialty, profile.specialtyCode);
        if (!specialty) {
          this.aiModels = catalog;
          return;
        }
        this.applySpecialtyFilter(specialty, catalog);
      },
      error: () => {
        this.aiModels = catalog;
      }
    });
  }

  private applySpecialtyFilter(specialty: string, catalog: AIModel[]): void {
    const result = filterModelsBySpecialty(specialty, catalog);
    this.doctorSpecialty = result.specialty;
    this.aiModels = result.models as AIModel[];
    this.specialtyLocked = result.locked;
    this.uploadBlockedBySpecialty = result.locked && result.allowedOrgans.length === 0;
    this.specialtyRestrictionMessage = buildSpecialtyRestrictionMessage(
      result.specialty,
      result.allowedOrgans
    );
  }

  detectOrganFromFilename(filename: string): string {
    if (!filename) return '';

    const lowerFilename = filename.toLowerCase();
    for (const [organ, keywords] of Object.entries(this.organKeywords)) {
      for (const keyword of keywords) {
        if (lowerFilename.includes(keyword.toLowerCase())) {
          return organ;
        }
      }
    }
    return 'unknown';
  }

  checkCompatibility(model: AIModel): boolean {
    if (!this.detectedOrgan || this.detectedOrgan === 'unknown') {
      return true;
    }
    return model.organ === this.detectedOrgan;
  }

  getOrganName(organ: string): string {
    const organNames: Record<string, string> = {
      cerveau: 'Cerveau',
      sein: 'Sein',
      peau: 'Peau',
      oeil: 'Œil',
      poumon: 'Poumon',
      foie: 'Foie',
      coeur: 'Cœur'
    };
    return organNames[organ] || organ;
  }

  selectModel(model: AIModel): void {
    if (this.uploadBlockedBySpecialty) {
      alert(this.specialtyRestrictionMessage);
      return;
    }
    if (!this.aiModels.some((m) => m.id === model.id)) {
      alert(this.specialtyRestrictionMessage || 'Ce modèle n\'est pas autorisé pour votre spécialité.');
      return;
    }
    if (model.status === 'inactive') {
      alert('Ce modèle est actuellement en maintenance.');
      return;
    }

    const isCompatible = this.checkCompatibility(model);
    this.compatibilityWarning = !isCompatible;
    this.forceContinue = false;
    this.selectedModel = model;
  }

  forceSelectModel(): void {
    if (this.specialtyLocked) {
      return;
    }
    this.forceContinue = true;
    this.compatibilityWarning = false;
    this.confirmSelection();
  }

  cancelSelection(): void {
    this.selectedModel = null;
    this.compatibilityWarning = false;
    this.forceContinue = false;
  }

  confirmSelection(): void {
    if (this.selectedModel) {
      this.router.navigate(['/doctor/analyze'], {
        queryParams: {
          patientId: this.patientId,
          modelId: this.selectedModel.id,
          modelName: this.selectedModel.name
        }
      });
    }
  }

  goBack(): void {
    if (this.patientId) {
      this.router.navigate(['/doctor/medical-record', this.patientId]);
    } else {
      this.router.navigate(['/doctor']);
    }
  }

  openDoiLink(doi: string): void {
    window.open(`https://doi.org/${doi}`, '_blank');
  }

  changeImage(): void {
    if (this.patientId) {
      this.router.navigate(['/doctor/medical-record', this.patientId]);
    } else {
      this.router.navigate(['/doctor/medical-record']);
    }
  }
}
