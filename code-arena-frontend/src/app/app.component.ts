import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthUserSyncService } from './core/auth/auth-user-sync.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />'
})
export class AppComponent {
  private readonly authUserSync = inject(AuthUserSyncService);

  constructor() {
    this.authUserSync.keepAlive();
  }
}
