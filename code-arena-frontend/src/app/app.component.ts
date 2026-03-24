import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthUserSyncService } from './core/auth/auth-user-sync.service';
import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />'
})
export class AppComponent {
  private readonly authUserSync = inject(AuthUserSyncService);
  private readonly auth = inject(AuthService); 

  constructor() {
    this.authUserSync.keepAlive();
    this.auth.user$.pipe(take(1)).subscribe(user => {
      console.log('MY SUB:', user?.sub);
    });
  }
}