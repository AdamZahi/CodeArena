import { Component, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <section style="padding: 1.5rem; display: grid; gap: 0.75rem; max-width: 540px;">
      <p>Home works</p>
      <button
        type="button"
        (click)="logout()"
        style="width: fit-content; padding: 0.6rem 1rem; border: 0; border-radius: 8px; cursor: pointer; background: #ef4444; color: #fff; font-weight: 600;"
      >
        Logout (test)
      </button>
    </section>
  `
})
export class HomeComponent {
  private readonly auth = inject(AuthService);

  logout(): void {
    void this.auth.logout({
      logoutParams: {
        returnTo: window.location.origin,
        client_id: environment.auth0ClientId
      }
    });
  }
}
