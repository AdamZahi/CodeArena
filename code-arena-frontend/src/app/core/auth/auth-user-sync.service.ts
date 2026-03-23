import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '@auth0/auth0-angular';
import { EMPTY, Subscription } from 'rxjs';
import { catchError, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthUserSyncService {
  private readonly subscription: Subscription;

  constructor(private readonly auth: AuthService, private readonly http: HttpClient) {
    // Trigger one authenticated backend call after login so UserSyncFilter persists the user.
    this.subscription = this.auth.isAuthenticated$
      .pipe(
        distinctUntilChanged(),
        filter((isAuthenticated) => isAuthenticated),
        switchMap(() =>
          this.http.get(`${environment.apiBaseUrl}/api/users/me`).pipe(catchError(() => EMPTY))
        )
      )
      .subscribe();
  }

  keepAlive(): void {
    // Intentionally empty. Injecting this service activates the sync subscription.
  }
}
