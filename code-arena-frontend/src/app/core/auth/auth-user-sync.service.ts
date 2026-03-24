import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '@auth0/auth0-angular';
import { EMPTY, Subscription } from 'rxjs';
import { catchError, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthUserSyncService {
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly subscription: Subscription;

  constructor() {
    this.subscription = this.auth.isAuthenticated$
      .pipe(
        distinctUntilChanged(),
        filter((isAuthenticated) => isAuthenticated),
        switchMap(() =>
          this.http.get(`${environment.apiBaseUrl}/api/users/me`)
            .pipe(catchError(() => EMPTY))
        )
      )
      .subscribe();
  }

  keepAlive(): void {}
}