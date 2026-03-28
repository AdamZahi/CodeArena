import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../shared/layout/navbar.component';
import { FooterComponent } from '../shared/layout/footer.component';

@Component({
  selector: 'app-bo-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, NavbarComponent, FooterComponent],
  template: `
    <div class="layout">
      <app-navbar />
      <div class="admin-shell">
        <aside class="sidebar">
          <div class="sidebar-header">
            <h3>BACKOFFICE</h3>
            <p class="side-sub">ADMIN CONTROL CENTER_</p>
          </div>
          <div class="sidebar-menu">
            <a routerLink="/admin/dashboard" routerLinkActive="active">
              <span class="icon">▤</span> DASHBOARD
            </a>
            <a routerLink="/admin/users" routerLinkActive="active">
              <span class="icon">▢</span> USERS
            </a>
            <a routerLink="/admin/challenges" routerLinkActive="active">
              <span class="icon">◊</span> CHALLENGES
            </a>
            <a routerLink="/admin/problems" routerLinkActive="active">
              <span class="icon">⌥</span> PROBLEMS
            </a>
            <a routerLink="/admin/battles" routerLinkActive="active">
              <span class="icon">⚔</span> BATTLES
            </a>
            <a routerLink="/admin/shop" routerLinkActive="active">
              <span class="icon">🛒</span> SHOP
            </a>
            <a routerLink="/admin/reports" routerLinkActive="active">
              <span class="icon">🗒</span> REPORTS
            </a>
            <a routerLink="/admin/events" routerLinkActive="active">
              <span class="icon">📅</span> EVENTS
            </a>
            <a routerLink="/admin/coaching" routerLinkActive="active">
              <span class="icon">🎓</span> COACHING
            </a>
            <a routerLink="/admin/terminal-quest" routerLinkActive="active">
              <span class="icon">⌨</span> TERMINAL QUEST
            </a>
          </div>
          <div class="sidebar-footer">
            <span class="v-status">SYSTEM SECURE_</span>
          </div>
        </aside>
        <main class="content">
          <router-outlet />
        </main>
      </div>
      <app-footer />
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Rajdhani:wght@400;500;600;700&display=swap');

    .layout {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      background: #0a0a0f;
      color: #e2e8f0;
      font-family: 'Rajdhani', sans-serif;
      position: relative;
    }

    /* Animated background effects */
    .layout::before {
      content: '';
      position: fixed;
      top: 0; left: 0;
      width: 100%; height: 2px;
      background: linear-gradient(90deg, transparent, #8b5cf6, transparent);
      animation: scan-line 4s linear infinite;
      opacity: 0.15;
      pointer-events: none;
      z-index: 9999;
    }

    .layout::after {
      content: '';
      position: fixed;
      inset: 0;
      background-image: 
        linear-gradient(rgba(139,92,246,0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(139,92,246,0.03) 1px, transparent 1px);
      background-size: 40px 40px;
      pointer-events: none;
      z-index: 0;
    }

    @keyframes scan-line {
      0% { transform: translateY(-100%); }
      100% { transform: translateY(100vh); }
    }

    .admin-shell {
      display: grid;
      grid-template-columns: 280px 1fr;
      flex: 1;
      height: calc(100vh - 72px - 64px);
      position: relative;
      z-index: 1;
    }

    .sidebar {
      background: rgba(13, 13, 21, 0.8);
      border-right: 1px solid #1a1a2e;
      display: flex;
      flex-direction: column;
      padding: 0;
      backdrop-filter: blur(5px);
    }

    .sidebar-header {
      padding: 30px 24px;
      border-bottom: 1px solid #1a1a2e;
    }

    .sidebar-header h3 {
      margin: 0;
      font-family: 'Orbitron', monospace;
      font-size: 18px;
      font-weight: 900;
      letter-spacing: 2px;
      background: linear-gradient(135deg, #8b5cf6, #06b6d4);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .side-sub {
      margin: 4px 0 0;
      color: #64748b;
      font-size: 8px;
      letter-spacing: 3px;
      font-family: 'Orbitron', monospace;
    }

    .sidebar-menu {
      flex: 1;
      padding: 20px 12px;
      display: flex;
      flex-direction: column;
      gap: 4px;
      overflow-y: auto;
    }

    .sidebar-menu a {
      color: #94a3b8;
      text-decoration: none;
      padding: 12px 16px;
      border-radius: 4px;
      font-family: 'Orbitron', monospace;
      font-size: 11px;
      letter-spacing: 2px;
      transition: all 0.3s;
      display: flex;
      align-items: center;
      gap: 12px;
      border: 1px solid transparent;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }

    .icon {
      font-size: 14px;
      width: 20px;
      color: #64748b;
      transition: color 0.3s;
    }

    .sidebar-menu a:hover {
      background: rgba(139, 92, 246, 0.05);
      color: #8b5cf6;
      border-color: rgba(139, 92, 246, 0.2);
    }

    .sidebar-menu a:hover .icon { color: #8b5cf6; }

    .sidebar-menu a.active {
      background: linear-gradient(90deg, rgba(139, 92, 246, 0.15), transparent);
      color: #8b5cf6;
      border-left: 3px solid #8b5cf6;
      box-shadow: 0 0 15px rgba(139, 92, 246, 0.1);
    }

    .sidebar-menu a.active .icon { color: #8b5cf6; }

    .sidebar-footer {
      padding: 20px;
      border-top: 1px solid #1a1a2e;
      text-align: center;
    }

    .v-status {
      font-family: 'Orbitron', monospace;
      font-size: 8px;
      color: #06b6d4;
      letter-spacing: 2px;
      text-shadow: 0 0 5px rgba(6, 182, 212, 0.5);
    }

    .content {
      padding: 40px;
      overflow-y: auto;
      background: rgba(10, 10, 15, 0.4);
      position: relative;
    }
  `]
})
export class BoShellComponent {}
