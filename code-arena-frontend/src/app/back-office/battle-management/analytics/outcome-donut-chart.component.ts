import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnChanges,
  ViewChild
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { OutcomeDistribution } from '../models/battle-admin.models';

Chart.register(...registerables);

@Component({
  selector: 'app-outcome-donut-chart',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="card">
      <header><h3>OUTCOMES</h3></header>
      @if (loading) {
        <div class="skeleton"></div>
      } @else {
        <div class="canvas-wrap">
          <canvas #cv></canvas>
        </div>
        @if (data) {
          <ul class="legend">
            <li><span class="dot win"></span> Wins · {{ pct(data.winRate) }}</li>
            <li><span class="dot draw"></span> Draws · {{ pct(data.drawRate) }}</li>
            <li><span class="dot abandon"></span> Abandoned · {{ pct(data.abandonedRate) }}</li>
          </ul>
        }
      }
    </div>
  `,
  styles: [`
    .card { background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px; padding: 18px 20px; }
    h3 { margin: 0 0 12px; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; color: #94a3b8; }
    .canvas-wrap { height: 220px; position: relative; }
    .legend { list-style: none; padding: 0; margin: 12px 0 0; display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #94a3b8; }
    .dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; vertical-align: middle; }
    .dot.win     { background: #22c55e; }
    .dot.draw    { background: #f59e0b; }
    .dot.abandon { background: #ef4444; }
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
export class OutcomeDonutChartComponent implements AfterViewInit, OnChanges {
  @Input() data: OutcomeDistribution | null = null;
  @Input() loading = false;

  @ViewChild('cv') canvasRef?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;

  ngAfterViewInit(): void { this.render(); }
  ngOnChanges(): void { if (this.canvasRef) this.render(); }

  pct(rate: number) { return `${Math.round((rate ?? 0) * 100)}%`; }

  private render() {
    if (!this.canvasRef || !this.data) return;
    const cfg: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: ['Wins', 'Draws', 'Abandoned'],
        datasets: [{
          data: [this.data.wins, this.data.draws, this.data.abandoned],
          backgroundColor: ['#22c55e', '#f59e0b', '#ef4444'],
          borderColor: '#0a0a0f',
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: { legend: { display: false } }
      }
    };
    if (this.chart) {
      this.chart.data = cfg.data;
      this.chart.update();
      return;
    }
    this.chart = new Chart(this.canvasRef.nativeElement, cfg);
  }
}
