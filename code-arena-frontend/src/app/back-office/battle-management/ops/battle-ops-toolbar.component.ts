import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-battle-ops-toolbar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toolbar">
      <button class="btn primary" (click)="scanStuck.emit()">⚡ SCAN STUCK ROOMS</button>
      <button class="btn warn" (click)="bulkCancel.emit()">✕ BULK CANCEL</button>
      <button class="btn ghost" (click)="exportData.emit()">⬇ EXPORT DATA</button>
    </div>
  `,
  styles: [`
    .toolbar {
      position: sticky; top: 0; z-index: 5;
      display: flex; gap: 10px;
      padding: 12px 16px;
      background: rgba(13,13,21,0.92);
      border: 1px solid #1a1a2e;
      border-radius: 6px;
      backdrop-filter: blur(8px);
    }
    .btn {
      padding: 9px 18px; font-family: 'Orbitron', monospace; font-size: 11px;
      letter-spacing: 1.5px; cursor: pointer; border-radius: 3px; border: 1px solid;
    }
    .btn.primary { background: rgba(139,92,246,0.15); border-color: #8b5cf6; color: #8b5cf6; }
    .btn.warn    { background: rgba(245,158,11,0.15); border-color: rgba(245,158,11,0.5); color: #f59e0b; }
    .btn.ghost   { background: transparent; border-color: #1a1a2e; color: #94a3b8; }
    .btn:hover   { transform: translateY(-1px); }
  `]
})
export class BattleOpsToolbarComponent {
  @Output() scanStuck = new EventEmitter<void>();
  @Output() bulkCancel = new EventEmitter<void>();
  @Output() exportData = new EventEmitter<void>();
}
