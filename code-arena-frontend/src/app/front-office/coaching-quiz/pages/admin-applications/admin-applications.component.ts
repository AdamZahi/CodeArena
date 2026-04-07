import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CoachingService } from '../../services/coaching.service';
import { CoachApplication } from '../../models/coaching-session.model';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';

@Component({
  selector: 'app-admin-applications',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="dashboard-container">
        
        <div class="lc-header-main">
          <div class="header-text">
            <h1 class="glitch-title">ADMIN_<span>CONTROL_PANEL</span></h1>
            <p class="hero-desc">Manage coach applications. Approve or reject operator access requests.</p>
          </div>
          <div class="tab-row">
            <button class="tab-btn" [class.active]="activeTab === 'pending'" (click)="activeTab = 'pending'; loadData()">
              PENDING <span class="count-badge" *ngIf="pendingCount > 0">{{ pendingCount }}</span>
            </button>
            <button class="tab-btn" [class.active]="activeTab === 'all'" (click)="activeTab = 'all'; loadData()">
              ALL_LOGS
            </button>
          </div>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>FETCHING_APPLICATION_DATA...</p>
        </div>

        <ng-container *ngIf="!loading">
          <!-- PENDING APPLICATIONS -->
          <div class="applications-section" *ngIf="activeTab === 'pending'">
            <div class="lc-empty" *ngIf="pendingApplications.length === 0">
              <p>NO_PENDING_APPLICATIONS. ALL_CLEAR.</p>
            </div>

            <div class="app-card pending-hl" *ngFor="let app of pendingApplications">
              <div class="card-inner">
                <div class="app-header">
                  <div class="applicant-info">
                    <div class="avatar-box">{{ app.applicantName.substring(0,1).toUpperCase() }}</div>
                    <div>
                      <h3 class="applicant-name">{{ app.applicantName }}</h3>
                      <span class="applicant-email">{{ app.applicantEmail }}</span>
                    </div>
                  </div>
                  <div class="status-chip pending">PENDING</div>
                </div>
                
                <div class="submitted-date">
                  <span class="meta-lbl">SUBMITTED:</span>
                  <span class="meta-val">{{ app.createdAt | date:'medium' }}</span>
                </div>

                <div class="cv-section" *ngIf="app.cvFileName">
                  <div class="form-section-title mt-4">
                    <span class="section-icon">📎</span>
                    <h3 style="font-size: 0.9rem;">ATTACHED_DOCUMENT:</h3>
                    <button class="action-neon-btn small ml-3" (click)="openPdf(app)">
                      📥 OPEN {{ app.cvFileName }}
                    </button>
                  </div>
                </div>

                <div class="admin-note-input">
                  <label>ADMIN_NOTE (optional):</label>
                  <input type="text" [(ngModel)]="adminNotes[app.id || '']" placeholder="Add a note for the applicant..." class="cyber-input-small" />
                </div>

                <div class="action-row">
                  <button class="action-neon-btn approve" (click)="approve(app)" [disabled]="processing[app.id || '']">
                    ✅ APPROVE
                  </button>
                  <button class="action-neon-btn reject" (click)="reject(app)" [disabled]="processing[app.id || '']">
                    ❌ REJECT
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- ALL APPLICATIONS LOG -->
          <div class="applications-section" *ngIf="activeTab === 'all'">
            <div class="lc-empty" *ngIf="allApplications.length === 0">
              <p>NO_APPLICATION_LOGS_FOUND.</p>
            </div>

            <div class="app-card" *ngFor="let app of allApplications"
                 [ngClass]="{'approved-hl': app.status === 'APPROVED', 'rejected-hl': app.status === 'REJECTED', 'pending-hl': app.status === 'PENDING'}">
              <div class="card-inner">
                <div class="app-header">
                  <div class="applicant-info">
                    <div class="avatar-box" [ngClass]="app.status?.toLowerCase()">{{ app.applicantName.substring(0,1).toUpperCase() }}</div>
                    <div>
                      <h3 class="applicant-name">{{ app.applicantName }}</h3>
                      <span class="applicant-email">{{ app.applicantEmail }}</span>
                    </div>
                  </div>
                  <div class="status-chip" [ngClass]="app.status?.toLowerCase()">{{ app.status }}</div>
                </div>

                <div class="submitted-date">
                  <span class="meta-lbl">SUBMITTED:</span>
                  <span class="meta-val">{{ app.createdAt | date:'medium' }}</span>
                  <span class="meta-lbl ml-3" *ngIf="app.reviewedAt">REVIEWED:</span>
                  <span class="meta-val" *ngIf="app.reviewedAt">{{ app.reviewedAt | date:'medium' }}</span>
                </div>

                <div class="cv-section" *ngIf="app.cvFileName" style="border:none; padding:0; margin-top:20px;">
                  <div class="form-section-title mt-4">
                    <span class="section-icon">📎</span>
                    <h3 style="font-size: 0.9rem;">ATTACHED_DOCUMENT:</h3>
                    <button class="action-neon-btn small ml-3" (click)="openPdf(app)">
                      📥 OPEN {{ app.cvFileName }}
                    </button>
                  </div>
                </div>

                <div class="admin-note-display" *ngIf="app.adminNote">
                  <span class="meta-lbl">ADMIN_NOTE:</span>
                  <span class="note-text">"{{ app.adminNote }}"</span>
                </div>

                <!-- Re-action for PENDING in ALL tab -->
                <div class="action-row" *ngIf="app.status === 'PENDING'">
                  <div class="admin-note-input inline">
                    <input type="text" [(ngModel)]="adminNotes[app.id || '']" placeholder="Admin note..." class="cyber-input-small" />
                  </div>
                  <button class="action-neon-btn approve small" (click)="approve(app)" [disabled]="processing[app.id || '']">✅</button>
                  <button class="action-neon-btn reject small" (click)="reject(app)" [disabled]="processing[app.id || '']">❌</button>
                </div>
              </div>
            </div>
          </div>
        </ng-container>

      </div>
    </div>
  `,
  styles: [`
    :host {
      --neon: #8b5cf6;
      --neon2: #06b6d4;
      --neon3: #10b981;
      --neon-red: #f43f5e;
      --neon-yellow: #ecc94b;
      --dark: #0a0a0f;
      --card: #0d0d15;
      --border: #1a1a2e;
      --text: #e2e8f0;
      --muted: #64748b;
    }

    @keyframes scan-line { 0% { transform: translateY(-100%); } 100% { transform: translateY(100vh); } }

    .lc-container {
      min-height: 100vh; background: var(--dark); font-family: 'Rajdhani', sans-serif; color: var(--text);
      position: relative; overflow-x: hidden; padding-bottom: 6rem;
    }
    .lc-container::before {
      content: ''; position: fixed; top: 0; left: 0; width: 100%; height: 2px;
      background: linear-gradient(90deg, transparent, var(--neon-red), transparent);
      animation: scan-line 4s linear infinite; opacity: 0.15; pointer-events: none; z-index: 999;
    }

    .dashboard-container { max-width: 1200px; margin: 0 auto; padding: 3rem 2rem; position: relative; z-index: 1; }

    .lc-header-main { margin-bottom: 4rem; padding-bottom: 2rem; border-bottom: 1px solid var(--border); }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 2.5rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 0.5rem; }
    .glitch-title span { color: var(--neon-red); text-shadow: 0 0 15px var(--neon-red); }
    .hero-desc { font-size: 1rem; color: var(--muted); letter-spacing: 0.5px; margin-bottom: 2rem; }

    .tab-row { display: flex; gap: 1rem; }
    .tab-btn { background: var(--card); border: 1px solid var(--border); color: var(--muted); padding: 10px 24px;
      font-family: 'Orbitron', monospace; font-size: 10px; font-weight: 700; letter-spacing: 2px; cursor: pointer; transition: all 0.3s;
      clip-path: polygon(8px 0%, 100% 0%, calc(100% - 8px) 100%, 0% 100%);
    }
    .tab-btn.active { background: var(--neon-red); color: #fff; border-color: var(--neon-red); }
    .tab-btn:hover:not(.active) { border-color: var(--text); color: var(--text); }
    .count-badge { background: #fff; color: #000; padding: 2px 8px; border-radius: 10px; font-size: 9px; margin-left: 6px; }

    /* APPLICATION CARDS */
    .applications-section { display: flex; flex-direction: column; gap: 2.5rem; }

    .app-card {
      background: var(--card); border: 1px solid var(--border); position: relative; overflow: hidden; transition: all 0.3s;
      clip-path: polygon(20px 0%, 100% 0%, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0% 100%, 0% 20px);
    }
    .pending-hl { border-left: 3px solid var(--neon-yellow); }
    .approved-hl { border-left: 3px solid var(--neon3); }
    .rejected-hl { border-left: 3px solid var(--neon-red); }
    .app-card:hover { transform: translateY(-3px); box-shadow: 0 0 20px rgba(139, 92, 246, 0.08); }

    .card-inner { padding: 2.5rem; }

    .app-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
    .applicant-info { display: flex; align-items: center; gap: 1.5rem; }
    .avatar-box { width: 50px; height: 50px; background: rgba(139, 92, 246, 0.1); border: 1px solid var(--neon); color: var(--neon);
      display: flex; align-items: center; justify-content: center; font-family: 'Orbitron', monospace; font-weight: 900; font-size: 20px;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); }
    .avatar-box.approved { border-color: var(--neon3); color: var(--neon3); background: rgba(16, 185, 129, 0.1); }
    .avatar-box.rejected { border-color: var(--neon-red); color: var(--neon-red); background: rgba(244, 63, 94, 0.1); }
    .avatar-box.pending { border-color: var(--neon-yellow); color: var(--neon-yellow); background: rgba(236, 201, 75, 0.1); }

    .applicant-name { font-family: 'Orbitron', sans-serif; color: #fff; font-size: 1.3rem; margin: 0 0 0.3rem; }
    .applicant-email { color: var(--muted); font-size: 0.85rem; font-family: 'Fira Code', monospace; }

    .status-chip { font-family: 'Orbitron', monospace; font-size: 9px; padding: 5px 14px; border: 1px solid; font-weight: 900; letter-spacing: 2px; }
    .status-chip.pending { color: var(--neon-yellow); border-color: var(--neon-yellow); background: rgba(236, 201, 75, 0.05); }
    .status-chip.approved { color: var(--neon3); border-color: var(--neon3); background: rgba(16, 185, 129, 0.05); }
    .status-chip.rejected { color: var(--neon-red); border-color: var(--neon-red); background: rgba(244, 63, 94, 0.05); }

    .submitted-date { display: flex; align-items: center; gap: 0.8rem; margin-bottom: 2rem; flex-wrap: wrap; }
    .meta-lbl { font-size: 9px; color: var(--muted); font-family: 'Orbitron', monospace; letter-spacing: 2px; }
    .meta-val { font-size: 0.9rem; color: #fff; font-family: 'Fira Code', monospace; }
    .ml-3 { margin-left: 1.5rem; }

    .cv-section { margin-bottom: 2rem; }
    .cv-label { font-size: 10px; color: var(--neon2); font-family: 'Orbitron', monospace; letter-spacing: 2px; margin-bottom: 1rem; cursor: pointer; }
    .cv-label .toggle { margin-left: 0.5rem; font-size: 8px; }
    .cv-content { background: rgba(0,0,0,0.4); border: 1px solid var(--border); padding: 2rem; color: var(--text);
      font-family: 'Fira Code', monospace; font-size: 0.85rem; line-height: 1.8; white-space: pre-wrap; max-height: 400px; overflow-y: auto; }

    .admin-note-input { margin-bottom: 2rem; }
    .admin-note-input.inline { flex: 1; margin-bottom: 0; }
    .admin-note-input label { font-size: 9px; color: var(--muted); font-family: 'Orbitron', monospace; letter-spacing: 1px; display: block; margin-bottom: 0.5rem; }
    .cyber-input-small { background: rgba(0,0,0,0.3); border: 1px solid var(--border); color: #fff; padding: 0.8rem 1rem;
      font-family: 'Fira Code', monospace; font-size: 0.85rem; width: 100%; box-sizing: border-box; outline: none; transition: all 0.3s; }
    .cyber-input-small:focus { border-color: var(--neon2); }

    .admin-note-display { margin-bottom: 2rem; }
    .note-text { color: var(--neon2); font-style: italic; font-size: 0.9rem; margin-left: 0.5rem; }

    .action-row { display: flex; gap: 1.5rem; align-items: center; }
    .action-neon-btn {
      padding: 12px 28px; font-family: 'Orbitron', monospace; font-size: 10px; font-weight: 900;
      border: none; cursor: pointer; transition: all 0.3s; letter-spacing: 1px;
      clip-path: polygon(8px 0%, 100% 0%, calc(100% - 8px) 100%, 0% 100%);
    }
    .action-neon-btn.approve { background: var(--neon3); color: #000; }
    .action-neon-btn.approve:hover:not(:disabled) { background: #fff; box-shadow: 0 0 20px var(--neon3); }
    .action-neon-btn.reject { background: var(--neon-red); color: #fff; }
    .action-neon-btn.reject:hover:not(:disabled) { background: #fff; color: #000; box-shadow: 0 0 20px var(--neon-red); }
    .action-neon-btn:disabled { opacity: 0.3; cursor: not-allowed; }
    .action-neon-btn.small { padding: 10px 18px; }

    .lc-loading, .lc-empty { text-align: center; padding: 5rem; font-family: 'Orbitron', monospace; color: var(--muted); letter-spacing: 3px; }
    .spinner { width: 50px; height: 50px; border: 2px solid var(--border); border-top-color: var(--neon-red); border-radius: 50%; margin: 0 auto 2rem; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class AdminApplicationsComponent implements OnInit {
  pendingApplications: CoachApplication[] = [];
  allApplications: CoachApplication[] = [];
  pendingCount = 0;
  loading = true;
  activeTab: 'pending' | 'all' = 'pending';

  adminNotes: { [key: string]: string } = {};
  processing: { [key: string]: boolean } = {};
  expandedCv: { [key: string]: boolean } = {};

  constructor(private coachingService: CoachingService) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading = true;
    if (this.activeTab === 'pending') {
      this.coachingService.getPendingApplications().subscribe({
        next: (data) => {
          this.pendingApplications = data;
          this.pendingCount = data.length;
          this.loading = false;
          // Pre-expand CV for pending
          data.forEach(app => { if (app.id) this.expandedCv[app.id] = true; });
        },
        error: (err) => {
          this.loading = false;
          if (err.status === 403) {
            alert('Access denied. Admin credentials required.');
          }
        }
      });
    } else {
      this.coachingService.getAllApplications().subscribe({
        next: (data) => {
          this.allApplications = data;
          this.pendingCount = data.filter(a => a.status === 'PENDING').length;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          if (err.status === 403) {
            alert('Access denied. Admin credentials required.');
          }
        }
      });
    }
  }

  approve(app: CoachApplication) {
    if (!app.id) return;
    if (!confirm(`Approve ${app.applicantName} as a coach?`)) return;

    this.processing[app.id] = true;
    const note = this.adminNotes[app.id] || '';
    this.coachingService.approveApplication(app.id, note).subscribe({
      next: (res) => {
        this.processing[app.id!] = false;
        alert(`✅ ${app.applicantName} has been approved as a coach.`);
        this.loadData();
      },
      error: (err) => {
        this.processing[app.id!] = false;
        alert(err.error?.message || 'Error approving application.');
      }
    });
  }

  reject(app: CoachApplication) {
    if (!app.id) return;
    if (!confirm(`Reject ${app.applicantName}'s application?`)) return;

    this.processing[app.id] = true;
    const note = this.adminNotes[app.id] || '';
    this.coachingService.rejectApplication(app.id, note).subscribe({
      next: () => {
        this.processing[app.id!] = false;
        alert(`❌ ${app.applicantName}'s application has been rejected.`);
        this.loadData();
      },
      error: (err) => {
        this.processing[app.id!] = false;
        alert(err.error?.message || 'Error rejecting application.');
      }
    });
  }

  openPdf(app: CoachApplication) {
    if (!app.cvFileBase64) return;
    
    // Convert base64 data URL back to Blob and open in new tab
    const arr = app.cvFileBase64.split(',');
    const mimeMatch = arr[0].match(/:(.*?);/);
    if (!mimeMatch) return;
    const mime = mimeMatch[1];
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    const blob = new Blob([u8arr], { type: mime });
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank');
  }
}
