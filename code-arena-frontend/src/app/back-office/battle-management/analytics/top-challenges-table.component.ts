import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { TopChallenge } from '../models/battle-admin.models';

@Component({
  selector: 'app-top-challenges-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <header><h3>TOP CHALLENGES</h3></header>
      @if (loading) {
        <div class="skeleton"></div>
      } @else if (data.length === 0) {
        <div class="empty">No data yet.</div>
      } @else {
        <table>
          <thead>
            <tr>
              <th>#</th><th>TITLE</th><th>DIFFICULTY</th><th>USAGE</th>
            </tr>
          </thead>
          <tbody>
            @for (c of data; track c.challengeId; let i = $index) {
              <tr>
                <td class="rank">{{ i + 1 }}</td>
                <td>{{ c.title }}</td>
                <td>
                  <span class="badge" [class.easy]="lc(c.difficulty) === 'easy'"
                                       [class.medium]="lc(c.difficulty) === 'medium'"
                                       [class.hard]="lc(c.difficulty) === 'hard'">
                    {{ c.difficulty || '—' }}
                  </span>
                </td>
                <td class="num">{{ c.timesUsed.toLocaleString() }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
  styles: [`
    .card {
      background: rgba(13,13,21,0.7);
      border: 1px solid #1a1a2e;
      border-radius: 6px;
      padding: 18px 20px;
    }
    h3 {
      margin: 0 0 12px;
      font-family: 'Orbitron', monospace;
      font-size: 12px; letter-spacing: 2px; color: #94a3b8;
    }
    table { width: 100%; border-collapse: collapse; font-family: 'Rajdhani', sans-serif; }
    th, td { text-align: left; padding: 10px 8px; font-size: 13px; border-bottom: 1px solid #1a1a2e; }
    th { color: #64748b; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; }
    td.rank { color: #8b5cf6; font-family: 'Orbitron', monospace; font-size: 12px; }
    td.num { text-align: right; color: #06b6d4; font-family: 'Orbitron', monospace; }
    .badge {
      display: inline-block; padding: 2px 8px; border-radius: 3px; font-size: 11px;
      letter-spacing: 1px; background: #1a1a2e; color: #94a3b8;
    }
    .badge.easy   { color: #22c55e; background: rgba(34,197,94,0.1); }
    .badge.medium { color: #f59e0b; background: rgba(245,158,11,0.1); }
    .badge.hard   { color: #ef4444; background: rgba(239,68,68,0.1); }
    .empty { color: #64748b; font-size: 13px; padding: 20px 0; text-align: center; }
    .skeleton {
      height: 220px;
      background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a);
      background-size: 200% 100%;
      animation: pulse 1.5s ease-in-out infinite;
      border-radius: 4px;
    }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `]
})
export class TopChallengesTableComponent {
  @Input() data: TopChallenge[] = [];
  @Input() loading = false;
  lc(s: string | null) { return (s ?? '').toLowerCase(); }
}
