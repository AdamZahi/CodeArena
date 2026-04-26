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
import { LanguageDistribution } from '../models/battle-admin.models';

Chart.register(...registerables);

@Component({
  selector: 'app-language-bar-chart',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="card">
      <header><h3>LANGUAGES IN USE</h3></header>
      @if (loading) {
        <div class="skeleton"></div>
      } @else if (data.length === 0) {
        <div class="empty">No submissions in range.</div>
      } @else {
        <div class="canvas-wrap">
          <canvas #cv></canvas>
        </div>
      }
    </div>
  `,
  styles: [`
    .card { background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px; padding: 18px 20px; }
    h3 { margin: 0 0 12px; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; color: #94a3b8; }
    .canvas-wrap { height: 240px; position: relative; }
    .empty { padding: 20px 0; text-align: center; color: #64748b; font-size: 13px; }
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
export class LanguageBarChartComponent implements AfterViewInit, OnChanges {
  @Input() data: LanguageDistribution[] = [];
  @Input() loading = false;

  @ViewChild('cv') canvasRef?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;

  ngAfterViewInit(): void { this.render(); }
  ngOnChanges(): void { if (this.canvasRef) this.render(); }

  private render() {
    if (!this.canvasRef || this.data.length === 0) return;
    const cfg: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: this.data.map((d) => d.language),
        datasets: [{
          data: this.data.map((d) => d.count),
          backgroundColor: '#06b6d4',
          borderRadius: 3
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: '#64748b' }, grid: { color: '#1a1a2e' }, beginAtZero: true },
          y: { ticks: { color: '#94a3b8' }, grid: { color: 'transparent' } }
        }
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
