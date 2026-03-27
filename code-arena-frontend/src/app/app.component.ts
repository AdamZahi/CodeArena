import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthUserSyncService } from './core/auth/auth-user-sync.service';
import { Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />'
})
export class AppComponent {
  private readonly authUserSync = inject(AuthUserSyncService);
  private readonly router = inject(Router);

  constructor() {
    this.authUserSync.keepAlive();
    this.authUserSync.currentUser$
      .pipe(filter((user) => !!user))
      .subscribe((user) => {
        const shouldRedirectToAdminLanding = this.router.url === '/' || this.router.url.startsWith('/login');
        if (user?.role === 'ADMIN' && shouldRedirectToAdminLanding) {
          void this.router.navigateByUrl('/admin/dashboard');
        }
    });
  }
}