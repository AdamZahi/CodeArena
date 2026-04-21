import { Component, OnInit, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { AuthProfileService } from '../../../../core/auth/auth-profile.service';
import { CyberAlertComponent } from '../cyber-alert/cyber-alert.component';

@Component({
  selector: 'app-coaching-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, CyberAlertComponent],
  template: `
    <app-cyber-alert></app-cyber-alert>
    <nav class="lc-header">
      <div class="lc-logo" routerLink="/coaching-quiz/coaches">
        CODE<span class="accent">ARENA</span>
      </div>
      
      <div class="lc-header-right">
        <ng-container *ngIf="!isCoach && !isAdmin">
          <a class="lc-nav-item" routerLink="/coaching-quiz/coaches" routerLinkActive="active" [routerLinkActiveOptions]="{exact: false}">Mentors</a>
          <a class="lc-nav-item" routerLink="/coaching-quiz/sessions" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Dashboard</a>
          <a class="lc-nav-item" routerLink="/coaching-quiz/my-training" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Training</a>
          <a class="lc-nav-item" routerLink="/coaching-quiz/quizzes" routerLinkActive="active" [routerLinkActiveOptions]="{exact: false}">Quizzes</a>
          <a class="lc-nav-item mentor-link" routerLink="/coaching-quiz/ai-code-mentor" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">🎓 AI Mentor</a>
          <a class="lc-nav-item apply-link" routerLink="/coaching-quiz/apply-coach" routerLinkActive="active">Become Coach</a>
        </ng-container>

        <ng-container *ngIf="isCoach">
          <a class="lc-nav-item" routerLink="/coaching-quiz/coach-dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Dashboard</a>
          <a class="lc-nav-item" routerLink="/coaching-quiz/coach-reservations" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">My Sessions</a>
          <a class="lc-nav-item" routerLink="/coaching-quiz/coach-create-session" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">+ New Session</a>
          <a class="lc-nav-item ai-link" routerLink="/coaching-quiz/coach-ai-assistant" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">🧠 AI Architect</a>
        </ng-container>

        <ng-container *ngIf="isAdmin">
          <a class="lc-nav-item admin-link" routerLink="/coaching-quiz/admin/applications" routerLinkActive="active">View Coach Requests (Admin)</a>
        </ng-container>

        <div class="user-profile" (click)="logout()" *ngIf="isAuthenticated">
          <span class="user-name">LOGOUT</span>
          <span class="ac-icon">🚀</span>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    :host {
      --neon: #8b5cf6;
      --neon2: #06b6d4;
      --dark: #0a0a0f;
      --border: #1a1a2e;
      --muted: #64748b;
      --text: #e2e8f0;
    }

    .lc-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.5rem 2rem;
      background: rgba(10, 10, 15, 0.9);
      backdrop-filter: blur(10px);
      border-bottom: 1px solid var(--border);
      position: sticky;
      top: 0;
      z-index: 1000;
      font-family: 'Rajdhani', sans-serif;
    }

    .lc-header::after {
      content: '';
      position: absolute;
      bottom: -1px; left: 0;
      width: 300px; height: 2px;
      background: linear-gradient(90deg, var(--neon), var(--neon2), transparent);
    }

    .lc-logo {
      font-family: 'Orbitron', monospace;
      font-size: 20px;
      font-weight: 900;
      color: var(--neon2);
      letter-spacing: 2px;
      text-transform: uppercase;
      cursor: pointer;
    }

    .lc-logo .accent {
      background: linear-gradient(135deg, #8b5cf6, #06b6d4);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .lc-header-right {
      display: flex;
      align-items: center;
      gap: 1.5rem;
    }

    .lc-nav-item {
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      letter-spacing: 2px;
      color: var(--muted);
      text-transform: uppercase;
      text-decoration: none;
      cursor: pointer;
      transition: color 0.3s ease;
    }

    .lc-nav-item:hover {
      color: var(--neon2);
    }

    .lc-nav-item.active {
      color: var(--neon);
      text-shadow: 0 0 10px rgba(139, 92, 246, 0.5);
    }

    .user-profile {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 16px;
      background: rgba(139, 92, 246, 0.05);
      border: 1px solid rgba(139, 92, 246, 0.2);
      border-radius: 4px;
      transition: all 0.3s;
      cursor: pointer;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }

    .user-profile:hover {
      background: rgba(139, 92, 246, 0.15);
      border-color: var(--neon);
      box-shadow: 0 0 15px rgba(139, 92, 246, 0.2);
    }

    .user-name {
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      font-weight: 700;
      color: var(--text);
      letter-spacing: 1px;
    }

    .ac-icon { font-size: 14px; }

    .apply-link { color: #10b981 !important; }
    .apply-link:hover { text-shadow: 0 0 10px rgba(16, 185, 129, 0.5); }
    .admin-link { color: #f43f5e !important; }
    .admin-link:hover { text-shadow: 0 0 10px rgba(244, 63, 94, 0.5); }
    .ai-link { color: #06b6d4 !important; background: rgba(6,182,212,0.08); padding: 4px 12px; border: 1px solid rgba(6,182,212,0.2); border-radius: 2px; }
    .ai-link:hover { text-shadow: 0 0 12px rgba(6, 182, 212, 0.6); background: rgba(6,182,212,0.15); border-color: rgba(6,182,212,0.4); }
    .ai-link.active { background: rgba(6,182,212,0.2) !important; border-color: #06b6d4 !important; box-shadow: 0 0 15px rgba(6,182,212,0.2); }
    .mentor-link { color: #3b82f6 !important; background: rgba(59,130,246,0.08); padding: 4px 12px; border: 1px solid rgba(59,130,246,0.2); border-radius: 2px; }
    .mentor-link:hover { text-shadow: 0 0 12px rgba(59, 130, 246, 0.6); background: rgba(59,130,246,0.15); border-color: rgba(59,130,246,0.4); }
    .mentor-link.active { background: rgba(59,130,246,0.2) !important; border-color: #3b82f6 !important; box-shadow: 0 0 15px rgba(59,130,246,0.2); }
  `]
})
export class CoachingNavbarComponent implements OnInit {
  isCoach = false;
  isAdmin = false;
  isAuthenticated = false;

  private profileService = inject(AuthProfileService);

  constructor(private auth: AuthService) {
    effect(() => {
      const role = this.profileService.role();
      this.isCoach = (role === 'COACH');
      this.isAdmin = (role === 'ADMIN');
    });
  }

  ngOnInit() {
    this.auth.isAuthenticated$.subscribe(isAuth => this.isAuthenticated = isAuth);
  }

  logout() {
    this.auth.logout({ logoutParams: { returnTo: window.location.origin + '/login' } });
  }
}
