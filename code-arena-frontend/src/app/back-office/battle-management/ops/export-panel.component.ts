import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-export-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <header><h3>EXPORT BATTLES</h3></header>

      <div class="row">
        <label>FROM <input type="datetime-local" [(ngModel)]="from" (change)="estimate()" /></label>
        <label>TO <input type="datetime-local" [(ngModel)]="to" (change)="estimate()" /></label>
        <label>FORMAT
          <select [(ngModel)]="format">
            <option value="csv">CSV</option>
            <option value="json">JSON</option>
          </select>
        </label>
      </div>

      <div class="estimate">
        @if (estimating()) {
          <span>Estimating…</span>
        } @else if (estimatedRows() !== null) {
          ≈ <strong>{{ estimatedRows() }}</strong> battles in selected range
        } @else {
          Pick a date range to see an estimate.
        }
      </div>

      <button class="btn primary" [disabled]="downloading()" (click)="download()">
        {{ downloading() ? 'PREPARING…' : '⬇ DOWNLOAD' }}
      </button>

      @if (downloading()) {
        <div class="progress"><span></span></div>
      }
    </div>
  `,
  styles: [`
    .card { background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px; padding: 18px 20px; display: flex; flex-direction: column; gap: 14px; }
    h3 { margin: 0; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; color: #94a3b8; }
    .row { display: flex; gap: 14px; flex-wrap: wrap; }
    label { display: flex; flex-direction: column; gap: 6px; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; color: #64748b; }
    input, select {
      background: #0a0a0f; border: 1px solid #1a1a2e; color: #e2e8f0;
      padding: 7px 10px; font-family: 'Rajdhani', sans-serif; font-size: 13px; border-radius: 3px; color-scheme: dark;
    }
    .estimate { color: #94a3b8; font-size: 13px; }
    .estimate strong { color: #06b6d4; font-family: 'Orbitron', monospace; }
    .btn.primary {
      align-self: flex-start; padding: 10px 18px; font-family: 'Orbitron', monospace;
      font-size: 11px; letter-spacing: 1.5px; cursor: pointer; border-radius: 3px;
      border: 1px solid #06b6d4; background: rgba(6,182,212,0.15); color: #06b6d4;
    }
    .btn.primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .progress { height: 3px; background: #1a1a2e; border-radius: 2px; overflow: hidden; }
    .progress span { display: block; height: 100%; width: 30%; background: linear-gradient(90deg, transparent, #06b6d4, transparent); animation: indeterminate 1.4s linear infinite; }
    @keyframes indeterminate { 0% { transform: translateX(-100%); } 100% { transform: translateX(400%); } }
  `]
})
export class ExportPanelComponent {
  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);

  readonly estimating = signal(false);
  readonly estimatedRows = signal<number | null>(null);
  readonly downloading = signal(false);

  from = '';
  to = '';
  format: 'csv' | 'json' = 'csv';

  estimate() {
    const fromIso = this.from ? `${this.from}:00` : undefined;
    const toIso = this.to ? `${this.to}:00` : undefined;
    if (!fromIso && !toIso) {
      this.estimatedRows.set(null);
      return;
    }
    this.estimating.set(true);
    this.api.estimateExport(fromIso, toIso).subscribe({
      next: (r) => { this.estimatedRows.set(r.estimatedRows); this.estimating.set(false); },
      error: () => { this.estimating.set(false); this.estimatedRows.set(null); }
    });
  }

  download() {
    const fromIso = this.from ? `${this.from}:00` : undefined;
    const toIso = this.to ? `${this.to}:00` : undefined;
    this.downloading.set(true);
    this.api.exportBattles(fromIso, toIso, this.format).subscribe({
      next: (blob) => {
        this.downloading.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `battles-${new Date().toISOString().slice(0, 10)}.${this.format}`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        this.toast.success('Export downloaded');
      },
      error: (e) => {
        this.downloading.set(false);
        this.toast.error('Export failed: ' + (e?.error?.message ?? e?.message ?? ''));
      }
    });
  }
}
