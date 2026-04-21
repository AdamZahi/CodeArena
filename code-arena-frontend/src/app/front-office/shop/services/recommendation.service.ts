import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Product } from '../models/product.model';

export interface Recommendation {
  id: string;
  score: number;
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class RecommendationService {

  private readonly AI_URL = 'http://localhost:5000';

  constructor(private http: HttpClient) {}

  getRecommendations(
    participantId: string,
    userOrders: { productId: string; productName: string; category: string; quantity: number }[],
    allProducts: Product[],
    limit = 4
  ): Observable<Recommendation[]> {

    const body = {
      participantId,
      userOrders,
      allProducts: allProducts.map(p => ({
        id: p.id,
        name: p.name,
        category: p.category,
        price: p.price,
        stock: p.stock
      })),
      limit
    };

    return this.http.post<any>(`${this.AI_URL}/api/recommend`, body).pipe(
      map(res => res.recommendations || []),
      catchError(() => of([]))
    );
  }
}