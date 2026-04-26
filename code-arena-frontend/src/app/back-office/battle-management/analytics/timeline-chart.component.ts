import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';
import { TimelinePoint } from '../models/battle-admin.models';

Chart.register(...registerables);

@Component({
  selector: 'app-timeline-chart',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="card">
      <header>
        <h3>BATTLES OVER TIME</h3>
      </header>
      @if (loading) {
        <div class="skeleton"></div>
      } @else {
        <div class="canvas-wrap">
          <canvas #cv></canvas>
        </div>
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
      margin: 0 0 14px;
      font-family: 'Orbitron', monospace;
      font-size: 12px;
      letter-spacing: 2px;
      color: #94a3b8;
    }
    .canvas-wrap { height: 260px; position: relative; }
    .skeleton {
      height: 260px;
      background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a);
      background-size: 200% 100%;
      animation: pulse 1.5s ease-in-out infinite;
      border-radius: 4px;
    }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `]
})
export class TimelineChartComponent implements AfterViewInit, OnChanges {
  @Input() data: TimelinePoint[] = [];
  @Input() loading = false;

  @ViewChild('cv') canvasRef?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;

  ngAfterViewInit(): void {
    this.render();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] && this.canvasRef) this.render();
  }

  private render() {
    if (!this.canvasRef) return;
    const cfg: ChartConfiguration<'line'> = {
      type: 'line',
      data: this.toChartData(),
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 350 },
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: '#64748b', maxRotation: 0, autoSkip: true }, grid: { color: '#1a1a2e' } },
          y: { ticks: { color: '#64748b' }, grid: { color: '#1a1a2e' }, beginAtZero: true }
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

  private toChartData(): ChartData<'line'> {
    return {
      labels: this.data.map((p) => p.period),
      datasets: [
        {
          data: this.data.map((p) => p.count),
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139,92,246,0.15)',
          tension: 0.25,
          fill: true,
          pointRadius: 2,
          pointBackgroundColor: '#06b6d4'
        }
      ]
    };
  }
}
