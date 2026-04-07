import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private get returnUrl(): string {
    return this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
  }

  /** Navigate to the custom email registration form */
  registerWithEmail(): void {
    this.router.navigate(['/register/email']);
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
}
