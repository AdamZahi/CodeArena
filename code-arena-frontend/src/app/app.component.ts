import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthUserSyncService } from './core/auth/auth-user-sync.service';
import { AuthService } from '@auth0/auth0-angular';
import { Router } from '@angular/router';
import { filter, take, switchMap, map } from 'rxjs/operators';
import { NotificationService } from './front-office/shop/services/notification.service';
import { NotificationComponent } from './front-office/shop/notification/notification.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet,NotificationComponent],
template: `
  <router-outlet />
  <app-notification />
`
})
export class AppComponent {
  private readonly authUserSync = inject(AuthUserSyncService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  constructor() {
    this.authUserSync.keepAlive();

    // ── REDIRECT ADMIN TO DASHBOARD ───────────────
    this.authUserSync.currentUser$
      .pipe(filter((user) => !!user))
      .subscribe((user) => {
        const shouldRedirectToAdminLanding = this.router.url === '/' || this.router.url.startsWith('/login');
        if (user?.role === 'ADMIN' && shouldRedirectToAdminLanding) {
          void this.router.navigateByUrl('/admin/dashboard');
        }
      });

    // ── CONNECT WEBSOCKET AS SOON AS USER IS KNOWN ──
    // Guarantees WebSocket ready for price updates, order notifs, stock alerts
    // Now passing the access token for backend authentication
    this.auth.user$.pipe(
      filter(user => !!user?.sub),
      take(1),
      switchMap(user => 
        this.auth.getAccessTokenSilently().pipe(
          map(token => ({ user, token }))
        )
      )
    ).subscribe(({ user, token }) => {
      if (user?.sub) {
        this.notificationService.connect(user.sub);
      }
    });

  }
}
