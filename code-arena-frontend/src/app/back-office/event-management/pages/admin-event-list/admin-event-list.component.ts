import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { Chart, registerables } from 'chart.js';
import { ProgrammingEvent } from '../../../../front-office/event/models/event.model';
import { EventService } from '../../../../front-office/event/services/event.service';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-event-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-event-list.component.html',
  styleUrl: './admin-event-list.component.css'
})
export class AdminEventListComponent implements OnInit, OnDestroy {
  isLoading = true;
  events: ProgrammingEvent[] = [];
  errorMessage: string | null = null;

  showDashboard = false;
  toggleDashboard(): void {
    this.showDashboard = !this.showDashboard;
    if (this.showDashboard) {
      setTimeout(() => {
        this.renderDonutChart();
        this.renderBarChart(this.events);
      }, 100);
    }
  }

  totalEvents = 0;
  openEvents = 0;
  exclusiveEvents = 0;
  upcomingEvents = 0;
  activeEvents = 0;
  completedEvents = 0;
  private donutChart: any = null;
  private barChart: any = null;

  private subs = new Subscription();

  constructor(
    private readonly eventService: EventService,
    public readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  ngOnDestroy(): void {
    this.donutChart?.destroy();
    this.barChart?.destroy();
    this.subs.unsubscribe();
  }

  goCreate(): void {
    this.router.navigate(['create'], { relativeTo: this.route });
  }

  viewEvent(id: string): void {
    this.router.navigate([id], { relativeTo: this.route });
  }

  deleteEvent(id: string): void {
    const sub = this.eventService.deleteEvent(id).subscribe({
      next: () => this.loadEvents(),
      error: () => (this.errorMessage = 'Failed to delete event.')
    });
    this.subs.add(sub);
  }

  private loadEvents(): void {
    this.isLoading = true;
    this.errorMessage = null;
    const sub = this.eventService.getEvents().subscribe({
      next: (events: any[]) => {
        this.events = events;
        this.isLoading = false;
        this.computeStats(events);
      },
      error: () => {
        this.errorMessage = 'Failed to load events.';
        this.isLoading = false;
      }
    });
    this.subs.add(sub);
  }

  computeStats(events: any[]): void {
    this.totalEvents = events.length;
    this.openEvents = events.filter(e =>
      e.type === 'OPEN' || e.eventType === 'OPEN').length;
    this.exclusiveEvents = events.filter(e =>
      e.type === 'EXCLUSIVE' || e.eventType === 'EXCLUSIVE').length;
    this.upcomingEvents = events.filter(e => e.status === 'UPCOMING').length;
    this.activeEvents = events.filter(e => e.status === 'ACTIVE').length;
    this.completedEvents = events.filter(e => e.status === 'COMPLETED').length;
    setTimeout(() => {
      this.renderDonutChart();
      this.renderBarChart(events);
    }, 100);
  }

  renderDonutChart(): void {
    const canvas = document.getElementById('donutChart') as HTMLCanvasElement;
    if (!canvas) return;
    if (this.donutChart) this.donutChart.destroy();
    this.donutChart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['OPEN', 'EXCLUSIVE'],
        datasets: [{
          data: [this.openEvents, this.exclusiveEvents],
          backgroundColor: ['#10b981', '#8b5cf6'],
          borderColor: ['#0d0d15'],
          borderWidth: 3
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            labels: { color: '#e2e8f0', font: { family: 'Orbitron' } }
          }
        }
      }
    });
  }

  renderBarChart(events: any[]): void {
    const canvas = document.getElementById('barChart') as HTMLCanvasElement;
    if (!canvas) return;
    if (this.barChart) this.barChart.destroy();
    const top5 = events.slice(0, 5);
    this.barChart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: top5.map(e => e.title?.substring(0, 15) || 'Event'),
        datasets: [{
          label: 'PARTICIPANTS',
          data: top5.map(e => e.currentParticipants || 0),
          backgroundColor: '#06b6d4',
          borderColor: '#0891b2',
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            labels: { color: '#e2e8f0', font: { family: 'Orbitron' } }
          }
        },
        scales: {
          x: { ticks: { color: '#64748b' }, grid: { color: '#1a1a2e' } },
          y: { ticks: { color: '#64748b' }, grid: { color: '#1a1a2e' } }
        }
      }
    });
  }
}
