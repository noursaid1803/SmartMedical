export interface AiModelDefinition {
  id: string;
  name: string;
  organ: string;
  icon: string;
  color: string;
  description: string;
  specialties: string[];
}

export const ALL_AI_MODELS: AiModelDefinition[] = [
  { id: 'brain-tumor-3d', name: 'Cerveau (2D/3D Tumeurs)', organ: 'cerveau', icon: '🧠', color: '#e91e63', description: 'Segmentation volumétrique de tumeurs cérébrales', specialties: ['Neurologie', 'Radiologie'] },
  { id: 'breast-cancer-mammo', name: 'Sein / Mammographie', organ: 'sein', icon: '🎀', color: '#c2185b', description: 'Classification de lésions mammaires BI-RADS', specialties: ['Gynécologie', 'Radiologie'] },
  { id: 'skin-lesion', name: 'Lésions cutanées', organ: 'peau', icon: '🔬', color: '#ff9800', description: 'Classification de lésions pigmentées', specialties: ['Dermatologie', 'Radiologie'] },
  { id: 'diabetic-retinopathy', name: 'Rétinopathie diabétique', organ: 'oeil', icon: '👁️', color: '#2196f3', description: 'Détection de rétinopathie sur fond d\'œil', specialties: ['Ophtalmologie', 'Radiologie'] },
  { id: 'alzheimer-mri', name: 'Alzheimer (IRM)', organ: 'cerveau', icon: '🧠', color: '#9c27b0', description: 'Prédiction de la maladie d\'Alzheimer', specialties: ['Neurologie', 'Radiologie'] },
  { id: 'pulmonary-nodule', name: 'Poumon / Nodules', organ: 'poumon', icon: '🫁', color: '#00bcd4', description: 'Segmentation de nodules pulmonaires', specialties: ['Pneumologie', 'Oncologie', 'Radiologie'] },
  { id: 'liver-segmentation', name: 'Foie / Segmentation', organ: 'foie', icon: '🫘', color: '#795548', description: 'Segmentation du foie et lésions focales', specialties: ['Gastroentérologie', 'Radiologie'] },
  { id: 'cardiac-mri', name: 'Cardiaque (IRM)', organ: 'coeur', icon: '❤️', color: '#f44336', description: 'Segmentation des cavités cardiaques', specialties: ['Cardiologie', 'Radiologie'] }
];

/** Organe autorisé par spécialité médicale (clé = libellé normalisé). */
export const SPECIALTY_ORGAN_MAP: Record<string, string[]> = {
  'Pneumologie': ['poumon'],
  'Oncologie': ['poumon'],
  'Neurologie': ['cerveau'],
  'Cardiologie': ['coeur'],
  'Dermatologie': ['peau'],
  'Ophtalmologie': ['oeil'],
  'Gynécologie': ['sein'],
  'Gastroentérologie': ['foie'],
  'Radiologie': ['cerveau', 'sein', 'peau', 'oeil', 'poumon', 'foie', 'coeur'],
  'Médecine générale': []
};

const ORGAN_DISPLAY_NAMES: Record<string, string> = {
  cerveau: 'Cerveau',
  sein: 'Sein',
  peau: 'Peau',
  oeil: 'Œil',
  poumon: 'Poumon',
  foie: 'Foie',
  coeur: 'Cœur'
};

