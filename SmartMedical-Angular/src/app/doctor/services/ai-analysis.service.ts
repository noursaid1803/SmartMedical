import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface OrganValidationResponse {
  success: boolean;
  valid: boolean;
  organ: string;
  organ_name: string;
  confidence: number;
  expected_organ?: string;
  expected_organ_name?: string;
  reason?: string;
  alternative_organs?: Array<{organ: string, confidence: number}>;
  all_scores?: {[key: string]: number};
  method?: string;
  error?: string;
  score?: number;
  total_checks?: number;
  criteria_tags?: string[];
  message?: string;
  rejection_reason?: string;
}

export interface AlzheimerAnalysisResponse {
  success: boolean;
  stage: number;
  stage_label: string;
  stage_name: string;
  confidence: number;
  precision: number;
  diagnosis: string;
  recommendations: string;
  gradcam_image_base64?: string;
  probabilities?: Record<string, number>;
  affected_zones?: Array<{
    region?: string;
    zone?: string;
    label?: string;
    activation_pct?: number;
    confidence_pct?: number;
    intensity_pct?: number;
    confidence?: number;
    intensity?: string;
    severity?: string;
  }>;
  validation?: OrganValidationResponse;
  model?: Record<string, unknown>;
  alert?: boolean;
}

export interface OrganPredictionResponse {
  success: boolean;
  organ: string;
  organ_name: string;
  confidence: number;
  alternative_organs?: Array<{organ: string, confidence: number}>;
  all_scores?: {[key: string]: number};
  error?: string;
}

export interface SupportedOrgan {
  id: string;
  name: string;
}

export interface ModelInfo {
  model_name: string;
  architecture: string;
  num_classes: number;
  classes: Array<{id: string, name: string}>;
}

export interface LungCancerDetection {
  type: string;
  location: string;
  confidence: number;
  severity: 'low' | 'medium' | 'high';
}

export interface LungCancerAnalysisResponse {
  success: boolean;
  stage: number;
  stage_label: string;
  stage_name: string;
  confidence: number;
  isNormal: boolean;
  diagnosis: string;
  detections: LungCancerDetection[];
  recommendations: string;
  gradcam_image_base64?: string;
  probabilities?: Record<string, number>;
  architecture?: string;
  model_loaded?: boolean;
  alert?: boolean;
}

interface AuthFallbackValidationResponse {
  success: boolean;
  valid: boolean;
  organ: string;
  organName: string;
  confidence: number;
  expectedOrgan?: string;
  expectedOrganName?: string;
  reason?: string;
  allScores?: {[key: string]: number};
  method?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiAnalysisService {
  private primaryApiUrl = `${environment.gatewayUrl || ''}/ai`;
  private alzheimerApiUrl = `${environment.gatewayUrl || ''}/ai/alzheimer`;
  private fallbackApiUrl = `${environment.gatewayUrl || ''}/auth/organ-validation`;

  constructor(private http: HttpClient) {}

  validateImage(imageFile: File, expectedOrgan: string, doctorSpecialty?: string): Observable<OrganValidationResponse> {
    const formData = new FormData();
    formData.append('file', imageFile);
    formData.append('expected_organ', expectedOrgan);
    if (doctorSpecialty) {
      formData.append('doctor_specialty', doctorSpecialty);
    }

    return this.http.post<OrganValidationResponse>(`${this.primaryApiUrl}/validate`, formData).pipe(
      catchError(() => this.validateImageFallback(formData))
    );
  }

  private validateImageFallback(formData: FormData): Observable<OrganValidationResponse> {
    return this.http.post<AuthFallbackValidationResponse>(`${this.fallbackApiUrl}/validate`, formData).pipe(
      map((response) => ({
        success: response.success,
        valid: response.valid,
        organ: response.organ,
        organ_name: response.organName,
        confidence: response.confidence,
        expected_organ: response.expectedOrgan,
        expected_organ_name: response.expectedOrganName,
        reason: response.reason,
        all_scores: response.allScores,
        method: response.method || 'auth-fallback'
      })),
      catchError((err) => of({
        success: false,
        valid: false,
        organ: 'unknown',
        organ_name: 'Inconnu',
        confidence: 0,
        reason: 'Service de validation indisponible. Réessayez plus tard.',
        error: err?.message || 'Validation unavailable'
      }))
    );
  }

  predictOrgan(imageFile: File): Observable<OrganPredictionResponse> {
    const formData = new FormData();
    formData.append('file', imageFile);

    return this.http.post<OrganPredictionResponse>(`${this.primaryApiUrl}/predict`, formData);
  }

  analyzeImage(imageFile: File, modelType?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', imageFile);
    if (modelType) {
      formData.append('model_type', modelType);
    }

    return this.http.post(`${this.primaryApiUrl}/analyze`, formData);
  }

  validateBrainMri(imageFile: File): Observable<OrganValidationResponse> {
    const formData = new FormData();
    formData.append('file', imageFile);
    return this.http.post<OrganValidationResponse>(`${this.alzheimerApiUrl}/validate`, formData);
  }

  analyzeAlzheimer(imageFile: File): Observable<AlzheimerAnalysisResponse> {
    const formData = new FormData();
    formData.append('file', imageFile);
    return this.http.post<AlzheimerAnalysisResponse>(`${this.alzheimerApiUrl}/analyze`, formData);
  }

  analyzeLungCancer(imageFile: File, doctorSpecialty?: string): Observable<LungCancerAnalysisResponse> {
    const formData = new FormData();
    formData.append('file', imageFile);
    if (doctorSpecialty) {
      formData.append('doctor_specialty', doctorSpecialty);
    }
    return this.http.post<LungCancerAnalysisResponse>(
      `${this.primaryApiUrl}/analyze/lung-cancer`,
      formData
    );
  }

  getLungCancerModelStatus(): Observable<any> {
    return this.http.get(`${this.primaryApiUrl}/analyze/lung-cancer/status`);
  }

  getSupportedOrgans(): Observable<SupportedOrgan[]> {
    return this.http.get<{organs: SupportedOrgan[]}>(`${this.primaryApiUrl}/organs`)
      .pipe(map(response => response.organs));
  }

  getModelInfo(): Observable<ModelInfo> {
    return this.http.get<ModelInfo>(`${this.primaryApiUrl}/model/info`);
  }

  healthCheck(): Observable<any> {
    return this.http.get(`${this.primaryApiUrl}/`);
  }

  base64ToFile(base64String: string, filename: string = 'image.jpg'): File {
    const arr = base64String.split(',');
    const mime = arr[0].match(/:(.*?);/)?.[1] || 'image/jpeg';
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);

    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }

    return new File([u8arr], filename, { type: mime });
  }
}
