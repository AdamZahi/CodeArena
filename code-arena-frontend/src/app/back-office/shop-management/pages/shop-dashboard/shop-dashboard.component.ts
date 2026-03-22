import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule }   from '@angular/common';
import { Router }         from '@angular/router';
import { HttpClient }     from '@angular/common/http';
import { environment }    from '../../../../../environments/environment';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-shop-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shop-dashboard.component.html',
  styleUrl: './shop-dashboard.component.css'
})
export class ShopDashboardComponent implements OnInit, AfterViewInit {

  @ViewChild('categoryChart') categoryChartRef!: ElementRef;
  @ViewChild('orderChart')    orderChartRef!: ElementRef;

  stats: any = null;
  isLoading = true;
  bestSellers: any[] = [];

  private categoryChart: Chart | null = null;
  private orderChart: Chart | null = null;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  ngAfterViewInit(): void {}

  loadStats(): void {
    this.isLoading = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/shop/stats`).subscribe({
      next: (res) => {
        this.stats = res.data;
        this.bestSellers = this.parseBestSellers(res.data.bestSellers || []);
        this.isLoading = false;
        // build charts after view is ready
        setTimeout(() => this.buildCharts(), 100);
      },
      error: () => { this.isLoading = false; }
    });
  }

  parseBestSellers(raw: any[]): any[] {
    return raw.map((row: any) => ({
      id:        row[0],
      name:      row[1],
      totalSold: row[2]
    }));
  }

  buildCharts(): void {
    this.buildCategoryChart();
    this.buildOrderChart();
  }

  buildCategoryChart(): void {
    if (!this.categoryChartRef) return;
    if (this.categoryChart) this.categoryChart.destroy();

    const categories = this.stats?.byCategory || {};
    const labels = Object.keys(categories).filter(k => categories[k] > 0);
    const data   = labels.map(k => categories[k]);

    this.categoryChart = new Chart(this.categoryChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: [
            '#8b5cf6', '#06b6d4', '#10b981',
            '#f59e0b', '#ef4444', '#ec4899',
            '#6366f1', '#14b8a6', '#f97316'
          ],
          borderWidth: 2,
          borderColor: '#0a0a0f'
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'right',
            labels: {
              color: '#94a3b8',
              font: { size: 11 },
              padding: 12
            }
          }
        }
      }
    });
  }

  buildOrderChart(): void {
    if (!this.orderChartRef) return;
    if (this.orderChart) this.orderChart.destroy();

    const s = this.stats;
    const labels = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
    const data = [
      s?.pendingOrders   || 0,
      s?.confirmedOrders || 0,
      s?.shippedOrders   || 0,
      s?.deliveredOrders || 0,
      s?.cancelledOrders || 0
    ];

    this.orderChart = new Chart(this.orderChartRef.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Orders',
          data,
          backgroundColor: [
            'rgba(245,158,11,0.7)',
            'rgba(139,92,246,0.7)',
            'rgba(6,182,212,0.7)',
            'rgba(16,185,129,0.7)',
            'rgba(239,68,68,0.7)'
          ],
          borderColor: [
            '#f59e0b', '#8b5cf6', '#06b6d4', '#10b981', '#ef4444'
          ],
          borderWidth: 1,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        scales: {
          x: {
            ticks: { color: '#94a3b8', font: { size: 10 } },
            grid: { color: 'rgba(255,255,255,0.05)' }
          },
          y: {
            ticks: { color: '#94a3b8', font: { size: 10 }, stepSize: 1 },
            grid: { color: 'rgba(255,255,255,0.05)' },
            beginAtZero: true
          }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  }

  goToProducts(): void { this.router.navigate(['/admin/shop']); }
  goToOrders(): void   { this.router.navigate(['/admin/shop/orders']); }
}