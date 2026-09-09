import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

export interface OrganDetectionResult {
  organ: string;
  confidence: number;
  detected: boolean;
  alternativeOrgans?: { organ: string; confidence: number }[];
}

@Injectable({
  providedIn: 'root'
})
export class ImageAnalysisService {

  // Simuler l'analyse d'image pour détecter l'organe
  // Dans une version réelle, cela utiliserait TensorFlow.js ou un backend Python avec OpenCV
  analyzeImage(imageBase64: string, expectedOrgan: string): Observable<OrganDetectionResult> {
    // Simulation basée sur l'analyse des pixels (dans la vraie vie: CNN/Deep Learning)
    return this.simulateOrganDetection(imageBase64, expectedOrgan);
  }

  private simulateOrganDetection(imageBase64: string, expectedOrgan: string): Observable<OrganDetectionResult> {
    // Analyser les caractéristiques basiques de l'image pour simulation
    const imageFeatures = this.extractBasicFeatures(imageBase64);
    
    // Détecter l'organe basé sur les caractéristiques
    const detection = this.detectOrganByFeatures(imageFeatures, expectedOrgan);
    
    // Simuler un délai d'analyse (1-2 secondes)
    return of(detection).pipe(delay(1000 + Math.random() * 1000));
  }

  private extractBasicFeatures(imageBase64: string): any {
    // Extraction basique pour simulation
    // Dans la vraie vie: extraction de caractéristiques avec OpenCV (formes, textures, contours)
    const imageSize = imageBase64.length;
    const hasHighContrast = imageBase64.includes('data:image');
    
    return {
      size: imageSize,
      hasMetadata: hasHighContrast,
      // Simuler des ratios d'aspect typiques par organe
      aspectRatio: this.estimateAspectRatio(imageBase64)
    };
  }

  private estimateAspectRatio(imageBase64: string): number {
    // Estimation basée sur la taille de l'image
    // Les images médicales ont des ratios caractéristiques
    const size = imageBase64.length;
    
    if (size < 50000) return 0.8;  // Petites images (peau, œil)
    if (size < 150000) return 1.0; // Images moyennes (cerveau)
    if (size < 300000) return 1.2; // Images larges (thorax)
    return 1.0;
  }

  private detectOrganByFeatures(features: any, expectedOrgan: string): OrganDetectionResult {
    const organProfiles: { [key: string]: { aspectRange: [number, number], sizeRange: [number, number] } } = {
      'cerveau': { aspectRange: [0.9, 1.1], sizeRange: [50000, 300000] },
      'sein': { aspectRange: [0.7, 1.0], sizeRange: [40000, 200000] },
      'peau': { aspectRange: [0.8, 1.2], sizeRange: [20000, 100000] },
      'oeil': { aspectRange: [0.9, 1.1], sizeRange: [15000, 80000] },
      'poumon': { aspectRange: [1.0, 1.4], sizeRange: [100000, 500000] },
      'foie': { aspectRange: [0.9, 1.3], sizeRange: [80000, 400000] },
      'coeur': { aspectRange: [0.8, 1.2], sizeRange: [60000, 250000] }
    };

    // Calculer le score pour chaque organe
    const scores: { [key: string]: number } = {};
    
    for (const [organ, profile] of Object.entries(organProfiles)) {
      const aspectScore = this.calculateRangeScore(features.aspectRatio, profile.aspectRange);
      const sizeScore = this.calculateRangeScore(features.size, profile.sizeRange);
      scores[organ] = (aspectScore + sizeScore) / 2;
    }

    // Trouver le meilleur match
    let bestOrgan = '';
    let bestScore = 0;
    const alternatives: { organ: string; confidence: number }[] = [];

    for (const [organ, score] of Object.entries(scores)) {
      if (score > bestScore) {
        if (bestOrgan) {
          alternatives.push({ organ: bestOrgan, confidence: Math.round(bestScore * 100) });
        }
        bestOrgan = organ;
        bestScore = score;
      } else if (score > 0.3) {
        alternatives.push({ organ, confidence: Math.round(score * 100) });
      }
    }

    // Ajouter un peu de randomness pour rendre réaliste
    const randomFactor = 0.9 + Math.random() * 0.2; // 0.9 - 1.1
    const finalConfidence = Math.min(Math.round(bestScore * 100 * randomFactor), 98);

    // Vérifier si l'organe attendu correspond
    const isMatch = bestOrgan === expectedOrgan;
    
    // Si ce n'est pas un match mais que le score est élevé pour l'organe attendu
    const expectedScore = scores[expectedOrgan] || 0;
    const couldBeExpected = expectedScore > 0.6;

    return {
      organ: bestOrgan,
      confidence: finalConfidence,
      detected: isMatch || couldBeExpected,
      alternativeOrgans: alternatives.slice(0, 2)
    };
  }

  private calculateRangeScore(value: number, range: [number, number]): number {
    const [min, max] = range;
    if (value >= min && value <= max) {
      return 1.0;
    }
    const center = (min + max) / 2;
    const deviation = Math.abs(value - center) / center;
    return Math.max(0, 1 - deviation);
  }

  // Méthode pour obtenir le nom de l'organe en français
  getOrganName(organ: string): string {
    const organNames: { [key: string]: string } = {
      'cerveau': 'Cerveau',
      'sein': 'Sein',
      'peau': 'Peau',
      'oeil': 'Œil',
      'poumon': 'Poumon',
      'foie': 'Foie',
      'coeur': 'Cœur'
    };
    return organNames[organ] || organ;
  }

  // Méthode pour obtenir l'icône de l'organe
  getOrganIcon(organ: string): string {
    const organIcons: { [key: string]: string } = {
      'cerveau': '🧠',
      'sein': '🎀',
      'peau': '🔬',
      'oeil': '👁️',
      'poumon': '🫁',
      'foie': '🫘',
      'coeur': '❤️'
    };
    return organIcons[organ] || '🔍';
  }
}
