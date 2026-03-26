import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '@auth0/auth0-angular';
import { BehaviorSubject, EMPTY, Subscription } from 'rxjs';
import { catchError, distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface CurrentUser {
  id?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  role?: string;
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

  keepAlive(): void {}
}