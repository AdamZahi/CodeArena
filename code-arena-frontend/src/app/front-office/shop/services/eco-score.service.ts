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

  private readonly AI_URL = 'http://localhost:5000';
  private cache = new Map<string, EcoScore>();

  constructor(private http: HttpClient) {}

  // ── BATCH: score all products at once, instant, no delays needed
  loadAllScores(products: { id: string; name: string; category: string }[]): Observable<{ [id: string]: EcoScore }> {
    // Filter out already cached
    const uncached = products.filter(p => !this.cache.has(p.id));

    if (uncached.length === 0) {
      const result: { [id: string]: EcoScore } = {};
      products.forEach(p => { result[p.id] = this.cache.get(p.id)!; });
      return of(result);
    }

    return this.http.post<any>(`${this.AI_URL}/api/eco-score/batch`, { products: uncached }).pipe(
      map(res => {
        const result: { [id: string]: EcoScore } = {};
        // Add newly fetched to cache
Object.entries(res.scores || {}).forEach(([id, score]: [string, any]) => {
  this.cache.set(id, score);
  result[id] = score;
  // ── SAVE ECO SCORE TO DB ──────────────────
  // Persists AI score so backend can use it for loyalty eco bonus
  // Fire-and-forget — doesn't affect UI if it fails
  this.http.post(`http://localhost:8080/api/shop/products/${id}/eco-score`,
    { score: score.score }).subscribe();
});
        // Add already cached ones
        products.forEach(p => {
          if (this.cache.has(p.id)) result[p.id] = this.cache.get(p.id)!;
        });
        return result;
      }),
      catchError(() => {
        // Fallback if Flask is down
        const result: { [id: string]: EcoScore } = {};
        products.forEach(p => {
          result[p.id] = { score: 5, label: 'Average', reason: 'Standard environmental impact.', color: '#f59e0b', emoji: '♻️' };
        });
        return of(result);
      })
    );
  }

  // ── SINGLE score (kept for compatibility)
  getScore(productId: string, productName: string, category: string): Observable<EcoScore> {
    if (this.cache.has(productId)) return of(this.cache.get(productId)!);

    return this.http.post<any>(`${this.AI_URL}/api/eco-score`, { productId, productName, category }).pipe(
      map(res => {
        const score: EcoScore = { score: res.score, label: res.label, reason: res.reason, color: res.color, emoji: res.emoji };
        this.cache.set(productId, score);
        return score;
      }),
      catchError(() => of({ score: 5, label: 'Average', reason: 'Standard environmental impact.', color: '#f59e0b', emoji: '♻️' }))
    );
  }

  
}