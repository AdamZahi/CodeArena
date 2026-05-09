import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = `${environment.apiBaseUrl}/api/arenatalk/payments`;

  constructor(private http: HttpClient) {}

  createCheckout(coins: number, userId: string, userName: string) {
    return this.http.post<any>(`${this.apiUrl}/create-checkout-session`, {
      coins,
      userId,
      userName
    });
  }
}