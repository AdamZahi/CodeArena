import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-host">
      @for (t of toast.toasts(); track t.id) {
        <div class="toast" [class.success]="t.tone === 'success'"
             [class.error]="t.tone === 'error'"
             [class.info]="t.tone === 'info'"
             (click)="toast.dismiss(t.id)">
          {{ t.text }}
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-host {
      position: fixed;
      bottom: 24px;
      right: 24px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      z-index: 9000;
      pointer-events: none;
    }
    .toast {
      pointer-events: auto;
      cursor: pointer;
      min-width: 240px;
      max-width: 360px;
      padding: 12px 16px;
      border-radius: 6px;
      font-family: 'Rajdhani', sans-serif;
      font-size: 14px;
      letter-spacing: 0.5px;
      color: #e2e8f0;
      background: rgba(13, 13, 21, 0.92);
      border-left: 3px solid #8b5cf6;
      box-shadow: 0 8px 30px rgba(0,0,0,0.45);
      backdrop-filter: blur(6px);
    }
    .toast.success { border-left-color: #22c55e; }
    .toast.error   { border-left-color: #ef4444; }
    .toast.info    { border-left-color: #06b6d4; }
  `]
})
export class ToastHostComponent {
  readonly toast = inject(ToastService);
}
