import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '@auth0/auth0-angular';
import { BehaviorSubject, EMPTY, Subscription, Observable } from 'rxjs';
import { catchError, distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface CurrentUser {
  id?: string;
  keycloakId?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  nickname?: string;
  role?: string;
  level?: number;
  totalXp?: number;
  activeIconId?: string;
  activeBorderId?: string;
  activeTitle?: string;
  activeBadge1?: string;
  activeBadge2?: string;
  activeBadge3?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthUserSyncService {
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly subscription: Subscription;
  private readonly currentUserSubject = new BehaviorSubject<CurrentUser | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor() {
    this.subscription = this.auth.isAuthenticated$
      .pipe(
        distinctUntilChanged(),
        filter((isAuthenticated) => isAuthenticated),
        switchMap(() =>
          this.http.get<CurrentUser>(`${environment.apiBaseUrl}/api/users/me`)
            .pipe(
              tap((user) => this.currentUserSubject.next(user)),
              catchError(() => {
                this.currentUserSubject.next(null);
                return EMPTY;
              })
            )
        )
      )
      .subscribe();
  }

  forceSync(): void {
    this.getCurrentUserSnapshot().subscribe();
  }

  getCurrentUserSnapshot(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${environment.apiBaseUrl}/api/users/me`)
      .pipe(
        tap((user) => this.currentUserSubject.next(user)),
        catchError(() => EMPTY)
      );
  }

  keepAlive(): void {}
}