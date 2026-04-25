import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface EcoScore {
  score: number;
  label: string;
  reason: string;
  color: string;
  emoji: string;
}

@Injectable({ providedIn: 'root' })
export class EcoScoreService {

  private readonly BACKEND_URL = 'http://localhost:8080/api/shop';
  private cache = new Map<string, EcoScore>();

  constructor(private http: HttpClient) {}

  // ── BATCH: score all products at once via BACKEND (backend calls Flask)
  loadAllScores(products: { id: string; name: string; category: string }[]): Observable<{ [id: string]: EcoScore }> {
    const uncached = products.filter(p => !this.cache.has(p.id));

    if (uncached.length === 0) {
      const result: { [id: string]: EcoScore } = {};
      products.forEach(p => { result[p.id] = this.cache.get(p.id)!; });
      return of(result);
    }

    // Call backend for each uncached product
    // Backend calls Flask internally — Angular never touches Flask
    const result: { [id: string]: EcoScore } = {};

    // Add already cached
    products.forEach(p => {
      if (this.cache.has(p.id)) result[p.id] = this.cache.get(p.id)!;
    });

    // For uncached: call backend /analyze which calls Flask internally
    uncached.forEach(p => {
      this.http.post<any>(`${this.BACKEND_URL}/products/${p.id}/analyze`, {}).pipe(
        catchError(() => of(null))
      ).subscribe(res => {
        if (res?.data?.ecoScore != null) {
          const score: EcoScore = this.buildEcoScore(res.data.ecoScore);
          this.cache.set(p.id, score);
          result[p.id] = score;
        } else {
          // fallback if Flask is down
          const fallback = this.getFallback();
          this.cache.set(p.id, fallback);
          result[p.id] = fallback;
        }
      });
    });

    return of(result);
  }

  // ── SINGLE score via backend
  getScore(productId: string): Observable<EcoScore> {
    if (this.cache.has(productId)) return of(this.cache.get(productId)!);

    return this.http.post<any>(`${this.BACKEND_URL}/products/${productId}/analyze`, {}).pipe(
      map(res => {
        const score = this.buildEcoScore(res.data.ecoScore);
        this.cache.set(productId, score);
        return score;
      }),
      catchError(() => of(this.getFallback()))
    );
  }

  // ── Build EcoScore object from numeric score
  private buildEcoScore(score: number): EcoScore {
    if (score >= 8) return { score, label: 'Excellent', reason: 'Very low environmental impact.', color: '#10b981', emoji: '🌱' };
    if (score >= 5) return { score, label: 'Good', reason: 'Moderate environmental impact.', color: '#f59e0b', emoji: '♻️' };
    return { score, label: 'Poor', reason: 'High environmental impact.', color: '#ef4444', emoji: '⚠️' };
  }

  // ── Fallback when backend/Flask unavailable
  private getFallback(): EcoScore {
    return { score: 5, label: 'Average', reason: 'Standard environmental impact.', color: '#f59e0b', emoji: '♻️' };
  }
}