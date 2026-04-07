import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthProfileService } from '../../core/auth/auth-profile.service';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <div class="routing-state">
      <div class="spinner"></div>
      <p>Routing Workspace...</p>
    </div>
  `,
  styles: [`
    .routing-state {
      min-height: 100vh; background: #0f0f11; display: flex; flex-direction: column;
      align-items: center; justify-content: center; color: #a1a1a6; font-family: 'Fira Code', monospace;
    }
    .spinner {
      width: 50px; height: 50px; border: 4px solid #28282d; border-top-color: #d03c3c;
      border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 2rem;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class HomeComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly profileService = inject(AuthProfileService);

  async ngOnInit() {
    // Wait until profile is loaded
    const role = await this.profileService.getRole();

    if (role === 'COACH') {
      this.router.navigate(['/coaching-quiz/coach-dashboard']);
    } else {
      this.router.navigate(['/coaching-quiz/coaches']);
    }
  }
}
