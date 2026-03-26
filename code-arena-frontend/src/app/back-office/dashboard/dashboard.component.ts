import { Component } from '@angular/core';
import { UserListComponent } from '../user-management/pages/user-list/user-list.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [UserListComponent],
  template: `
    <section class="dash-header">
      <h1 class="page-title">ADMIN DASHBOARD</h1>
      <p class="page-sub">GLOBAL OVERVIEW & USER MANAGEMENT</p>
    </section>
    <app-user-list />
  `,
  styles: [`
    .dash-header {
      margin-bottom: 18px;
      padding-bottom: 14px;
      border-bottom: 1px solid #1a1a2e;
    }
    .page-title {
      margin: 0;
      font-family: 'Orbitron', monospace;
      font-size: 20px;
      letter-spacing: 2px;
      background: linear-gradient(135deg, #8b5cf6, #06b6d4);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .page-sub {
      margin: 6px 0 0;
      color: #64748b;
      font-family: 'Orbitron', monospace;
      font-size: 10px;
      letter-spacing: 1.5px;
    }
  `]
})
export class DashboardComponent {}
