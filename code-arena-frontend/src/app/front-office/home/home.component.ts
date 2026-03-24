import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  imports: [CommonModule]
})
export class HomeComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  isAuthenticated$ = this.auth.isAuthenticated$;
  user$ = this.auth.user$;

  navigate(path: string): void {
    this.router.navigate([path]);
  }

  logout(): void {
    void this.auth.logout({
      logoutParams: {
        returnTo: window.location.origin,
        client_id: environment.auth0ClientId
      }
    });
  }
}
