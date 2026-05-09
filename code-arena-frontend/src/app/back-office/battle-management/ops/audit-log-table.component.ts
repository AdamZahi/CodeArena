import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { AuditLogEntry, SpringPage } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-audit-log-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <header><h3>AUDIT LOG</h3></header>

      @if (loading()) {
        <div class="skeleton"></div>
      } @else {
        @if (page(); as p) {
          @if (p.content.length === 0) {
            <p class="empty">No admin actions recorded yet.</p>
          } @else {
            <table>
              <thead>
                <tr>
                  <th>WHEN</th><th>ADMIN</th><th>ACTION</th><th>ROOM</th><th>DETAILS</th>
                </tr>
              </thead>
              <tbody>
                @for (e of p.content; track e.id) {
                  <tr>
                    <td>{{ e.performedAt | date:'short' }}</td>
                    <td>{{ e.adminUsername ?? e.adminId }}</td>
                    <td><span class="badge" [class]="actionClass(e.action)">{{ e.action }}</span></td>
                    <td class="mono">{{ e.targetRoomId ? e.targetRoomId.slice(0, 8) : '—' }}</td>
                    <td>
                      <details>
                        <summary>view</summary>
                        <pre>{{ pretty(e.details) }}</pre>
                      </details>
                    </td>
                  </tr>
                }
              </tbody>
            </table>

            <div class="pager">
              <button [disabled]="p.first" (click)="reload(p.number - 1)">‹ PREV</button>
              <span>Page {{ p.number + 1 }} / {{ p.totalPages || 1 }} · {{ p.totalElements }} entries</span>
              <button [disabled]="p.last" (click)="reload(p.number + 1)">NEXT ›</button>
            </div>
          }
        }
      }
    </div>
  `,
  styles: [`
    .card { background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px; padding: 18px 20px; display: flex; flex-direction: column; gap: 12px; }
    h3 { margin: 0; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; color: #94a3b8; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 8px; font-size: 13px; border-bottom: 1px solid #1a1a2e; text-align: left; vertical-align: top; }
    th { color: #64748b; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; }
    td.mono { font-family: 'Orbitron', monospace; }
    .badge { padding: 2px 8px; border-radius: 3px; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1px; }
    .badge.force-end     { background: rgba(245,158,11,0.15); color: #f59e0b; }
    .badge.reassign      { background: rgba(6,182,212,0.15); color: #06b6d4; }
    .badge.reset         { background: rgba(139,92,246,0.15); color: #8b5cf6; }
    .badge.bulk-cancel   { background: rgba(239,68,68,0.15); color: #ef4444; }
    .badge.notify        { background: rgba(34,197,94,0.15); color: #22c55e; }
    .badge.default       { background: #1a1a2e; color: #94a3b8; }
    details summary { cursor: pointer; color: #06b6d4; font-size: 12px; }
    pre { white-space: pre-wrap; word-break: break-word; background: #0a0a0f; padding: 8px; border-radius: 3px; font-size: 11px; color: #94a3b8; max-height: 220px; overflow: auto; }
    .empty { color: #64748b; padding: 20px 0; text-align: center; font-size: 13px; }
    .skeleton { height: 280px; background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a); background-size: 200% 100%; animation: pulse 1.5s ease-in-out infinite; border-radius: 4px; }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
    .pager {
      display: flex; align-items: center; justify-content: space-between;
      padding: 8px 0; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1px; color: #94a3b8;
    }
    .pager button {
      background: transparent; border: 1px solid #1a1a2e; color: #94a3b8;
      padding: 6px 12px; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px;
      cursor: pointer; border-radius: 3px;
    }
    .pager button:disabled { opacity: 0.4; cursor: not-allowed; }
  `]
})
export class AuditLogTableComponent implements OnInit {
  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly page = signal<SpringPage<AuditLogEntry> | null>(null);

  ngOnInit(): void {
    this.reload(0);
  }

  reload(page: number) {
    this.loading.set(true);
    this.api.auditLog(page, 25).subscribe({
      next: (p) => { this.page.set(p); this.loading.set(false); },
      error: (e) => {
        this.loading.set(false);
        this.toast.error('Failed to load audit log: ' + (e?.error?.message ?? ''));
      }
    });
  }

  actionClass(action: string): string {
    switch (action) {
      case 'FORCE_END': return 'badge force-end';
      case 'REASSIGN_WINNER': return 'badge reassign';
      case 'RESET': return 'badge reset';
      case 'BULK_CANCEL': return 'badge bulk-cancel';
      case 'NOTIFY': return 'badge notify';
      default: return 'badge default';
    }
  }

  pretty(json: string | null): string {
    if (!json) return '';
    try { return JSON.stringify(JSON.parse(json), null, 2); }
    catch { return json; }
  }
}
