import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { AvgDuration, BattleSummary, OutcomeDistribution } from '../models/battle-admin.models';

interface Card {
  label: string;
  value: string;
  hint?: string;
  trend: number | null;
}

@Component({
  selector: 'app-kpi-cards',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (loading) {
      <div class="grid">
        @for (s of skeletonRange; track s) {
          <div class="card skeleton"></div>
        }
      </div>
    } @else {
      <div class="grid">
        @for (c of cards; track c.label) {
          <div class="card">
            <div class="label">{{ c.label }}</div>
            <div class="value">{{ c.value }}</div>
            @if (c.hint) { <div class="hint">{{ c.hint }}</div> }
            @if (c.trend !== null && c.trend !== undefined) {
              <div class="trend" [class.up]="c.trend > 0" [class.down]="c.trend < 0">
                {{ c.trend > 0 ? '▲' : (c.trend < 0 ? '▼' : '◆') }}
                {{ trendLabel(c.trend) }}
              </div>
            }
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
    }
    .card {
      padding: 18px 20px;
      background: rgba(13,13,21,0.7);
      border: 1px solid #1a1a2e;
      border-radius: 6px;
      position: relative;
      overflow: hidden;
    }
    .card::after {
      content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px;
      background: linear-gradient(90deg, transparent, #8b5cf6, transparent);
    }
    .label {
      font-family: 'Orbitron', monospace;
      font-size: 10px;
      letter-spacing: 2px;
      color: #64748b;
    }
    .value {
      margin-top: 8px;
      font-family: 'Orbitron', monospace;
      font-size: 26px;
      color: #e2e8f0;
    }
    .hint { color: #94a3b8; font-size: 12px; margin-top: 4px; }
    .trend {
      margin-top: 8px;
      font-size: 11px;
      letter-spacing: 1px;
      color: #64748b;
    }
    .trend.up { color: #22c55e; }
    .trend.down { color: #ef4444; }
    .skeleton {
      min-height: 110px;
      background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a);
      background-size: 200% 100%;
      animation: pulse 1.5s ease-in-out infinite;
    }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `]
})
export class KpiCardsComponent {
  @Input() summary: BattleSummary | null = null;
  @Input() outcome: OutcomeDistribution | null = null;
  @Input() avgDuration: AvgDuration | null = null;
  @Input() loading = false;

  readonly skeletonRange = [0, 1, 2, 3, 4];

  get cards(): Card[] {
    if (!this.summary) return [];
    const winRatePct = Math.round((this.summary.globalWinRate ?? 0) * 100);
    const abandonedRatePct = this.outcome ? Math.round(this.outcome.abandonedRate * 100) : null;
    const avg = this.avgDuration?.avgDurationMinutes ?? this.summary.avgDurationMinutes ?? 0;
    return [
      { label: 'TOTAL BATTLES', value: this.summary.totalBattles.toLocaleString(), trend: null },
      { label: 'ACTIVE NOW', value: this.summary.activeBattles.toLocaleString(), hint: 'in progress', trend: null },
      { label: 'COMPLETED', value: this.summary.completedBattles.toLocaleString(), trend: null },
      { label: 'WIN RATE', value: `${winRatePct}%`, trend: winRatePct - 50 },
      { label: 'AVG DURATION', value: `${avg.toFixed(1)} min`, hint: this.avgDuration ? `${this.avgDuration.sampleSize} battles sampled` : undefined, trend: null },
      { label: 'ABANDONED RATE', value: abandonedRatePct === null ? '–' : `${abandonedRatePct}%`, trend: abandonedRatePct === null ? null : -abandonedRatePct }
    ];
  }

  trendLabel(t: number): string {
    return `${Math.abs(Math.round(t))} pts vs baseline`;
  }
}
