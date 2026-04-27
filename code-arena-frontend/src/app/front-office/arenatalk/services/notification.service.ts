import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ArenNotification {
  id: number;
  message: string;
  hubName: string;
  hubId: number;
  type: 'ACCEPTED' | 'REJECTED';
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private apiUrl = '/api/arenatalk/notifications';
  private notificationsSubject = new BehaviorSubject<ArenNotification[]>([]);
  notifications$ = this.notificationsSubject.asObservable();

  constructor(private http: HttpClient) {}

  loadNotifications(keycloakId: string): void {
    const params = new HttpParams().set('keycloakId', keycloakId);
    this.http.get<ArenNotification[]>(this.apiUrl, { params }).subscribe({
      next: (notifs) => this.notificationsSubject.next(notifs),
      error: () => this.notificationsSubject.next([])
    });
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(keycloakId: string): Observable<void> {
    const params = new HttpParams().set('keycloakId', keycloakId);
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {}, { params });
  }

  get unreadCount(): number {
    return this.notificationsSubject.value.filter(n => !n.read).length;
  }

  get all(): ArenNotification[] {
    return this.notificationsSubject.value;
  }
}