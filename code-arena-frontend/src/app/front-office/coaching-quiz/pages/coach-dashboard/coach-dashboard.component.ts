import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { CoachingService } from '../../services/coaching.service';
import { AlertService } from '../../services/alert.service';
import { Coach, CoachingSession, SessionFeedback } from '../../models/coaching-session.model';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-coach-dashboard',
  standalone: true,
  imports: [CommonModule, CoachingNavbarComponent, RouterModule],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="dashboard-container">
        
        <div class="lc-header-main">
            <div class="header-text">
                <h1 class="glitch-title">COMMAND_<span>CENTER</span></h1>
                <p class="hero-desc">Welcome back, {{ coach?.name || 'OPERATOR' }}. Neural link established.</p>
            </div>
            <button class="action-neon-btn create" routerLink="/coaching-quiz/coach-create-session">
               + INITIALIZE_SESSION
            </button>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>SYNCING_MENTOR_PROFILE...</p>
        </div>

        <ng-container *ngIf="!loading && coach">
          <!-- STATS CARDS -->
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-icon neon-purple">🗓️</div>
              <div class="stat-content">
                <h3>{{ coach.totalSessions }}</h3>
                <p>TOTAL_SESSIONS</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon neon-blue">👥</div>
              <div class="stat-content">
                <h3>{{ totalLearnersScheduled }}</h3>
                <p>SYNCED_LEARNERS</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon neon-yellow">⭐</div>
              <div class="stat-content">
                <h3>{{ coach.rating | number:'1.1-1' }}</h3>
                <p>TRUST_SCORE</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon neon-green">🏆</div>
              <div class="stat-content">
                <h3 class="level-text">ELITE_SENSEI</h3>
                <p>SECURITY_RANK</p>
              </div>
            </div>
          </div>

          <!-- BOOKED SESSIONS -->
          <div class="section pt-4">
            <h2 class="section-label">RESERVED_CONNECTIONS_ACTIVE</h2>
            
            <div class="sessions-grid">
               <div class="lc-empty" *ngIf="reservedSessions.length === 0">
                 NO_RESERVED_CONNECTIONS_ACTIVE.
               </div>

              <div class="session-card reserved" *ngFor="let session of reservedSessions">
                <div class="card-inner">
                  <div class="session-header">
                    <span class="tag-mini">{{ session.language }}</span>
                    <span class="diff-pill easy">UPTIME {{ session.currentParticipants }}/{{ session.maxParticipants }}</span>
                  </div>
                  <h3 class="problem-title">{{ session.title }}</h3>
                  
                  <div class="session-info-box">
                    <div class="info-item"><span class="lbl">TIMESTAMP:</span> {{ session.scheduledAt | date:'medium' }}</div>
                  </div>

                  <div class="card-footer">
                    <button class="action-neon-btn abort" (click)="rejectSession(session)">REJECT</button>
                    <button class="action-neon-btn delete" (click)="deleteSession(session)">DELETE</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- OPEN SESSIONS -->
          <div class="section pt-4">
            <h2 class="section-label">OPEN_DATA_SLOTS</h2>
             
            <div class="sessions-grid">
               <div class="lc-empty" *ngIf="openSessions.length === 0">
                 ALL_SLOTS_SATURATED_OR_NONE_PUBLISHED.
               </div>

              <div class="session-card" *ngFor="let session of openSessions">
                <div class="card-inner">
                  <div class="session-header">
                    <span class="tag-mini">{{ session.language }}</span>
                    <span class="diff-pill medium">AWAITING_SIGNAL</span>
                  </div>
                  <h3 class="problem-title">{{ session.title }}</h3>
                  <div class="session-info-box">
                    <div class="info-item"><span class="lbl">EXPECTED:</span> {{ session.scheduledAt | date:'medium' }}</div>
                    <div class="info-item"><span class="lbl">RUNTIME:</span> {{ session.durationMinutes }}m</div>
                  </div>
                  <div class="card-footer">
                    <button class="action-neon-btn delete" (click)="deleteSession(session)">DELETE</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- RECENT FEEDBACKS -->
          <div class="section pt-4">
             <h2 class="section-label">NEURAL_CALIBRATION_LOGS</h2>

             <div class="lc-empty" *ngIf="feedbacks.length === 0">
                 NO_LOGS_DETECTED.
             </div>

             <div class="feedbacks-list" *ngIf="feedbacks.length > 0">
                <div class="feedback-card" *ngFor="let fb of feedbacks">
                  <div class="fb-header">
                     <span class="fb-user">USER::{{ fb.userId || 'ANON' }}</span>
                     <span class="fb-date">{{ fb.createdAt | date:'mediumDate' }}</span>
                  </div>
                  <div class="stars">
                     <span class="star" *ngFor="let star of [1,2,3,4,5]" [class.filled]="star <= fb.rating">★</span>
                  </div>
                  <p class="fb-comment">"{{ fb.comment }}"</p>
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
      --dark: #0a0a0f;
      --card: #0d0d15;
      --border: #1a1a2e;
      --text: #e2e8f0;
      --muted: #64748b;
    }

    @keyframes scan-line { 0% { transform: translateY(-100%); } 100% { transform: translateY(100vh); } }

    .lc-container {
      min-height: 100vh; background: var(--dark); font-family: 'Rajdhani', sans-serif; color: var(--text);
      position: relative; overflow: hidden; padding-bottom: 4rem;
    }
    .lc-container::before {
      content: ''; position: fixed; top: 0; left: 0; width: 100%; height: 2px;
      background: linear-gradient(90deg, transparent, var(--neon), transparent);
      animation: scan-line 4s linear infinite; opacity: 0.1; pointer-events: none; z-index: 999;
    }

    .dashboard-container { max-width: 1400px; margin: 0 auto; padding: 3rem 2rem; position: relative; z-index: 1; }

    .lc-header-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4rem; padding-bottom: 2rem; border-bottom: 1px solid var(--border); }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 0.5rem; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 15px var(--neon); }
    .hero-desc { font-size: 1.1rem; color: var(--muted); letter-spacing: 0.5px; }

    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1.5rem; margin-bottom: 4rem; }
    .stat-card {
      background: var(--card); border: 1px solid var(--border); border-radius: 4px; padding: 1.5rem;
      display: flex; align-items: center; gap: 1.5rem; transition: all 0.3s;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }
    .stat-card:hover { transform: translateY(-5px); border-color: var(--neon2); }
    
    .stat-icon { width: 50px; height: 50px; background: rgba(255,255,255,0.05); border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; }
    .neon-purple { color: #8b5cf6; text-shadow: 0 0 10px #8b5cf6; }
    .neon-blue { color: #06b6d4; text-shadow: 0 0 10px #06b6d4; }
    .neon-yellow { color: #ecc94b; text-shadow: 0 0 10px #ecc94b; }
    .neon-green { color: #10b981; text-shadow: 0 0 10px #10b981; }

    .stat-content h3 { font-family: 'Orbitron', monospace; font-size: 1.8rem; margin: 0; color: var(--text); }
    .stat-content p { margin: 0; color: var(--muted); font-size: 10px; font-weight: 700; letter-spacing: 1px; }

    .section { margin-bottom: 4rem; }
    .pt-4 { padding-top: 2rem; border-top: 1px solid var(--border); }
    .section-label { font-family: 'Orbitron', sans-serif; font-size: 14px; color: var(--neon2); margin-bottom: 2rem; letter-spacing: 2px; }

    .sessions-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 2rem; }
    .session-card {
      background: var(--card); border: 1px solid var(--border); border-radius: 8px; position: relative; overflow: hidden;
      clip-path: polygon(20px 0%, 100% 0%, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0% 100%, 0% 20px);
    }
    .session-card.reserved { border-left: 4px solid var(--neon3); }
    .card-inner { padding: 2rem; }
    
    .session-header { display: flex; justify-content: space-between; margin-bottom: 1.5rem; }
    .tag-mini { padding: 4px 10px; background: rgba(139,92,246,0.1); color: var(--neon); font-family: 'Orbitron', monospace; font-size: 9px; }
    .diff-pill { font-family: 'Orbitron', monospace; font-size: 9px; font-weight: 700; padding: 4px 10px; }
    .diff-pill.easy { background: rgba(16,185,129,0.1); color: var(--neon3); border: 1px solid var(--neon3); }
    .diff-pill.medium { background: rgba(6,182,212,0.1); color: var(--neon2); border: 1px solid var(--neon2); }

    .problem-title { font-family: 'Orbitron', sans-serif; color: var(--text); margin: 0 0 1rem; font-size: 1.3rem; }
    .session-info-box { background: rgba(0,0,0,0.4); border: 1px solid var(--border); padding: 1rem; margin-bottom: 1.5rem; }
    .info-item { color: var(--text); font-size: 0.85rem; font-family: monospace; display: flex; gap: 8px; margin-bottom: 4px; }
    .info-item .lbl { color: var(--muted); font-family: 'Orbitron', sans-serif; font-size: 9px; }
    .sim-link { color: var(--neon2); font-weight: 700; text-decoration: none; border-bottom: 1px dashed var(--neon2); }

    .feedbacks-list { display: flex; flex-direction: column; gap: 1rem; }
    .feedback-card { background: var(--card); border: 1px solid var(--border); padding: 1.5rem; clip-path: polygon(0 0, 100% 0, 95% 100%, 0% 100%); }
    .fb-header { display: flex; justify-content: space-between; margin-bottom: 0.5rem; font-family: 'Orbitron', monospace; font-size: 11px; }
    .fb-user { color: var(--neon2); }
    .fb-comment { color: #a1a1a6; font-style: italic; margin-top: 1rem; line-height: 1.6; }
    .stars { display: flex; color: var(--border); font-size: 0.9rem; }
    .stars .filled { color: var(--neon); }

    .card-footer { display: flex; justify-content: flex-end; gap: 10px; }
    .action-neon-btn {
      padding: 10px 20px; font-family: 'Orbitron', monospace; font-size: 10px; font-weight: 900;
      border: none; cursor: pointer; transition: all 0.3s;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }
    .action-neon-btn.create { background: var(--neon); color: #fff; }
    .action-neon-btn.create:hover { background: var(--neon2); box-shadow: 0 0 20px var(--neon2); }
    .action-neon-btn.abort { background: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid #ef4444; }
    .action-neon-btn.delete { background: #000; color: var(--muted); border: 1px solid var(--border); }
    .action-neon-btn.delete:hover { color: #ef4444; border-color: #ef4444; }

    .lc-loading, .lc-empty { text-align: center; padding: 4rem; font-family: 'Orbitron', monospace; color: var(--muted); letter-spacing: 2px; }
    .spinner { width: 40px; height: 40px; border: 2px solid var(--border); border-top-color: var(--neon); border-radius: 50%; margin: 0 auto 1.5rem; animation: spin 1s linear infinite; }
  `]
})
export class CoachDashboardComponent implements OnInit {
  coachUserId: string = '';
  coach: Coach | null = null;

  mySessions: CoachingSession[] = [];
  reservedSessions: CoachingSession[] = [];
  openSessions: CoachingSession[] = [];
  feedbacks: SessionFeedback[] = [];

  totalLearnersScheduled: number = 0;
  loading = true;

  constructor(
    private coachingService: CoachingService,
    private alertService: AlertService,
    private auth: AuthService
  ) { }

  ngOnInit() {
    this.auth.user$.subscribe((user: any) => {
      if (user && user.sub) {
        this.coachUserId = user.sub;
        this.fetchCoachData();
      } else {
        this.loading = false;
      }
    });
  }

  fetchCoachData() {
    this.loading = true;

    // 1. Fetch coach profile
    this.coachingService.getAllCoaches().subscribe({
      next: (coaches) => {
        const found = coaches.find(c => c.userId === this.coachUserId);
        if (found) {
          this.coach = found;
        } else {
          // Provide a default empty coach profile for new coaches
          this.coach = {
            id: 'new-coach',
            userId: this.coachUserId,
            name: 'New Coach',
            bio: 'Welcome to Code Arena! Setup your profile to attract learners.',
            specializations: [],
            rating: 0,
            totalSessions: 0
          };
        }
        this.fetchSessions();
        this.fetchFeedbacks();
      },
      error: () => { this.loading = false; }
    });
  }

  fetchSessions() {
    // 2. Fetch all sessions and filter by this coach
    this.coachingService.getAllSessions().subscribe({
      next: (data) => {
        this.mySessions = data.filter(s => s.coachId === this.coachUserId);

        // Split into strictly reserved vs open
        this.reservedSessions = this.mySessions.filter(s => s.currentParticipants > 0);
        this.openSessions = this.mySessions.filter(s => s.currentParticipants === 0);

        // Calculate total learners waiting across all sessions
        this.totalLearnersScheduled = this.reservedSessions.reduce((acc, curr) => acc + curr.currentParticipants, 0);

        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  fetchFeedbacks() {
    this.coachingService.getCoachFeedbacks(this.coachUserId).subscribe({
      next: (data) => {
        this.feedbacks = data;
      },
      error: () => {
        console.error('Could not fetch feedbacks.');
      }
    });
  }

  async rejectSession(session: CoachingSession) {
    if (!session.id) return;
    const msg = `Etes-vous sûr de vouloir rejeter cette session ("${session.title}")? Les participants recevront un e-mail d'annulation.`;
    const confirmed = await this.alertService.showConfirm('REJECTION_PROTOCOL', msg);
    if (confirmed) {
      this.coachingService.rejectSession(session.id).subscribe({
        next: () => {
          this.alertService.success('Session rejetée avec succès.', 'PROTOCOL_COMPLETE');
          this.fetchSessions();
        },
        error: (err) => {
          console.error(err);
          this.alertService.error('Erreur lors du rejet de la session.', 'SYSTEM_ERROR');
        }
      });
    }
  }

  async deleteSession(session: CoachingSession) {
    if (!session.id) return;
    const msg = `⚠️ ATTENTION: Voulez-vous supprimer définitivement cette session ("${session.title}")? Cette action est irréversible.`;
    const confirmed = await this.alertService.showConfirm('PURGE_PROTOCOL', msg);
    if (confirmed) {
      this.coachingService.deleteSession(session.id).subscribe({
        next: () => {
          this.alertService.success('Session supprimée définitivement.', 'PROTOCOL_COMPLETE');
          this.fetchSessions();
        },
        error: (err) => {
          console.error(err);
          this.alertService.error('Erreur lors de la suppression de la session.', 'SYSTEM_ERROR');
        }
      });
    }
  }
}
