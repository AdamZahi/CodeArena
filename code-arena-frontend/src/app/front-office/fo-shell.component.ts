import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationComponent } from './shop/notification/notification.component';
import { NavbarComponent } from '../shared/layout/navbar.component';
import { FooterComponent } from '../shared/layout/footer.component';

@Component({
  selector: 'app-fo-shell',
  standalone: true,
  imports: [RouterOutlet, NotificationComponent, NavbarComponent, FooterComponent],
  template: `
    <div class="layout">
      <app-navbar />
      <main class="content">
        <router-outlet />
      </main>
      <app-footer />
    </div>
    <app-notification />
  `,
  styles: [`
    .layout {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      background: #0a0a0f;
      position: relative;
      overflow: hidden;
    }

    /* Animated background effects */
    .layout::before {
      content: '';
      position: fixed;
      top: 0; left: 0;
      width: 100%; height: 2px;
      background: linear-gradient(90deg, transparent, #8b5cf6, transparent);
      animation: scan-line 4s linear infinite;
      opacity: 0.1;
      pointer-events: none;
      z-index: 9999;
    }

    .layout::after {
      content: '';
      position: fixed;
      inset: 0;
      background-image: 
        linear-gradient(rgba(139,92,246,0.02) 1px, transparent 1px),
        linear-gradient(90deg, rgba(139,92,246,0.02) 1px, transparent 1px);
      background-size: 40px 40px;
      pointer-events: none;
      z-index: 0;
    }

    @keyframes scan-line {
      0% { transform: translateY(-100%); }
      100% { transform: translateY(100vh); }
    }

    .content {
      flex: 1;
      position: relative;
      z-index: 1;
    }
  `]
})
export class FoShellComponent {}