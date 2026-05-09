import { Injectable, signal } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { AuthUserSyncService } from './auth-user-sync.service';

@Injectable({ providedIn: 'root' })
export class AuthProfileService {
  role = signal<string>('');

  constructor(
    private auth: AuthService,
    private authUserSync: AuthUserSyncService
  ) {
    this.authUserSync.currentUser$.subscribe(user => {
      this.role.set(user?.role ?? '');
    });
  }
}
