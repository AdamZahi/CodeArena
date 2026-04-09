import { Injectable } from '@angular/core';
import { GeminiService } from './gemini.service';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface EcoScore {
  score: number;    // 1-10
  label: string;    // Poor / Average / Good / Excellent
  reason: string;   // one sentence explanation
  color: string;    // CSS color based on score
  emoji: string;    // visual indicator
}

@Injectable({ providedIn: 'root' })
export class EcoScoreService {

  // ── CACHE ──────────────────────────────────────
  // Store generated scores so we don't call Gemini repeatedly
  // Key: productId, Value: EcoScore
  private cache = new Map<string, EcoScore>();

  constructor(private geminiService: GeminiService) {}

// ── GET ECO SCORE ──────────────────────────────
// Returns cached score if available, otherwise generates new one
// Uses delay to avoid hitting Gemini rate limits (15 req/min free tier)
getScore(productId: string, productName: string, category: string, delayMs: number = 0): Observable<EcoScore> {

  // Return cached score if exists — no API call needed
  if (this.cache.has(productId)) {
    return of(this.cache.get(productId)!);
  }

  // Add delay to spread requests and avoid rate limiting
  return new Observable(observer => {
    setTimeout(() => {
      this.geminiService.generateEcoScore(productName, category).pipe(
        map(response => {
          const clean = response.replace(/```json|```/g, '').trim();
          const data = JSON.parse(clean);
          const ecoScore: EcoScore = {
            score: data.score,
            label: data.label,
            reason: data.reason,
            color: this.getColor(data.score),
            emoji: this.getEmoji(data.score)
          };
          this.cache.set(productId, ecoScore);
          return ecoScore;
        }),
        catchError(() => {
          const defaultScore: EcoScore = {
            score: 5, label: 'Average',
            reason: 'Standard environmental impact for this product type.',
            color: '#f59e0b', emoji: '🌿'
          };
          this.cache.set(productId, defaultScore);
          return of(defaultScore);
        })
      ).subscribe(score => {
        observer.next(score);
        observer.complete();
      });
    }, delayMs);
  });
}

  // ── COLOR HELPER ───────────────────────────────
  private getColor(score: number): string {
    if (score >= 9) return '#10b981'; // emerald — excellent
    if (score >= 7) return '#22c55e'; // green — good
    if (score >= 4) return '#f59e0b'; // amber — average
    return '#ef4444';                 // red — poor
  }

  // ── EMOJI HELPER ───────────────────────────────
  private getEmoji(score: number): string {
    if (score >= 9) return '🌱';
    if (score >= 7) return '🌿';
    if (score >= 4) return '♻️';
    return '⚠️';
  }
}