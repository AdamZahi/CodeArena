import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
}

interface PageResponse<T> {
  content: T[];
}

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="panel">
      <div class="panel-head">
        <h2 class="panel-title">ALL USERS</h2>
      </div>
      <p class="muted" *ngIf="loading">Loading users...</p>
      <p class="muted error" *ngIf="error">{{ error }}</p>

      <table class="users-table" *ngIf="!loading && users.length">
        <thead>
        <tr>
          <th>Email</th>
          <th>Name</th>
          <th>Role</th>
          <th>Status</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let user of users">
          <td>{{ user.email }}</td>
          <td>{{ user.firstName }} {{ user.lastName }}</td>
          <td>{{ user.role }}</td>
          <td>{{ user.active ? 'ACTIVE' : 'INACTIVE' }}</td>
        </tr>
        </tbody>
      </table>

      <p class="muted" *ngIf="!loading && !users.length && !error">No users found.</p>
    </div>
  `,
  styles: [`
    .panel {
      background: #0d0d15;
      border: 1px solid #1a1a2e;
      border-radius: 12px;
      padding: 20px;
    }
    .panel-head {
      margin-bottom: 12px;
      padding-bottom: 10px;
      border-bottom: 1px solid #1a1a2e;
    }
    .panel-title {
      margin: 0;
      font-family: 'Orbitron', monospace;
      font-size: 11px;
      letter-spacing: 2px;
      color: #8b5cf6;
    }
    .muted { color: #64748b; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1px; }
    .error { color: #ef4444; }
    .users-table { width: 100%; border-collapse: collapse; font-size: 13px; }
    .users-table th {
      text-align: left;
      padding: 10px 12px;
      border-bottom: 1px solid #1a1a2e;
      color: #64748b;
      font-family: 'Orbitron', monospace;
      font-size: 9px;
      letter-spacing: 1.4px;
    }
    .users-table td {
      padding: 12px;
      border-bottom: 1px solid #1a1a2e;
      color: #e2e8f0;
    }
    .users-table tr:hover td { background: #12121e; }
  `]
})
export class UserListComponent implements OnInit {
  private readonly http = inject(HttpClient);

  users: AdminUser[] = [];
  loading = false;
  error = '';

  ngOnInit(): void {
    this.loading = true;
    this.http.get<PageResponse<AdminUser>>(`${environment.apiBaseUrl}/api/users`)
      .subscribe({
        next: (res) => {
          this.users = res?.content ?? [];
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load users.';
          this.loading = false;
        }
      });
  }
}