/** Anciens libellés (backoffice / specialtyCode) → spécialité normalisée. */
const SPECIALTY_ALIASES: Record<string, string> = {
  'POUMON': 'Pneumologie',
  'PNEUMOLOGIE': 'Pneumologie',
  'ONCOLOGIE': 'Oncologie',
  'CERVEAU': 'Neurologie',
  'ALZHEIMER': 'Neurologie',
  'NEUROLOGIE': 'Neurologie',
  'SEIN': 'Gynécologie',
  'GYNECOLOGIE': 'Gynécologie',
  'GYNÉCOLOGIE': 'Gynécologie',
  'PEAU': 'Dermatologie',
  'DERMATOLOGIE': 'Dermatologie',
  'OEIL': 'Ophtalmologie',
  'OPHTALMOLOGIE': 'Ophtalmologie',
  'COEUR': 'Cardiologie',
  'CARDIOLOGIE': 'Cardiologie',
  'FOIE': 'Gastroentérologie',
  'GASTROENTEROLOGIE': 'Gastroentérologie',
  'GASTROENTÉROLOGIE': 'Gastroentérologie',
  'RADIOLOGIE': 'Radiologie',
  'MEDECINE_GENERALE': 'Médecine générale',
  'MÉDECINE GÉNÉRALE': 'Médecine générale',
  'Poumon / Nodules': 'Pneumologie',
  'Cerveau (2D/3D Tumeurs)': 'Neurologie',
  'Alzheimer (IRM)': 'Neurologie',
  'Sein / Mammographie': 'Gynécologie',
  'Lésions cutanées': 'Dermatologie',
  'Rétinopathie diabétique': 'Ophtalmologie',
  'Cardiaque (IRM)': 'Cardiologie',
  'Foie / Segmentation': 'Gastroentérologie'
};

export function normalizeSpecialty(raw?: string | null, specialtyCode?: string | null): string {
  const code = (specialtyCode || '').trim();
  if (code && SPECIALTY_ALIASES[code]) {
    return SPECIALTY_ALIASES[code];
  }
  const value = (raw || '').trim();
  if (!value) {
    return '';
  }
  if (SPECIALTY_ALIASES[value]) {
    return SPECIALTY_ALIASES[value];
  }
  const direct = Object.keys(SPECIALTY_ORGAN_MAP).find(
    (key) => key.toLowerCase() === value.toLowerCase()
  );
  return direct || value;
}

export function getAllowedOrgans(specialty: string): string[] | null {
  const normalized = normalizeSpecialty(specialty);
  if (!normalized) {
    return null;
  }
  if (normalized in SPECIALTY_ORGAN_MAP) {
    return SPECIALTY_ORGAN_MAP[normalized];
  }
  return [];
}

export function filterModelsBySpecialty(
  specialty: string,
  models: AiModelDefinition[] = ALL_AI_MODELS
): { models: AiModelDefinition[]; locked: boolean; specialty: string; allowedOrgans: string[] } {
  const normalized = normalizeSpecialty(specialty);
  const allowedOrgans = getAllowedOrgans(normalized);

  if (allowedOrgans === null) {
    return { models: [...models], locked: false, specialty: normalized, allowedOrgans: [] };
  }

  if (allowedOrgans.length === 0) {
    return { models: [], locked: true, specialty: normalized, allowedOrgans: [] };
  }

  const filtered = models.filter((m) => allowedOrgans.includes(m.organ));
  return { models: filtered, locked: true, specialty: normalized, allowedOrgans };
}

export function isOrganAllowedForSpecialty(specialty: string, organ: string): boolean {
  const allowed = getAllowedOrgans(normalizeSpecialty(specialty));
  if (allowed === null) {
    return true;
  }
  return allowed.includes(organ);
}

export function getOrganDisplayName(organ: string): string {
  return ORGAN_DISPLAY_NAMES[organ] || organ;
}

export function buildSpecialtyRestrictionMessage(specialty: string, allowedOrgans: string[]): string {
  const normalized = normalizeSpecialty(specialty) || specialty;
  if (allowedOrgans.length === 0) {
    return `Votre spécialité (${normalized}) ne permet pas l'import d'images scanner/IRM. Veuillez orienter le patient vers un spécialiste.`;
  }
  const organLabels = allowedOrgans.map(getOrganDisplayName).join(', ');
  return `Votre spécialité (${normalized}) permet uniquement l'analyse de : ${organLabels}.`;
}
