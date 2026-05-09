import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type GiftType = 'COINS' | 'FIRE' | 'CROWN' | 'ROCKET';

export interface SendGiftRequest {
  fromUserId: string;
  fromUserName: string;
  toUserId: string;
  toUserName: string;
  hubId: number;
  voiceChannelId: number;
  giftType: GiftType;
  coins: number;
  amountMoney: number;
  currency: string;
}

export interface GiftTransaction {
  id: number;
  fromUserName: string;
  toUserName: string;
  giftType: GiftType;
  coins: number;
  amountMoney: number;
  currency: string;
}

@Injectable({
  providedIn: 'root'
})
export class GiftService {
  private apiUrl = `${environment.apiBaseUrl}/api/arenatalk/gifts`;

  constructor(private http: HttpClient) {}

  sendGift(request: SendGiftRequest): Observable<GiftTransaction> {
    return this.http.post<GiftTransaction>(`${this.apiUrl}/send`, request);
  }
}