import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output,
  inject,
  signal
} from '@angular/core';
import { Subscription, interval, startWith, switchMap } from 'rxjs';

import { StuckRoom } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-stuck-rooms-alert',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card" [class.has-stuck]="rooms().length > 0">
      <header>
        <div>
          <h3>STUCK ROOMS</h3>
          <p class="hint">Auto-refresh every 60s · {{ rooms().length }} stuck</p>
        </div>
        <button class="refresh" (click)="reload()">↻</button>
      </header>

      @if (loading()) {
        <div class="skeleton"></div>
      } @else if (rooms().length === 0) {
        <p class="all-clear">✓ ALL CLEAR — no stuck rooms.</p>
      } @else {
        <ul>
          @for (r of rooms(); track r.roomId) {
            <li>
              <div class="info">
                <span class="mono">{{ r.roomId.slice(0, 8) }}</span>
                <span class="meta">{{ r.mode }} · host {{ r.hostUsername ?? r.hostId }}</span>
                <span class="meta time">stuck {{ r.minutesStuck }} min · {{ r.participantCount }} players</span>
              </div>
              <button class="btn warn" (click)="forceEnd.emit(r.roomId)">FORCE END</button>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: [`
    .card { background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px; padding: 18px; }
    .card.has-stuck { border-color: rgba(245,158,11,0.5); box-shadow: 0 0 24px rgba(245,158,11,0.1); }
    header { display: flex; justify-content: space-between; align-items: flex-start; }
    h3 { margin: 0; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; color: #f59e0b; }
    .hint { margin: 4px 0 0; color: #64748b; font-size: 12px; }
    .refresh {
      background: transparent; border: 1px solid #1a1a2e; color: #94a3b8;
      padding: 4px 10px; cursor: pointer; border-radius: 3px; font-size: 14px;
    }
    .refresh:hover { color: #f59e0b; border-color: #f59e0b; }
    .all-clear { color: #22c55e; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 1.5px; margin-top: 14px; }
    ul { list-style: none; padding: 0; margin: 14px 0 0; display: flex; flex-direction: column; gap: 8px; }
    li {
      display: flex; align-items: center; justify-content: space-between;
      padding: 10px 12px; background: rgba(245,158,11,0.05);
      border: 1px solid rgba(245,158,11,0.2); border-radius: 4px;
    }
    .info { display: flex; flex-direction: column; gap: 2px; }
    .mono { font-family: 'Orbitron', monospace; color: #e2e8f0; font-size: 13px; }
    .meta { color: #94a3b8; font-size: 12px; }
    .meta.time { color: #f59e0b; }
    .btn { padding: 6px 14px; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.2px; cursor: pointer; border-radius: 3px; border: 1px solid; }
    .btn.warn { background: rgba(245,158,11,0.15); border-color: rgba(245,158,11,0.5); color: #f59e0b; }
    .skeleton { height: 80px; background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a); background-size: 200% 100%; animation: pulse 1.5s ease-in-out infinite; border-radius: 4px; margin-top: 14px; }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `]
})
export class StuckRoomsAlertComponent implements OnInit, OnDestroy {
  @Output() forceEnd = new EventEmitter<string>();

  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly rooms = signal<StuckRoom[]>([]);

  private sub?: Subscription;

  ngOnInit(): void {
    this.sub = interval(60_000).pipe(
      startWith(0),
      switchMap(() => {
        this.loading.set(true);
        return this.api.stuckRooms();
      })
    ).subscribe({
      next: (rooms) => { this.rooms.set(rooms); this.loading.set(false); },
      error: () => { this.loading.set(false); }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  reload() {
    this.loading.set(true);
    this.api.stuckRooms().subscribe({
      next: (rooms) => { this.rooms.set(rooms); this.loading.set(false); },
      error: (e) => {
        this.loading.set(false);
        this.toast.error('Failed to refresh stuck rooms: ' + (e?.error?.message ?? ''));
      }
    });
  }
}
