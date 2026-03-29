import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { environment } from '../../../environments/environment';
import { AuthUserSyncService } from '../../core/auth/auth-user-sync.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <header class="navbar">
      <a class="brand" routerLink="/">
        <span class="brand-kicker">CODEARENA</span>
        <span class="brand-title">SYSTEM PLATFORM</span>
      </a>
      <nav class="links">
        <a routerLink="/challenge" routerLinkActive="active">
          <span class="link-label">CHALLENGES</span>
        </a>
        <a routerLink="/battle" routerLinkActive="active">
          <span class="link-label">BATTLES</span>
        </a>
        <a routerLink="/shop" routerLinkActive="active">
          <span class="link-label">GEAR SHOP</span>
        </a>
        <a routerLink="/reward-profile" routerLinkActive="active">
          <span class="link-label">REWARDS</span>
        </a>
        <a routerLink="/terminal-quest" routerLinkActive="active">
          <span class="link-label">TERMINAL QUEST</span>
        </a>
          
        <a routerLink="/arenatalk" routerLinkActive="active">
  <span class="link-label">COMMUNITY ARENA</span>
</a>
        <a *ngIf="(currentUser$ | async)?.role === 'ADMIN'" routerLink="/admin/dashboard" routerLinkActive="active" class="admin-link">
          <span class="link-label">BACKOFFICE</span>
        </a>
        
      </nav>

      <div class="actions">
        <a *ngIf="!(isAuthenticated$ | async)" class="btn auth-btn" routerLink="/login">LOGIN_</a>
        
        <ng-container *ngIf="isAuthenticated$ | async">
          <div class="user-info" *ngIf="currentUser$ | async as user">
             <span class="user-role">{{ user.role }}</span>
             <span class="user-name">{{  'OPERATOR' }}</span>
          </div>
          <button class="btn secondary profile-btn" (click)="goProfile()">PROFILE</button>
          <button class="btn danger logout-btn" (click)="logout()">LOGOUT</button>
        </ng-container>
      </div>

      <div class="header-line"></div>
    </header>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Rajdhani:wght@400;500;600;700&display=swap');

    .navbar {
      position: sticky;
      top: 0;
      z-index: 1000;
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 32px;
      padding: 0 40px;
      height: 72px;
      background: rgba(13, 13, 21, 0.95);
      backdrop-filter: blur(10px);
      border-bottom: 1px solid #1a1a2e;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
      font-family: 'Rajdhani', sans-serif;
    }

    /* Top accent line */
    .header-line {
      position: absolute;
      bottom: -1px;
      left: 0;
      width: 100%;
      height: 1px;
      background: linear-gradient(90deg, transparent, #8b5cf6, #06b6d4, transparent);
      opacity: 0.6;
    }

    .brand {
      display: flex;
      flex-direction: column;
      text-decoration: none;
      line-height: 1.1;
      transition: transform 0.2s;
    }

    .brand:hover { transform: scale(1.02); }

    .brand-kicker {
      font-family: 'Orbitron', monospace;
      font-size: 10px;
      letter-spacing: 3px;
      color: #8b5cf6;
      font-weight: 900;
      text-shadow: 0 0 10px rgba(139, 92, 246, 0.5);
    }

    .brand-title {
      font-family: 'Orbitron', monospace;
      font-size: 14px;
      letter-spacing: 1px;
      font-weight: 700;
      color: #e2e8f0;
    }

    .links {
      display: flex;
      gap: 4px;
      height: 100%;
      align-items: center;
    }

    .links a {
      font-family: 'Orbitron', monospace;
      color: #94a3b8;
      text-decoration: none;
      font-size: 11px;
      letter-spacing: 1.5px;
      padding: 10px 18px;
      transition: all 0.3s;
      position: relative;
      height: 100%;
      display: flex;
      align-items: center;
    }

    .links a::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      width: 0;
      height: 2px;
      background: #8b5cf6;
      transition: width 0.3s;
      box-shadow: 0 0 10px #8b5cf6;
    }

    .links a:hover, .links a.active {
      color: #fff;
      background: rgba(139, 92, 246, 0.05);
    }

    .links a.active::after { width: 100%; }

    .admin-link {
      border-left: 1px solid #1a1a2e;
      margin-left: 10px;
      padding-left: 28px !important;
    }

    .link-label { position: relative; z-index: 1; }

    .actions { display: flex; gap: 12px; align-items: center; }

    .user-info {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      margin-right: 12px;
      line-height: 1.2;
    }

    .user-role {
      font-size: 8px;
      color: #06b6d4;
      font-family: 'Orbitron', monospace;
      letter-spacing: 1px;
    }

    .user-name {
      font-size: 12px;
      color: #cbd5e1;
      font-weight: 600;
    }

    .btn {
      background: transparent;
      border: 1px solid #1a1a2e;
      color: #94a3b8;
      padding: 8px 16px;
      border-radius: 4px;
      cursor: pointer;
      font-family: 'Orbitron', monospace;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 1.5px;
      transition: all 0.3s;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
      text-decoration: none;
    }

    .auth-btn { border-color: #8b5cf6; color: #8b5cf6; }
    .auth-btn:hover { background: #8b5cf6; color: #fff; box-shadow: 0 0 20px rgba(139, 92, 246, 0.4); }

    .secondary { border-color: #06b6d4; color: #06b6d4; }
    .secondary:hover { background: #06b6d4; color: #fff; box-shadow: 0 0 20px rgba(6, 182, 212, 0.4); }

    .danger { border-color: #ef4444; color: #ef4444; }
    .danger:hover { background: #ef4444; color: #fff; box-shadow: 0 0 20px rgba(239, 68, 68, 0.4); }
  `]
})
export class NavbarComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly authUserSync = inject(AuthUserSyncService);

  readonly isAuthenticated$ = this.auth.isAuthenticated$;
  readonly currentUser$ = this.authUserSync.currentUser$;

  goProfile(): void {
    void this.router.navigate(['/profile']);
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
