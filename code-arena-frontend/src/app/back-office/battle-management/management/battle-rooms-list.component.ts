import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  BattleRoomAdmin,
  BattleRoomStatus,
  RoomListFilters,
  SpringPage
} from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

const STATUS_OPTIONS: BattleRoomStatus[] = [
  'WAITING', 'COUNTDOWN', 'IN_PROGRESS', 'FINISHED', 'CANCELLED'
];

@Component({
  selector: 'app-battle-rooms-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="filters">
      <select [(ngModel)]="filters.status" (change)="reload(0)">
        <option [ngValue]="null">All statuses</option>
        @for (s of statusOptions; track s) { <option [ngValue]="s">{{ s }}</option> }
      </select>
      <input type="text" [(ngModel)]="filters.challengeId" placeholder="Challenge ID" (change)="reload(0)" />
      <input type="text" [(ngModel)]="filters.hostId" placeholder="Host ID (auth0|...)" (change)="reload(0)" />
      <input type="datetime-local" [(ngModel)]="filters.from" (change)="reload(0)" />
      <input type="datetime-local" [(ngModel)]="filters.to" (change)="reload(0)" />
      <button class="reset" (click)="resetFilters()">RESET</button>
    </div>

    @if (loading()) {
      <div class="skeleton"></div>
    } @else {
      @if (page(); as p) {
        @if (p.content.length === 0) {
          <div class="empty">No battle rooms match the current filters.</div>
        } @else {
          <table>
            <thead>
              <tr>
                <th (click)="sortBy('id')">ROOM ID</th>
                <th>CHALLENGE</th>
                <th>HOST</th>
                <th>STATUS</th>
                <th>MODE</th>
                <th class="num">PLAYERS</th>
                <th (click)="sortBy('createdAt')">CREATED</th>
                <th class="actions">ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              @for (r of p.content; track r.id) {
                <tr>
                  <td class="mono"><a [routerLink]="['..', 'rooms', r.id]">{{ shortId(r.id) }}</a></td>
                  <td>{{ r.challengeTitle ?? '—' }}</td>
                  <td>{{ r.hostUsername ?? '—' }}</td>
                  <td>
                    <span class="status" [class.waiting]="r.status === 'WAITING'"
                                         [class.in-progress]="r.status === 'IN_PROGRESS'"
                                         [class.finished]="r.status === 'FINISHED'"
                                         [class.cancelled]="r.status === 'CANCELLED'">
                      {{ r.status }}
                    </span>
                  </td>
                  <td>{{ r.mode }}</td>
                  <td class="num">{{ r.participantCount }}</td>
                  <td>{{ r.createdAt | date:'short' }}</td>
                  <td class="actions">
                    <a class="btn ghost" [routerLink]="['..', 'rooms', r.id]">View</a>
                    @if (r.status === 'IN_PROGRESS' || r.status === 'WAITING') {
                      <button class="btn warn" (click)="forceClose(r)">Cancel</button>
                    }
                    <button class="btn danger" (click)="confirmDelete(r)">Delete</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>

          <div class="pager">
            <button [disabled]="p.first" (click)="reload(p.number - 1)">‹ PREV</button>
            <span>Page {{ p.number + 1 }} / {{ p.totalPages || 1 }} · {{ p.totalElements }} rooms</span>
            <button [disabled]="p.last" (click)="reload(p.number + 1)">NEXT ›</button>
          </div>
        }
      }
    }
  `,
  styles: [`
    :host { display: flex; flex-direction: column; gap: 14px; }
    .filters {
      display: flex; gap: 8px; flex-wrap: wrap; align-items: center;
      padding: 12px; background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px;
    }
    .filters select, .filters input {
      background: #0a0a0f; border: 1px solid #1a1a2e; color: #e2e8f0;
      padding: 7px 10px; font-family: 'Rajdhani', sans-serif; font-size: 13px; border-radius: 3px;
      color-scheme: dark;
    }
    .reset {
      background: transparent; border: 1px solid #1a1a2e; color: #94a3b8;
      font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px;
      padding: 7px 12px; cursor: pointer; border-radius: 3px;
    }
    .reset:hover { color: #ef4444; border-color: #ef4444; }
    table { width: 100%; border-collapse: collapse; background: rgba(13,13,21,0.6); border: 1px solid #1a1a2e; border-radius: 6px; }
    th, td { padding: 10px 12px; font-size: 13px; border-bottom: 1px solid #1a1a2e; text-align: left; vertical-align: middle; }
    th { color: #64748b; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; cursor: pointer; }
    td.mono { font-family: 'Orbitron', monospace; }
    td.mono a { color: #06b6d4; text-decoration: none; }
    td.num { text-align: right; font-family: 'Orbitron', monospace; }
    td.actions { text-align: right; white-space: nowrap; }
    .status {
      display: inline-block; padding: 2px 10px; border-radius: 3px; font-size: 11px;
      font-family: 'Orbitron', monospace; letter-spacing: 1px;
    }
    .status.waiting     { background: rgba(6,182,212,0.15); color: #06b6d4; }
    .status.in-progress { background: rgba(34,197,94,0.15); color: #22c55e; }
    .status.finished    { background: rgba(100,116,139,0.15); color: #94a3b8; }
    .status.cancelled   { background: rgba(239,68,68,0.15); color: #ef4444; }
    .btn {
      display: inline-block; padding: 5px 10px; margin-left: 4px; font-size: 11px;
      font-family: 'Orbitron', monospace; letter-spacing: 1px; border-radius: 3px;
      cursor: pointer; text-decoration: none;
    }
    .btn.ghost  { background: transparent; border: 1px solid #1a1a2e; color: #94a3b8; }
    .btn.ghost:hover { color: #06b6d4; border-color: #06b6d4; }
    .btn.warn   { background: rgba(245,158,11,0.1); border: 1px solid rgba(245,158,11,0.4); color: #f59e0b; }
    .btn.danger { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.4); color: #ef4444; }
    .pager {
      display: flex; align-items: center; justify-content: space-between;
      padding: 10px 14px; background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px;
      font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1px; color: #94a3b8;
    }
    .pager button {
      background: transparent; border: 1px solid #1a1a2e; color: #94a3b8;
      padding: 6px 12px; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px;
      cursor: pointer; border-radius: 3px;
    }
    .pager button:disabled { opacity: 0.4; cursor: not-allowed; }
    .pager button:not(:disabled):hover { color: #8b5cf6; border-color: #8b5cf6; }
    .empty { padding: 40px; text-align: center; color: #64748b; background: rgba(13,13,21,0.6); border: 1px solid #1a1a2e; border-radius: 6px; }
    .skeleton {
      height: 320px; background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a);
      background-size: 200% 100%; animation: pulse 1.5s ease-in-out infinite; border-radius: 6px;
    }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `]
})
export class BattleRoomsListComponent implements OnInit {
  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly statusOptions = STATUS_OPTIONS;
  readonly loading = signal(false);
  readonly page = signal<SpringPage<BattleRoomAdmin> | null>(null);

  filters: RoomListFilters = {
    status: null,
    challengeId: null,
    hostId: null,
    from: null,
    to: null,
    page: 0,
    size: 20,
    sort: 'createdAt,desc'
  };

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParamMap;
    this.filters = {
      ...this.filters,
      status: (qp.get('status') as BattleRoomStatus) ?? null,
      challengeId: qp.get('challengeId'),
      hostId: qp.get('hostId'),
      from: qp.get('from'),
      to: qp.get('to'),
      page: Number(qp.get('page') ?? 0),
      size: Number(qp.get('size') ?? 20),
      sort: qp.get('sort') ?? 'createdAt,desc'
    };
    this.reload(this.filters.page ?? 0);
  }

  reload(page: number) {
    this.filters.page = page;
    this.persistQuery();
    this.loading.set(true);
    this.api.listRooms(this.filters).subscribe({
      next: (p) => { this.page.set(p); this.loading.set(false); },
      error: (e) => {
        this.loading.set(false);
        this.toast.error('Failed to load rooms: ' + (e?.error?.message ?? e?.message ?? ''));
      }
    });
  }

  resetFilters() {
    this.filters = { status: null, challengeId: null, hostId: null, from: null, to: null, page: 0, size: 20, sort: 'createdAt,desc' };
    this.reload(0);
  }

  sortBy(field: string) {
    const [current, dir] = (this.filters.sort ?? 'createdAt,desc').split(',');
    const nextDir = current === field && dir === 'desc' ? 'asc' : 'desc';
    this.filters.sort = `${field},${nextDir}`;
    this.reload(0);
  }

  shortId(id: string) { return id.slice(0, 8); }

  forceClose(r: BattleRoomAdmin) {
    const ok = confirm(`Force-close room ${this.shortId(r.id)}? ${r.participantCount} participants will be affected.`);
    if (!ok) return;
    this.api.updateRoomStatus(r.id, 'CANCELLED', 'Force-closed by admin').subscribe({
      next: () => { this.toast.success('Room cancelled'); this.reload(this.filters.page ?? 0); },
      error: (e) => this.toast.error('Cancel failed: ' + (e?.error?.message ?? ''))
    });
  }

  confirmDelete(r: BattleRoomAdmin) {
    const first = confirm(`Delete room ${this.shortId(r.id)}? This cannot be undone.`);
    if (!first) return;
    const second = confirm('Are you absolutely sure? Participants and submissions will also be removed.');
    if (!second) return;
    this.api.deleteRoom(r.id).subscribe({
      next: () => { this.toast.success('Room deleted'); this.reload(this.filters.page ?? 0); },
      error: (e) => this.toast.error('Delete failed: ' + (e?.error?.message ?? ''))
    });
  }

  private persistQuery() {
    const queryParams: Record<string, string> = {};
    Object.entries(this.filters).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') queryParams[k] = String(v);
    });
    this.router.navigate([], { relativeTo: this.route, queryParams, replaceUrl: true });
  }
}
