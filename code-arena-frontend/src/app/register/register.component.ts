import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  private get returnUrl(): string {
    return this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
  }

  registerWithGoogle(): void {
    void this.auth.loginWithRedirect({
      authorizationParams: { connection: 'google-oauth2' },
      appState: { target: this.returnUrl }
    });
  }

  registerWithGitHub(): void {
    void this.auth.loginWithRedirect({
      authorizationParams: { connection: 'github' },
      appState: { target: this.returnUrl }
    });
  }

  registerWithEmail(): void {
    void this.auth.loginWithRedirect({
      authorizationParams: { screen_hint: 'signup' },
      appState: { target: this.returnUrl }
    });
  }
}
