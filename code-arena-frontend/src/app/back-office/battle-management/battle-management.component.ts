import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ToastHostComponent } from './shared/toast-host.component';

@Component({
  selector: 'app-battle-management',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastHostComponent],
  template: `
    <section class="battle-shell">
      <header class="page-header">
        <div>
          <h1>BATTLE COMMAND</h1>
          <p>Monitor, configure, and intervene across the live battle system.</p>
        </div>
      </header>

      <nav class="tabs">
        <a routerLink="analytics" routerLinkActive="active">▤ ANALYTICS</a>
        <a routerLink="rooms" routerLinkActive="active">⚔ ROOMS</a>
        <a routerLink="config" routerLinkActive="active">⚙ CONFIG</a>
        <a routerLink="ops" routerLinkActive="active">⚡ OPS</a>
      </nav>

      <div class="tab-content">
        <router-outlet />
      </div>

      <app-toast-host />
    </section>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Rajdhani:wght@400;500;600;700&display=swap');

    .battle-shell { display: flex; flex-direction: column; gap: 24px; min-height: 100%; }

    .page-header h1 {
      margin: 0;
      font-family: 'Orbitron', monospace;
      font-size: 28px;
      letter-spacing: 4px;
      background: linear-gradient(135deg, #8b5cf6, #06b6d4);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .page-header p { margin: 4px 0 0; color: #94a3b8; font-size: 13px; letter-spacing: 1px; }

    .tabs {
      display: flex;
      gap: 8px;
      border-bottom: 1px solid #1a1a2e;
      padding-bottom: 0;
    }

    .tabs a {
      padding: 10px 20px;
      font-family: 'Orbitron', monospace;
      font-size: 11px;
      letter-spacing: 2px;
      color: #94a3b8;
      text-decoration: none;
      border: 1px solid transparent;
      border-bottom: none;
      border-radius: 4px 4px 0 0;
      transition: all 0.25s;
    }

    .tabs a:hover { color: #8b5cf6; background: rgba(139,92,246,0.05); }

    .tabs a.active {
      color: #8b5cf6;
      background: rgba(139,92,246,0.08);
      border-color: rgba(139,92,246,0.3);
      border-bottom: 1px solid #0a0a0f;
      box-shadow: 0 0 12px rgba(139,92,246,0.15);
    }

    .tab-content { flex: 1; }
  `]
})
export class BattleManagementComponent {}
