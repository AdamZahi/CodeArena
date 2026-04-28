import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService, AlertData } from '../../services/alert.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-cyber-alert',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="alert-overlay" *ngIf="activeAlert" [class.show]="isVisible">
      <div class="alert-modal" [class.error]="activeAlert.type === 'ERROR'" [class.success]="activeAlert.type === 'SUCCESS'" [class.warning]="activeAlert.type === 'WARNING'">
        <div class="alert-decorator top-left"></div>
        <div class="alert-decorator top-right"></div>
        <div class="alert-decorator bottom-left"></div>
        <div class="alert-decorator bottom-right"></div>
        
        <div class="alert-content">
          <div class="alert-header">
            <span class="alert-icon">{{ getIcon() }}</span>
            <h2 class="alert-title">{{ activeAlert.title }}</h2>
          </div>
          
          <div class="alert-body">
            <p>{{ activeAlert.message }}</p>
          </div>
          
          <div class="alert-footer">
            <button class="alert-btn cancel" *ngIf="activeAlert.isConfirm" (click)="close(false)">
              {{ activeAlert.cancelText || 'CANCEL' }}
            </button>
            <button class="alert-btn action" (click)="close(true)">
              {{ activeAlert.confirmText || 'OK' }}
            </button>
          </div>
        </div>
        
        <div class="scanline"></div>
      </div>
    </div>
  `,
  styles: [`
    .alert-overlay {
      position: fixed; top: 0; left: 0; width: 100%; height: 100%;
      background: rgba(0, 0, 0, 0.85); display: flex; align-items: center; justify-content: center;
      z-index: 10000; opacity: 0; pointer-events: none; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      backdrop-filter: blur(8px);
    }
    .alert-overlay.show { opacity: 1; pointer-events: all; }

    .alert-modal {
      width: 100%; max-width: 450px; background: #0d0d15; border: 1px solid #1a1a2e;
      position: relative; overflow: hidden; transform: scale(0.9) translateY(20px);
      transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      clip-path: polygon(20px 0%, 100% 0%, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0% 100%, 0% 20px);
      box-shadow: 0 0 40px rgba(0, 0, 0, 0.5);
    }
    .alert-overlay.show .alert-modal { transform: scale(1) translateY(0); }

    /* DECORATORS */
    .alert-decorator { position: absolute; width: 10px; height: 10px; border: 2px solid #8b5cf6; }
    .top-left { top: 0; left: 0; border-right: none; border-bottom: none; }
    .top-right { top: 0; right: 0; border-left: none; border-bottom: none; }
    .bottom-left { bottom: 0; left: 0; border-right: none; border-top: none; }
    .bottom-right { bottom: 3px; right: 3px; border-left: none; border-top: none; }

    .alert-content { padding: 2.5rem; position: relative; z-index: 2; }
    
    .alert-header { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem; }
    .alert-icon { font-size: 1.5rem; }
    .alert-title { font-family: 'Orbitron', sans-serif; font-size: 1.2rem; font-weight: 900; letter-spacing: 2px; color: #fff; margin: 0; text-transform: uppercase; }

    .alert-body { margin-bottom: 2.5rem; }
    .alert-body p { color: #94a3b8; font-family: 'Fira Code', monospace; line-height: 1.6; font-size: 0.95rem; margin: 0; }

    .alert-footer { display: flex; justify-content: flex-end; gap: 1rem; }
    
    .alert-btn {
      padding: 10px 24px; font-family: 'Orbitron', monospace; font-size: 11px; font-weight: 900; letter-spacing: 1px;
      cursor: pointer; transition: all 0.3s; border: none;
      clip-path: polygon(8px 0%, 100% 0%, calc(100% - 8px) 100%, 0% 100%);
    }
    .alert-btn.action { background: #8b5cf6; color: #fff; box-shadow: 0 0 15px rgba(139, 92, 246, 0.3); }
    .alert-btn.action:hover { background: #fff; color: #000; box-shadow: 0 0 25px rgba(139, 92, 246, 0.6); }
    
    .alert-btn.cancel { background: rgba(255, 255, 255, 0.05); color: #64748b; border: 1px solid rgba(255, 255, 255, 0.1); }
    .alert-btn.cancel:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }

    /* TYPES */
    .alert-modal.error { border-color: #f43f5e; }
    .alert-modal.error .alert-title { color: #f43f5e; }
    .alert-modal.error .alert-btn.action { background: #f43f5e; }
    .alert-modal.error .alert-decorator { border-color: #f43f5e; }

    .alert-modal.success { border-color: #10b981; }
    .alert-modal.success .alert-title { color: #10b981; }
    .alert-modal.success .alert-btn.action { background: #10b981; }
    .alert-modal.success .alert-decorator { border-color: #10b981; }

    .alert-modal.warning { border-color: #ecc94b; }
    .alert-modal.warning .alert-title { color: #ecc94b; }
    .alert-modal.warning .alert-btn.action { background: #ecc94b; color: #000; }
    .alert-modal.warning .alert-decorator { border-color: #ecc94b; }

    .scanline {
      width: 100%; height: 100px; z-index: 1; position: absolute; top: 0; left: 0;
      background: linear-gradient(0deg, rgba(0, 0, 0, 0) 0%, rgba(255, 255, 255, 0.02) 50%, rgba(0, 0, 0, 0) 100%);
      animation: scan 4s linear infinite;
    }
    @keyframes scan { from { transform: translateY(-100px); } to { transform: translateY(500px); } }
  `]
})
export class CyberAlertComponent implements OnInit, OnDestroy {
  activeAlert: AlertData | null = null;
  isVisible = false;
  private sub: Subscription | null = null;

  constructor(private alertService: AlertService) {}

  ngOnInit() {
    this.sub = this.alertService.alertState$.subscribe((data) => {
      this.activeAlert = data;
      setTimeout(() => this.isVisible = true, 50);
    });
  }

  ngOnDestroy() {
    if (this.sub) this.sub.unsubscribe();
  }

  getIcon(): string {
    if (!this.activeAlert) return 'ℹ️';
    switch (this.activeAlert.type) {
      case 'SUCCESS': return '✅';
      case 'ERROR': return '🚫';
      case 'WARNING': return '⚠️';
      default: return 'ℹ️';
    }
  }

  close(result: boolean) {
    this.isVisible = false;
    setTimeout(() => {
      if (this.activeAlert?.resolve) {
        this.activeAlert.resolve(result);
      }
      this.activeAlert = null;
    }, 300);
  }
}
