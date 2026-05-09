import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { BattleRoomDetail } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ForceEndModalComponent } from '../ops/force-end-modal.component';
import { ReassignWinnerModalComponent } from '../ops/reassign-winner-modal.component';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-battle-room-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ForceEndModalComponent,
    ReassignWinnerModalComponent
  ],
  template: `
    <div class="topbar">
      <a routerLink=".." class="back">‹ ROOMS</a>
      @if (room(); as r) {
        <span class="status-pill" [class.waiting]="r.status === 'WAITING'"
                                  [class.in-progress]="r.status === 'IN_PROGRESS'"
                                  [class.finished]="r.status === 'FINISHED'"
                                  [class.cancelled]="r.status === 'CANCELLED'">
          {{ r.status }}
        </span>
        <div class="actions">
          @if (r.status !== 'FINISHED' && r.status !== 'CANCELLED') {
            <button class="btn warn" (click)="showForceEnd = true">FORCE END</button>
          }
          @if (r.status === 'FINISHED') {
            <button class="btn ghost" (click)="showReassign = true">REASSIGN WINNER</button>
          }
          <button class="btn ghost" (click)="reset()">RESET</button>
          <button class="btn danger" (click)="cancel()">CANCEL ROOM</button>
        </div>
      }
    </div>

    @if (loading()) {
      <div class="skeleton"></div>
    } @else {
      @if (room(); as r) {
      <div class="meta-grid">
        <div><dt>ROOM ID</dt><dd class="mono">{{ r.id }}</dd></div>
        <div><dt>HOST</dt><dd>{{ r.hostUsername ?? r.hostId ?? '—' }}</dd></div>
        <div><dt>MODE</dt><dd>{{ r.mode }}</dd></div>
        <div><dt>MAX PLAYERS</dt><dd>{{ r.maxPlayers }}</dd></div>
        <div><dt>CHALLENGE COUNT</dt><dd>{{ r.challengeCount }}</dd></div>
        <div><dt>PUBLIC</dt><dd>{{ r.isPublic ? 'YES' : 'NO' }}</dd></div>
        <div><dt>INVITE TOKEN</dt><dd class="mono">{{ r.inviteToken ?? '—' }}</dd></div>
        <div><dt>WINNER</dt><dd>{{ winnerLabel(r) }}</dd></div>
        <div><dt>STARTS AT</dt><dd>{{ r.startsAt ? (r.startsAt | date:'medium') : '—' }}</dd></div>
        <div><dt>ENDS AT</dt><dd>{{ r.endsAt ? (r.endsAt | date:'medium') : '—' }}</dd></div>
        <div><dt>CREATED</dt><dd>{{ r.createdAt | date:'medium' }}</dd></div>
      </div>

      <section>
        <h3>CHALLENGES</h3>
        @if (r.challengeIds.length === 0) {
          <p class="empty">No challenges linked.</p>
        } @else {
          <ul class="chip-list">
            @for (cid of r.challengeIds; track cid) { <li>#{{ cid }}</li> }
          </ul>
        }
      </section>

      <section>
        <h3>PARTICIPANTS · {{ r.participants.length }}</h3>
        <div class="notify-row">
          <input type="text" placeholder="Notification title" [(ngModel)]="notifyTitle" />
          <input type="text" placeholder="Message body" [(ngModel)]="notifyMessage" />
          <button class="btn ghost" (click)="sendNotification()" [disabled]="sending()">SEND</button>
        </div>
        @if (r.participants.length === 0) {
          <p class="empty">No participants yet.</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>USERNAME</th><th>ROLE</th><th>READY</th>
                <th>SCORE</th><th>RANK</th><th>ELO Δ</th><th>JOINED</th>
              </tr>
            </thead>
            <tbody>
              @for (p of r.participants; track p.id) {
                <tr [class.winner]="p.rank === 1">
                  <td>{{ p.username ?? p.userId ?? '—' }}</td>
                  <td>{{ p.role }}</td>
                  <td>{{ p.ready ? '●' : '○' }}</td>
                  <td class="num">{{ p.score ?? '—' }}</td>
                  <td class="num">{{ p.rank ?? '—' }}</td>
                  <td class="num">{{ p.eloChange ?? '—' }}</td>
                  <td>{{ p.joinedAt | date:'short' }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>
      }
    }

    @if (showForceEnd && room()) {
      <app-force-end-modal [room]="room()!"
                           (closed)="showForceEnd = false"
                           (succeeded)="onMutated()" />
    }

    @if (showReassign && room()) {
      <app-reassign-winner-modal [room]="room()!"
                                 (closed)="showReassign = false"
                                 (succeeded)="onMutated()" />
    }
  `,
  styles: [`
    :host { display: flex; flex-direction: column; gap: 18px; }
    .topbar { display: flex; align-items: center; gap: 14px; }
    .back {
      font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.5px;
      color: #94a3b8; text-decoration: none;
    }
    .back:hover { color: #06b6d4; }
    .status-pill {
      padding: 4px 12px; border-radius: 3px; font-size: 11px;
      font-family: 'Orbitron', monospace; letter-spacing: 1px;
    }
    .status-pill.waiting     { background: rgba(6,182,212,0.15); color: #06b6d4; }
    .status-pill.in-progress { background: rgba(34,197,94,0.15); color: #22c55e; }
    .status-pill.finished    { background: rgba(100,116,139,0.15); color: #94a3b8; }
    .status-pill.cancelled   { background: rgba(239,68,68,0.15); color: #ef4444; }
    .actions { margin-left: auto; display: flex; gap: 8px; }
    .btn { padding: 7px 14px; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.2px; cursor: pointer; border-radius: 3px; border: 1px solid; }
    .btn.ghost  { background: transparent; border-color: #1a1a2e; color: #94a3b8; }
    .btn.ghost:hover { color: #06b6d4; border-color: #06b6d4; }
    .btn.warn   { background: rgba(245,158,11,0.1); border-color: rgba(245,158,11,0.4); color: #f59e0b; }
    .btn.danger { background: rgba(239,68,68,0.1); border-color: rgba(239,68,68,0.4); color: #ef4444; }

    .meta-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px;
      background: rgba(13,13,21,0.7); padding: 18px; border: 1px solid #1a1a2e; border-radius: 6px;
    }
    .meta-grid dt { font-family: 'Orbitron', monospace; font-size: 9px; letter-spacing: 1.5px; color: #64748b; }
    .meta-grid dd { margin: 4px 0 0; color: #e2e8f0; font-size: 14px; }
    .mono { font-family: 'Orbitron', monospace; word-break: break-all; }
    section h3 {
      margin: 0 0 10px; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; color: #94a3b8;
    }
    .chip-list { list-style: none; padding: 0; display: flex; gap: 8px; flex-wrap: wrap; }
    .chip-list li {
      padding: 4px 10px; background: rgba(139,92,246,0.1); color: #8b5cf6;
      border: 1px solid rgba(139,92,246,0.3); border-radius: 3px;
      font-family: 'Orbitron', monospace; font-size: 11px;
    }
    table { width: 100%; border-collapse: collapse; background: rgba(13,13,21,0.6); border: 1px solid #1a1a2e; border-radius: 6px; }
    th, td { padding: 10px 12px; font-size: 13px; border-bottom: 1px solid #1a1a2e; text-align: left; }
    th { color: #64748b; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; }
    td.num { text-align: right; font-family: 'Orbitron', monospace; }
    tr.winner { background: rgba(245,158,11,0.06); color: #f59e0b; }
    .empty { color: #64748b; font-size: 13px; padding: 12px 0; }
    .skeleton {
      height: 320px; background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a);
      background-size: 200% 100%; animation: pulse 1.5s ease-in-out infinite; border-radius: 6px;
    }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

    .notify-row { display: flex; gap: 8px; margin-bottom: 12px; }
    .notify-row input {
      flex: 1; background: #0a0a0f; border: 1px solid #1a1a2e; color: #e2e8f0;
      padding: 7px 10px; font-family: 'Rajdhani', sans-serif; font-size: 13px; border-radius: 3px;
    }
  `]
})
export class BattleRoomDetailComponent implements OnInit {
  private readonly api = inject(BattleAdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly room = signal<BattleRoomDetail | null>(null);
  readonly sending = signal(false);

  showForceEnd = false;
  showReassign = false;
  notifyTitle = '';
  notifyMessage = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  load(id: string) {
    this.loading.set(true);
    this.api.getRoom(id).subscribe({
      next: (r) => { this.room.set(r); this.loading.set(false); },
      error: (e) => {
        this.loading.set(false);
        this.toast.error('Failed to load room: ' + (e?.error?.message ?? e?.message ?? ''));
      }
    });
  }

  onMutated() {
    this.showForceEnd = false;
    this.showReassign = false;
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  cancel() {
    const r = this.room();
    if (!r) return;
    if (!confirm(`Cancel room ${r.id.slice(0, 8)}? This cannot be undone.`)) return;
    this.api.updateRoomStatus(r.id, 'CANCELLED', 'Cancelled by admin').subscribe({
      next: () => { this.toast.success('Room cancelled'); this.load(r.id); },
      error: (e) => this.toast.error('Cancel failed: ' + (e?.error?.message ?? ''))
    });
  }

  reset() {
    const r = this.room();
    if (!r) return;
    if (!confirm(`Reset room ${r.id.slice(0, 8)} to WAITING? Scores and XP will be reverted.`)) return;
    this.api.resetRoom(r.id).subscribe({
      next: () => { this.toast.success('Room reset'); this.load(r.id); },
      error: (e) => this.toast.error('Reset failed: ' + (e?.error?.message ?? ''))
    });
  }

  sendNotification() {
    const r = this.room();
    if (!r) return;
    if (!this.notifyTitle.trim() || !this.notifyMessage.trim()) {
      this.toast.error('Both title and message are required');
      return;
    }
    this.sending.set(true);
    this.api.sendNotification(r.id, this.notifyTitle, this.notifyMessage).subscribe({
      next: (resp) => {
        this.sending.set(false);
        this.notifyTitle = '';
        this.notifyMessage = '';
        this.toast.success(`Notification sent to ${resp.recipients} participants`);
      },
      error: (e) => {
        this.sending.set(false);
        this.toast.error('Notify failed: ' + (e?.error?.message ?? ''));
      }
    });
  }

  winnerLabel(r: BattleRoomDetail): string {
    if (!r.winnerId) return '—';
    const winner = r.participants.find((p) => p.userId === r.winnerId);
    return winner?.username ?? r.winnerId;
  }
}
