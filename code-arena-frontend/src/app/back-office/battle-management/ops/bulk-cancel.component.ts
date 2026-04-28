import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BattleRoomAdmin } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-bulk-cancel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <header>
        <h3>BULK CANCEL</h3>
        <p class="hint">Select open rooms below and provide a reason.</p>
      </header>

      @if (loading()) {
        <div class="skeleton"></div>
      } @else if (rooms().length === 0) {
        <p class="empty">No open rooms available.</p>
      } @else {
        <div class="control-row">
          <label class="check"><input type="checkbox" [checked]="allSelected()" (change)="toggleAll($event)" /> Select all</label>
          <span class="counter">{{ selected().size }} selected</span>
        </div>
        <ul>
          @for (r of rooms(); track r.id) {
            <li>
              <label>
                <input type="checkbox" [checked]="selected().has(r.id)" (change)="toggle(r.id, $event)" />
                <span class="mono">{{ r.id.slice(0, 8) }}</span>
                <span class="meta">{{ r.status }} · {{ r.mode }} · host {{ r.hostUsername ?? '—' }}</span>
              </label>
            </li>
          }
        </ul>

        <label class="reason">
          <span>REASON (required)</span>
          <textarea rows="2" [(ngModel)]="reason" placeholder="Why are these rooms being cancelled?"></textarea>
        </label>

        @if (step() === 1) {
          <div class="footer">
            <button class="btn warn" [disabled]="!canProceed()" (click)="step.set(2)">CONTINUE</button>
          </div>
        } @else {
          <div class="confirm">
            <p>About to cancel <strong>{{ selected().size }}</strong> room(s). This is irreversible.</p>
            <div class="footer">
              <button class="btn ghost" (click)="step.set(1)">‹ BACK</button>
              <button class="btn danger" [disabled]="submitting()" (click)="submit()">
                {{ submitting() ? 'CANCELLING…' : 'CONFIRM CANCEL' }}
              </button>
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .card { background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px; padding: 18px 20px; display: flex; flex-direction: column; gap: 12px; }
    h3 { margin: 0; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; color: #94a3b8; }
    .hint { margin: 4px 0 0; color: #64748b; font-size: 12px; }
    .control-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid #1a1a2e; }
    .check { display: flex; align-items: center; gap: 8px; color: #94a3b8; font-size: 12px; font-family: 'Orbitron', monospace; letter-spacing: 1.5px; }
    .counter { color: #06b6d4; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.5px; }
    ul { list-style: none; padding: 0; margin: 0; max-height: 280px; overflow-y: auto; display: flex; flex-direction: column; gap: 4px; }
    li { padding: 6px 4px; border-radius: 3px; }
    li:hover { background: rgba(139,92,246,0.05); }
    li label { display: flex; align-items: center; gap: 10px; cursor: pointer; }
    .mono { font-family: 'Orbitron', monospace; color: #e2e8f0; font-size: 13px; }
    .meta { color: #94a3b8; font-size: 12px; }
    .reason { display: flex; flex-direction: column; gap: 6px; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; color: #64748b; }
    textarea {
      background: #0a0a0f; border: 1px solid #1a1a2e; color: #e2e8f0;
      padding: 8px 10px; font-family: 'Rajdhani', sans-serif; font-size: 14px; border-radius: 3px; color-scheme: dark; resize: vertical;
    }
    .footer { display: flex; justify-content: flex-end; gap: 8px; }
    .btn { padding: 8px 16px; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.5px; cursor: pointer; border-radius: 3px; border: 1px solid; }
    .btn.ghost  { background: transparent; border-color: #1a1a2e; color: #94a3b8; }
    .btn.warn   { background: rgba(245,158,11,0.15); border-color: rgba(245,158,11,0.5); color: #f59e0b; }
    .btn.danger { background: rgba(239,68,68,0.15); border-color: rgba(239,68,68,0.5); color: #ef4444; }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .empty { color: #64748b; font-size: 13px; text-align: center; padding: 20px 0; }
    .skeleton { height: 200px; background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a); background-size: 200% 100%; animation: pulse 1.5s ease-in-out infinite; border-radius: 4px; }
    .confirm p { color: #ef4444; font-size: 13px; margin: 0 0 8px; }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `]
})
export class BulkCancelComponent implements OnInit {
  @Output() done = new EventEmitter<void>();

  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly rooms = signal<BattleRoomAdmin[]>([]);
  readonly selected = signal<Set<string>>(new Set());
  readonly step = signal<1 | 2>(1);

  reason = '';

  ngOnInit(): void {
    this.loadOpenRooms();
  }

  loadOpenRooms() {
    this.loading.set(true);
    this.api.listRooms({ size: 100, sort: 'createdAt,desc' }).subscribe({
      next: (page) => {
        this.rooms.set(page.content.filter((r) => r.status === 'WAITING' || r.status === 'IN_PROGRESS'));
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.error('Failed to load rooms: ' + (e?.error?.message ?? ''));
      }
    });
  }

  toggle(id: string, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    const next = new Set(this.selected());
    if (checked) next.add(id); else next.delete(id);
    this.selected.set(next);
  }

  toggleAll(event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.selected.set(checked ? new Set(this.rooms().map((r) => r.id)) : new Set());
  }

  allSelected(): boolean {
    return this.rooms().length > 0 && this.selected().size === this.rooms().length;
  }

  canProceed(): boolean {
    return this.selected().size > 0 && this.reason.trim().length > 0;
  }

  submit() {
    const ids = Array.from(this.selected());
    this.submitting.set(true);
    this.api.bulkCancel(ids, this.reason.trim()).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.toast.success(`Cancelled ${res.cancelled} of ${res.requested} rooms`);
        if (res.notFound.length > 0) {
          this.toast.info(`Skipped ${res.notFound.length} unknown id(s)`);
        }
        this.selected.set(new Set());
        this.step.set(1);
        this.reason = '';
        this.loadOpenRooms();
        this.done.emit();
      },
      error: (e) => {
        this.submitting.set(false);
        this.toast.error('Bulk cancel failed: ' + (e?.error?.message ?? ''));
      }
    });
  }
}
