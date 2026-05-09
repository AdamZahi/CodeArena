import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BattleRoomDetail } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-reassign-winner-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="overlay" (click)="onOverlayClick($event)">
      <div class="modal" role="dialog" aria-modal="true">
        <header>
          <h3>REASSIGN WINNER</h3>
          <button class="close" (click)="closed.emit()" aria-label="Close">×</button>
        </header>

        <div class="body">
          @if (step() === 1) {
            <p>Override the declared winner for room <strong class="mono">{{ shortId(room.id) }}</strong>.</p>
            <div class="current">
              <span class="label">CURRENT WINNER</span>
              <strong>{{ currentWinnerLabel() }}</strong>
            </div>

            <label>
              <span>NEW WINNER</span>
              <select [(ngModel)]="newWinnerId">
                <option [ngValue]="null">— Select —</option>
                @for (p of room.participants; track p.id) {
                  <option [ngValue]="p.userId" [disabled]="p.userId === room.winnerId">
                    {{ p.username ?? p.userId }}
                  </option>
                }
              </select>
            </label>

            <label>
              <span>REASON (required)</span>
              <textarea rows="3" [(ngModel)]="reason"
                        placeholder="Why is the winner being reassigned?"></textarea>
            </label>
          } @else {
            <p>Reassign winner to <strong>{{ newWinnerLabel() }}</strong>?
              The previous winner's XP will be reverted and the new winner will be awarded.</p>
            <ul class="summary">
              <li>From: <strong>{{ currentWinnerLabel() }}</strong></li>
              <li>To: <strong>{{ newWinnerLabel() }}</strong></li>
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
              {{ submitting() ? 'PROCESSING…' : 'CONFIRM REASSIGN' }}
            </button>
          }
        </footer>
      </div>
    </div>
  `,
  styles: [`
    .overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.65); backdrop-filter: blur(2px); display: flex; align-items: center; justify-content: center; z-index: 7000; }
    .modal { width: min(540px, 92vw); background: #0f0f1a; border: 1px solid #1a1a2e; border-radius: 6px; box-shadow: 0 24px 64px rgba(0,0,0,0.6); }
    header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid #1a1a2e; }
    h3 { margin: 0; font-family: 'Orbitron', monospace; font-size: 13px; letter-spacing: 2px; color: #06b6d4; }
    .close { background: transparent; border: none; color: #94a3b8; font-size: 22px; cursor: pointer; }
    .body { padding: 20px; display: flex; flex-direction: column; gap: 14px; color: #94a3b8; font-size: 14px; }
    .body p { margin: 0; }
    .current { padding: 10px 14px; background: #0a0a0f; border: 1px solid #1a1a2e; border-radius: 3px; display: flex; flex-direction: column; gap: 4px; }
    .current .label { font-family: 'Orbitron', monospace; font-size: 9px; letter-spacing: 1.5px; color: #64748b; }
    .current strong { color: #f59e0b; font-size: 15px; }
    label { display: flex; flex-direction: column; gap: 6px; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; color: #64748b; }
    select, textarea { background: #0a0a0f; border: 1px solid #1a1a2e; color: #e2e8f0; padding: 8px 10px; font-family: 'Rajdhani', sans-serif; font-size: 14px; border-radius: 3px; color-scheme: dark; resize: vertical; }
    .summary { padding-left: 18px; margin: 0; color: #e2e8f0; }
    .summary strong { color: #06b6d4; }
    footer { display: flex; justify-content: flex-end; gap: 8px; padding: 14px 20px; border-top: 1px solid #1a1a2e; }
    .btn { padding: 8px 16px; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.5px; cursor: pointer; border-radius: 3px; border: 1px solid; }
    .btn.ghost  { background: transparent; border-color: #1a1a2e; color: #94a3b8; }
    .btn.warn   { background: rgba(6,182,212,0.15); border-color: rgba(6,182,212,0.5); color: #06b6d4; }
    .btn.danger { background: rgba(239,68,68,0.15); border-color: rgba(239,68,68,0.5); color: #ef4444; }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .mono { font-family: 'Orbitron', monospace; }
  `]
})
export class ReassignWinnerModalComponent {
  @Input({ required: true }) room!: BattleRoomDetail;
  @Output() closed = new EventEmitter<void>();
  @Output() succeeded = new EventEmitter<void>();

  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);

  readonly step = signal<1 | 2>(1);
  readonly submitting = signal(false);

  newWinnerId: string | null = null;
  reason = '';

  canProceed(): boolean {
    return !!this.newWinnerId
      && this.reason.trim().length > 0
      && this.newWinnerId !== this.room.winnerId;
  }

  shortId(id: string) { return id.slice(0, 8); }

  currentWinnerLabel(): string {
    if (!this.room.winnerId) return 'No winner declared';
    return this.room.participants.find((p) => p.userId === this.room.winnerId)?.username ?? this.room.winnerId;
  }

  newWinnerLabel(): string {
    if (!this.newWinnerId) return '—';
    return this.room.participants.find((p) => p.userId === this.newWinnerId)?.username ?? this.newWinnerId;
  }

  onOverlayClick(e: MouseEvent) {
    if (e.target === e.currentTarget && !this.submitting()) this.closed.emit();
  }

  confirm() {
    if (!this.newWinnerId) return;
    this.submitting.set(true);
    this.api.reassignWinner(this.room.id, this.newWinnerId, this.reason.trim()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Winner reassigned');
        this.succeeded.emit();
      },
      error: (e) => {
        this.submitting.set(false);
        this.toast.error('Reassign failed: ' + (e?.error?.message ?? e?.message ?? ''));
      }
    });
  }
}
