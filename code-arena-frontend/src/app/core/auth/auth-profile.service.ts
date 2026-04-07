import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '@auth0/auth0-angular';
import { firstValueFrom, filter, map } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthProfileService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  public role = signal<'PARTICIPANT' | 'COACH' | 'ADMIN' | null>(null);
  public ready = signal<boolean>(false);

  constructor() {
    this.auth.isAuthenticated$.subscribe(isAuth => {
      if (isAuth) {
        this.loadProfile();
      } else {
        this.role.set(null);
        this.ready.set(true);
      }
    });
  }

  public async loadProfile() {
    try {
      // Wait for a valid token (auth0 handles it seamlessly)
      const token = await firstValueFrom(this.auth.getAccessTokenSilently());
      if (token) {
        // Fetch role directly from Spring Boot Database using the token!
        const profile = await firstValueFrom(this.http.get<any>('http://localhost:8080/api/users/me'));
        this.role.set(profile.role);
      }
    } catch (err) {
      console.error('Failed to load profile from backend', err);
      // Fallback
      this.role.set('PARTICIPANT');
    } finally {
      this.ready.set(true);
    }
  }

  public async getRole(): Promise<string> {
    if (!this.ready()) {
      // If not yet loaded, try loading manually
      await this.loadProfile();
    }
    return this.role() || 'PARTICIPANT';
  }
}
