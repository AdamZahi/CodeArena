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

private readonly BACKEND_URL = 'http://localhost:8080/api/shop';

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

   return this.http.post<any>(`${this.BACKEND_URL}/recommendations`, body).pipe(
      map(res => res.data?.recommendations || res.recommendations || []),
      catchError(() => of([]))
    );
  }
}