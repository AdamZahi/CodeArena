import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  private get returnUrl(): string {
    return this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
  }

  loginWithGoogle(): void {
    void this.auth.loginWithRedirect({
      authorizationParams: { connection: 'google-oauth2' },
      appState: { target: this.returnUrl }
    });
  }

  loginWithGitHub(): void {
    void this.auth.loginWithRedirect({
      authorizationParams: { connection: 'github' },
      appState: { target: this.returnUrl }
    });
  }

  loginWithEmail(): void {
    void this.auth.loginWithRedirect({
      appState: { target: this.returnUrl }
    });
  }
}
