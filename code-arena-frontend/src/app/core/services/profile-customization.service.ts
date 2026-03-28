import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CustomizationItem, UserUnlock, EquipItemRequest } from '../models/customization.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProfileCustomizationService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = `${environment.apiBaseUrl}/api/profile/customization`;
  private readonly ADMIN_API = `${environment.apiBaseUrl}/api/admin/customization`;

  // --- User Endpoints ---

  getMyUnlocks(): Observable<UserUnlock[]> {
    return this.http.get<UserUnlock[]>(`${this.API_URL}/unlocks`);
  }

  getMyUnlocksByType(type: string): Observable<UserUnlock[]> {
    return this.http.get<UserUnlock[]>(`${this.API_URL}/unlocks/${type}`);
  }

  equipItem(itemType: string, itemKey: string): Observable<void> {
    const request: EquipItemRequest = { itemType, itemKey };
    return this.http.post<void>(`${this.API_URL}/equip`, request);
  }

  syncUnlocks(): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/sync`, {});
  }

  // --- Public/Admin Catalog (Needed so the User sees What is Locked) ---

  getAllCatalogItems(): Observable<CustomizationItem[]> {
    return this.http.get<CustomizationItem[]>(this.ADMIN_API);
  }

  getCatalogItemsByType(type: string): Observable<CustomizationItem[]> {
    return this.http.get<CustomizationItem[]>(`${this.ADMIN_API}/type/${type}`);
  }
}
