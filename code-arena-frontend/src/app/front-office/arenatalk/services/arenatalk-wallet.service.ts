import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface ArenaTalkWallet {
  id: number;
  userId: string;
  userName: string;
  balance: number;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ArenaTalkWalletService {
  private apiUrl = `${environment.apiBaseUrl}/api/arenatalk/wallet`;

  constructor(private http: HttpClient) {}

  getWallet(userId: string, userName: string): Observable<ArenaTalkWallet> {
    const params = new HttpParams().set('userName', userName || 'Unknown');
    return this.http.get<ArenaTalkWallet>(`${this.apiUrl}/${encodeURIComponent(userId)}`, { params });
  }
}