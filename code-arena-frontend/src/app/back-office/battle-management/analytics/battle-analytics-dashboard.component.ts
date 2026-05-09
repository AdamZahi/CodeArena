import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import {
  AvgDuration,
  BattleSummary,
  LanguageDistribution,
  OutcomeDistribution,
  TimelinePoint,
  TopChallenge,
  TopPlayer
} from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

import { KpiCardsComponent } from './kpi-cards.component';
import { LanguageBarChartComponent } from './language-bar-chart.component';
import { OutcomeDonutChartComponent } from './outcome-donut-chart.component';
import { TimelineChartComponent } from './timeline-chart.component';
import { TopChallengesTableComponent } from './top-challenges-table.component';
import { TopPlayersTableComponent } from './top-players-table.component';

type RangePreset = '7d' | '30d' | '90d' | 'custom';

@Component({
  selector: 'app-battle-analytics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    KpiCardsComponent,
    TimelineChartComponent,
    TopChallengesTableComponent,
    TopPlayersTableComponent,
    OutcomeDonutChartComponent,
    LanguageBarChartComponent
  ],
  template: `
    <div class="filters">
      <div class="presets">
        @for (p of presets; track p) {
          <button type="button" [class.active]="preset === p" (click)="selectPreset(p)">{{ p.toUpperCase() }}</button>
        }
      </div>
      @if (preset === 'custom') {
        <div class="custom">
          <label>FROM <input type="date" [(ngModel)]="fromDate" (change)="reload(false)" /></label>
          <label>TO <input type="date" [(ngModel)]="toDate" (change)="reload(false)" /></label>
        </div>
      }
      <button type="button" class="refresh" (click)="reload(true)">↻ REFRESH</button>
    </div>

    <app-kpi-cards [summary]="summary()" [outcome]="outcome()" [avgDuration]="avgDuration()" [loading]="loading()" />

    <div class="grid two">
      <app-timeline-chart [data]="timeline()" [loading]="loading()" />
      <app-outcome-donut-chart [data]="outcome()" [loading]="loading()" />
    </div>

    <div class="grid two">
      <app-top-challenges-table [data]="topChallenges()" [loading]="loading()" />
      <app-top-players-table [data]="topPlayers()" [loading]="loading()" />
    </div>

    <app-language-bar-chart [data]="languageDist()" [loading]="loading()" />
  `,
  styles: [`
    :host { display: flex; flex-direction: column; gap: 18px; }
    .filters { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 4px; }
    .presets button {
      background: rgba(13,13,21,0.7);
      border: 1px solid #1a1a2e;
      color: #94a3b8;
      font-family: 'Orbitron', monospace;
      font-size: 11px;
      letter-spacing: 1.5px;
      padding: 8px 14px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .presets button:first-child { border-radius: 4px 0 0 4px; }
    .presets button:last-child  { border-radius: 0 4px 4px 0; }
    .presets button.active { color: #8b5cf6; border-color: #8b5cf6; background: rgba(139,92,246,0.1); }
    .presets button:hover  { color: #8b5cf6; }
    .custom { display: flex; gap: 12px; }
    .custom label {
      font-family: 'Orbitron', monospace;
      font-size: 10px;
      letter-spacing: 1.5px;
      color: #64748b;
      display: flex; align-items: center; gap: 6px;
    }
    .custom input {
      background: rgba(13,13,21,0.7);
      border: 1px solid #1a1a2e;
      color: #e2e8f0;
      padding: 6px 8px;
      font-family: 'Rajdhani', sans-serif;
      font-size: 13px;
      border-radius: 3px;
      color-scheme: dark;
    }
    .refresh {
      margin-left: auto;
      background: rgba(6,182,212,0.1);
      border: 1px solid rgba(6,182,212,0.3);
      color: #06b6d4;
      font-family: 'Orbitron', monospace;
      font-size: 11px;
      letter-spacing: 1.5px;
      padding: 8px 14px;
      cursor: pointer;
      border-radius: 4px;
    }
    .refresh:hover { background: rgba(6,182,212,0.2); }
    .grid.two { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }
    @media (max-width: 1100px) { .grid.two { grid-template-columns: 1fr; } }
  `]
})
export class BattleAnalyticsDashboardComponent implements OnInit {
  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);

  readonly presets: RangePreset[] = ['7d', '30d', '90d', 'custom'];
  preset: RangePreset = '30d';
  fromDate = '';
  toDate = '';

  readonly loading = signal(false);
  readonly summary = signal<BattleSummary | null>(null);
  readonly timeline = signal<TimelinePoint[]>([]);
  readonly topChallenges = signal<TopChallenge[]>([]);
  readonly topPlayers = signal<TopPlayer[]>([]);
  readonly outcome = signal<OutcomeDistribution | null>(null);
  readonly languageDist = signal<LanguageDistribution[]>([]);
  readonly avgDuration = signal<AvgDuration | null>(null);

  ngOnInit(): void {
    this.reload(false);
  }

  selectPreset(p: RangePreset) {
    this.preset = p;
    if (p !== 'custom') this.reload(false);
  }

  reload(refresh = false) {
    const { from, to } = this.range();
    this.loading.set(true);
    forkJoin({
      summary: this.api.getSummary(refresh),
      timeline: this.api.getTimeline(from, to, refresh),
      topChallenges: this.api.getTopChallenges(10, refresh),
      topPlayers: this.api.getTopPlayers(10, refresh),
      outcome: this.api.getOutcomeDistribution(refresh),
      langs: this.api.getLanguageDistribution(from, to, refresh),
      avg: this.api.getAvgDuration(refresh)
    }).subscribe({
      next: (r) => {
        this.summary.set(r.summary);
        this.timeline.set(r.timeline);
        this.topChallenges.set(r.topChallenges);
        this.topPlayers.set(r.topPlayers);
        this.outcome.set(r.outcome);
        this.languageDist.set(r.langs);
        this.avgDuration.set(r.avg);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.error('Failed to load analytics: ' + (e?.error?.message ?? e?.message ?? 'unknown error'));
      }
    });
  }

  private range(): { from: string | undefined; to: string | undefined } {
    const now = new Date();
    if (this.preset === 'custom') {
      return {
        from: this.fromDate ? `${this.fromDate}T00:00:00` : undefined,
        to: this.toDate ? `${this.toDate}T23:59:59` : undefined
      };
    }
    const days = this.preset === '7d' ? 7 : this.preset === '90d' ? 90 : 30;
    const from = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
    return { from: from.toISOString().slice(0, 19), to: now.toISOString().slice(0, 19) };
  }
}
