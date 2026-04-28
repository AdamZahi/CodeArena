import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BattleRoomDetail } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-force-end-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="overlay" (click)="onOverlayClick($event)">
      <div class="modal" role="dialog" aria-modal="true">
        <header>
          <h3>FORCE END BATTLE</h3>
          <button class="close" (click)="closed.emit()" aria-label="Close">×</button>
        </header>

        <div class="body">
          @if (step() === 1) {
            <p>Manually finish room <strong class="mono">{{ shortId(room.id) }}</strong>.
              Choose a winner (or no winner for a draw).</p>

            <label>
              <span>WINNER</span>
              <select [(ngModel)]="winnerId">
                <option [ngValue]="null">— No winner (draw) —</option>
                @for (p of room.participants; track p.id) {
                  <option [ngValue]="p.userId">{{ p.username ?? p.userId }}</option>
                }
              </select>
            </label>

            <label>
              <span>REASON (required)</span>
              <textarea rows="3" [(ngModel)]="reason"
                        placeholder="Why is this battle being force-ended?"></textarea>
            </label>

            <div class="impact">
              {{ room.participants.length }} participants will be affected.
              XP will be awarded to the winner and consolation XP to the rest.
            </div>
          } @else {
            <p>You're about to force-end this battle. <strong>This cannot be undone.</strong></p>
            <ul class="summary">
              <li>Winner: <strong>{{ winnerLabel() }}</strong></li>
              <li>Reason: <em>{{ reason }}</em></li>
            </ul>
          }
        </div>

        <footer>
          @if (step() === 1) {
            <button class="btn ghost" (click)="closed.emit()">CANCEL</button>
            <button class="btn warn" [disabled]="!canProceed()" (click)="step.set(2)">CONTINUE</button>
          } @else {
            <button class="btn ghost" (click)="step.set(1)">‹ BACK</button>
            <button class="btn danger" [disabled]="submitting()" (click)="confirm()">
              {{ submitting() ? 'PROCESSING…' : 'CONFIRM FORCE END' }}
            </button>
          }
        </footer>
      </div>
    </div>
  `,
  styles: [`
    .overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.65); backdrop-filter: blur(2px);
      display: flex; align-items: center; justify-content: center; z-index: 7000;
    }
    .modal {
      width: min(540px, 92vw); background: #0f0f1a; border: 1px solid #1a1a2e; border-radius: 6px;
      box-shadow: 0 24px 64px rgba(0,0,0,0.6);
    }
    header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px 20px; border-bottom: 1px solid #1a1a2e;
    }
    h3 { margin: 0; font-family: 'Orbitron', monospace; font-size: 13px; letter-spacing: 2px; color: #f59e0b; }
    .close { background: transparent; border: none; color: #94a3b8; font-size: 22px; cursor: pointer; line-height: 1; }
    .body { padding: 20px; display: flex; flex-direction: column; gap: 14px; color: #94a3b8; font-size: 14px; }
    .body p { margin: 0; }
    label { display: flex; flex-direction: column; gap: 6px; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; color: #64748b; }
    select, textarea {
      background: #0a0a0f; border: 1px solid #1a1a2e; color: #e2e8f0;
      padding: 8px 10px; font-family: 'Rajdhani', sans-serif; font-size: 14px; border-radius: 3px;
      color-scheme: dark; resize: vertical;
    }
    .impact {
      padding: 10px 14px; background: rgba(245,158,11,0.08); border-left: 3px solid #f59e0b;
      color: #f59e0b; font-size: 13px; border-radius: 3px;
    }
    .summary { padding-left: 18px; margin: 0; color: #e2e8f0; }
    .summary strong { color: #f59e0b; }
    footer {
      display: flex; justify-content: flex-end; gap: 8px; padding: 14px 20px; border-top: 1px solid #1a1a2e;
    }
    .btn { padding: 8px 16px; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.5px; cursor: pointer; border-radius: 3px; border: 1px solid; }
    .btn.ghost  { background: transparent; border-color: #1a1a2e; color: #94a3b8; }
    .btn.warn   { background: rgba(245,158,11,0.15); border-color: rgba(245,158,11,0.5); color: #f59e0b; }
    .btn.danger { background: rgba(239,68,68,0.15); border-color: rgba(239,68,68,0.5); color: #ef4444; }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .mono { font-family: 'Orbitron', monospace; }
  `]
})
export class ForceEndModalComponent {
  @Input({ required: true }) room!: BattleRoomDetail;
  @Output() closed = new EventEmitter<void>();
  @Output() succeeded = new EventEmitter<void>();

  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);

  readonly step = signal<1 | 2>(1);
  readonly submitting = signal(false);

  winnerId: string | null = null;
  reason = '';

  canProceed(): boolean {
    return this.reason.trim().length > 0;
  }

  shortId(id: string) { return id.slice(0, 8); }

  winnerLabel(): string {
    if (!this.winnerId) return 'Draw (no winner)';
    return this.room.participants.find((p) => p.userId === this.winnerId)?.username ?? this.winnerId;
  }

  onOverlayClick(e: MouseEvent) {
    if (e.target === e.currentTarget && !this.submitting()) this.closed.emit();
  }

  confirm() {
    this.submitting.set(true);
    this.api.forceEnd(this.room.id, this.winnerId, this.reason.trim()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Battle force-ended');
        this.succeeded.emit();
      },
      error: (e) => {
        this.submitting.set(false);
        this.toast.error('Force-end failed: ' + (e?.error?.message ?? e?.message ?? ''));
      }
    });
  }
}
