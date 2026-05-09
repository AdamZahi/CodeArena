import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { TopPlayer } from '../models/battle-admin.models';

@Component({
  selector: 'app-top-players-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <header><h3>TOP PLAYERS</h3></header>
      @if (loading) {
        <div class="skeleton"></div>
      } @else if (data.length === 0) {
        <div class="empty">No data yet.</div>
      } @else {
        <table>
          <thead>
            <tr>
              <th>RANK</th><th>USERNAME</th><th>PLAYED</th><th>WON</th>
              <th>WIN RATE</th><th>XP</th>
            </tr>
          </thead>
          <tbody>
            @for (p of data; track p.userId; let i = $index) {
              <tr>
                <td class="rank">#{{ i + 1 }}</td>
                <td class="user">{{ p.username }}</td>
                <td class="num">{{ p.battlesPlayed }}</td>
                <td class="num">{{ p.battlesWon }}</td>
                <td class="num">{{ (p.winRate * 100).toFixed(1) }}%</td>
                <td class="num">{{ p.xpEarned.toLocaleString() }}</td>
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
    th, td { padding: 10px 8px; font-size: 13px; border-bottom: 1px solid #1a1a2e; text-align: left; }
    th { color: #64748b; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; }
    td.rank { color: #8b5cf6; font-family: 'Orbitron', monospace; }
    td.user { color: #e2e8f0; }
    td.num { text-align: right; font-family: 'Orbitron', monospace; color: #06b6d4; }
    .empty { color: #64748b; padding: 20px 0; text-align: center; font-size: 13px; }
    .skeleton {
      height: 240px;
      background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a);
      background-size: 200% 100%;
      animation: pulse 1.5s ease-in-out infinite;
      border-radius: 4px;
    }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `]
})
export class TopPlayersTableComponent {
  @Input() data: TopPlayer[] = [];
  @Input() loading = false;
}
