import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoachingService } from '../../services/coaching.service';
import { Coach, CoachingSession } from '../../models/coaching-session.model';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-coach-reservations',
  standalone: true,
  imports: [CommonModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="dashboard-container">
        
        <div class="lc-hero">
          <h1 class="glitch-title">OPERATOR_<span>SCHEDULE</span></h1>
          <p class="hero-desc">Manage your synchronization sessions and establish secure neural uplinks with designated learners.</p>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>FETCHING_RESERVATION_DATA...</p>
        </div>

        <ng-container *ngIf="!loading">
          <div class="dashboard-section pt-4">
            <div class="section-title-badge mb-5">
              <h2 class="section-glitch-title">UPCOMING_<span>BOOKED_SESSIONS</span></h2>
              <span class="badge active-session">SECURE_SYNC</span>
            </div>
            
            <div class="cards-grid">
               <div class="lc-empty" *ngIf="reservedSessions.length === 0">
                 <p>NO_ACTIVE_RESERVATIONS_DETECTED. DATA_STREAM_IDLE.</p>
               </div>

              <div class="coach-style-card reserved-hl" *ngFor="let session of reservedSessions">
                <div class="card-inner">
                  <div class="coach-avatar">
                     <div class="avatar-box">{{ session.language.substring(0,1).toUpperCase() }}</div>
                  </div>
                  
                  <div class="coach-header">
                    <h3 class="problem-title">{{ session.title }}</h3>
                    <div class="status-row">
                      <span class="status-chip success">BOOKED: {{ session.currentParticipants }}/{{ session.maxParticipants }}</span>
                    </div>
                  </div>

                  <p class="bio">{{ session.description }}</p>
                  
                  <div class="session-meta-box">
                    <div class="meta-item">
                      <span class="meta-lbl">TIMESTAMP</span>
                      <span class="meta-val">{{ session.scheduledAt | date:'medium' }}</span>
                    </div>
                  </div>

                  <div class="coach-footer">
                    <div class="stat-group">
                      <span class="stat-val">{{ session.language }}</span>
                      <span class="stat-lbl">LANGUAGE_NODE</span>
                    </div>
                    
                    <button class="action-neon-btn purple" 
                      (click)="sendMeetLink(session)" 
                      [disabled]="session.id && sendingEmails[session.id]">
                      <span *ngIf="!session.id || !sendingEmails[session.id]">SEND_MEET_LINK</span>
                      <span *ngIf="session.id && sendingEmails[session.id]" class="spinner-small"></span>
                    </button>
                  </div>
                  <div class="eval-strip">DATA_STREAM_ACTIVE</div>
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
      background: linear-gradient(90deg, transparent, var(--neon), transparent);
      animation: scan-line 4s linear infinite; opacity: 0.1; pointer-events: none; z-index: 999;
    }

    .dashboard-container { max-width: 1400px; margin: 0 auto; padding: 4rem 2rem; position: relative; z-index: 1; }
    
    .lc-hero { text-align: center; margin-bottom: 5rem; }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 0.5rem; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 15px var(--neon); }
    .hero-desc { font-size: 1rem; color: var(--muted); max-width: 600px; margin: 0 auto; line-height: 1.6; letter-spacing: 0.5px; text-transform: uppercase; }

    .section-title-badge { display: flex; align-items: center; gap: 2rem; margin-bottom: 3rem; }
    .active-session { background: var(--neon3); color: #000; font-family: 'Orbitron', monospace; font-size: 9px; padding: 4px 12px; letter-spacing: 2px; font-weight: 900; box-shadow: 0 0 10px var(--neon3); }
    
    .section-glitch-title { font-family: 'Orbitron', sans-serif; font-size: 1.8rem; font-weight: 900; color: #fff; letter-spacing: 4px; margin: 0; }
    .section-glitch-title span { color: var(--neon2); text-shadow: 0 0 10px var(--neon2); }

    .cards-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(440px, 1fr)); gap: 2.5rem; }

    .coach-style-card { 
      background: var(--card); border: 1px solid var(--border); position: relative; overflow: hidden; transition: all 0.3s;
      clip-path: polygon(20px 0%, 100% 0%, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0% 100%, 0% 20px);
    }
    .reserved-hl { border-left: 3px solid var(--neon3); }
    .coach-style-card:hover { transform: translateY(-5px); border-color: var(--neon); box-shadow: 0 0 30px rgba(139, 92, 246, 0.1); }
    
    .card-inner { padding: 2.5rem; }
    .coach-avatar { margin-bottom: 2rem; }
    .avatar-box { width: 50px; height: 50px; background: rgba(16, 185, 129, 0.1); border: 1px solid var(--neon3); color: var(--neon3); display: flex; align-items: center; justify-content: center; font-family: 'Orbitron', monospace; font-weight: 900; font-size: 20px; clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); }

    .problem-title { font-family: 'Orbitron', sans-serif; color: #fff; font-size: 1.5rem; margin: 0 0 1rem; }
    .status-chip { font-family: 'Orbitron', monospace; font-size: 9px; padding: 4px 10px; border: 1px solid transparent; font-weight: 900; letter-spacing: 1px; }
    .status-chip.success { color: var(--neon3); border-color: var(--neon3); background: rgba(16, 185, 129, 0.05); }

    .bio { font-size: 0.95rem; line-height: 1.6; color: var(--muted); margin-bottom: 2rem; min-height: 4rem; }

    .session-meta-box { background: rgba(0,0,0,0.3); border: 1px solid var(--border); padding: 1.5rem; margin-bottom: 2.5rem; display: flex; flex-direction: column; gap: 1rem; }
    .meta-item { display: flex; flex-direction: column; gap: 4px; }
    .meta-lbl { font-size: 9px; color: var(--muted); font-family: 'Orbitron', monospace; letter-spacing: 2px; }
    .meta-val { font-size: 0.9rem; color: #fff; font-family: 'Fira Code', monospace; }
    .meta-val.link { color: var(--neon-red); text-decoration: none; border-bottom: 1px solid transparent; width: fit-content; transition: all 0.3s; }
    .meta-val.link:hover { color: #fff; border-bottom-color: #fff; }

    .coach-footer { padding-top: 2rem; border-top: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
    .stat-val { font-family: 'Orbitron', monospace; font-size: 1.3rem; color: #fff; font-weight: 700; display: block; }
    .stat-lbl { font-size: 10px; color: var(--muted); letter-spacing: 1px; }

    .action-neon-btn { 
      background: var(--neon); color: #fff; border: none; padding: 12px 24px; font-family: 'Orbitron', monospace; font-size: 10px; font-weight: 900; 
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); cursor: pointer; transition: all 0.3s; letter-spacing: 1px;
    }
    .action-neon-btn:hover:not(:disabled) { background: var(--neon2); box-shadow: 0 0 20px var(--neon2); }
    .action-neon-btn:disabled { opacity: 0.3; cursor: not-allowed; }

    .eval-strip { margin-top: 2rem; padding: 10px; background: rgba(6, 182, 212, 0.05); border: 1px solid rgba(6, 182, 212, 0.1); color: var(--neon2); font-family: 'Orbitron', monospace; font-size: 9px; text-align: center; letter-spacing: 2px; font-weight: 900; }

    .lc-loading, .lc-empty { text-align: center; padding: 5rem; font-family: 'Orbitron', monospace; color: var(--muted); letter-spacing: 3px; }
    .spinner { width: 50px; height: 50px; border: 2px solid var(--border); border-top-color: var(--neon); border-radius: 50%; margin: 0 auto 2rem; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .spinner-small { display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.8s linear infinite; }

    @media (max-width: 992px) {
      .cards-grid { grid-template-columns: 1fr; }
    }
  `]
})
export class CoachReservationsComponent implements OnInit {
  coachUserId: string = '';
  coach: Coach | null = null;
  reservedSessions: CoachingSession[] = [];
  loading = true;

  constructor(private coachingService: CoachingService, private auth: AuthService) { }

  ngOnInit() {
    this.auth.user$.subscribe((user: any) => {
      if (user && user.sub) {
        this.coachUserId = user.sub;
        this.fetchData();
      } else {
        this.loading = false;
      }
    });
  }

  fetchData() {
    this.loading = true;
    this.coachingService.getAllSessions().subscribe({
      next: (data) => {
        const mySessions = data.filter(s => s.coachId === this.coachUserId);
        this.reservedSessions = mySessions.filter(s => s.currentParticipants > 0);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        alert('Erreur lors du chargement des réservations.');
      }
    });
  }

  sendingEmails: { [key: string]: boolean } = {};

  sendMeetLink(session: CoachingSession) {
    if (!session.id) return;

    this.sendingEmails[session.id] = true;

    this.coachingService.sendMeetingLinks(session.id).subscribe({
      next: (res) => {
        this.sendingEmails[session.id] = false;
        if (res.success) {
          alert('Les e-mails contenant le lien ont été envoyés avec succès aux participants !');
        } else {
          alert('Erreur lors de l\'envoi : ' + res.message);
        }
      },
      error: (err) => {
        this.sendingEmails[session.id] = false;
        console.error('Email sending error:', err);
        const errorMsg = err.error?.message || 'Erreur inconnue (Vérifiez votre connexion SMTP ou le moniteur réseau).';
        alert('Erreur: ' + errorMsg);
      }
    });
  }
}
