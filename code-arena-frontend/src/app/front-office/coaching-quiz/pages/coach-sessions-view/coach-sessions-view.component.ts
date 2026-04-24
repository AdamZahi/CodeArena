import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { CoachingService } from '../../services/coaching.service';
import { AlertService } from '../../services/alert.service';
import { CoachingSession, Coach } from '../../models/coaching-session.model';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-coach-sessions-view',
  standalone: true,
  imports: [CommonModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="sessions-view-container">
        
        <div class="header-section">
           <button class="back-link" (click)="goBack()">[ RETURN_TO_MENTORS ]</button>
           
           <div class="mentor-details" *ngIf="coach">
             <div class="avatar-box">
               <div class="avatar">{{ coach.userId.substring(0, 2).toUpperCase() }}</div>
               <div class="avatar-glow"></div>
             </div>
             <div class="info">
               <h1 class="glitch-title">OPERATOR_<span>{{ coach.userId }}</span></h1>
               <div class="trust-stats">
                 <div class="stars">
                   <span class="star" *ngFor="let s of getStars(coach.rating)" [class.filled]="s">★</span>
                 </div>
                 <span class="rating-val">{{ coach.rating | number:'1.1-1' }}</span>
                 <span class="separator">//</span>
                 <span class="sess-count">{{ coach.totalSessions }} COMPLETED_SYNC</span>
               </div>
               <p class="bio-desc">{{ coach.bio }}</p>
               <div class="specs">
                 <span class="spec-tag" *ngFor="let spec of coach.specializations">{{ spec }}</span>
               </div>
             </div>
           </div>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>RETRIEVING_DOJO_SCHEDULE...</p>
        </div>

        <div class="sessions-grid" *ngIf="!loading">
           <div class="lc-empty" *ngIf="sessions.length === 0">
              <p>NO_UPCOMING_SYNC_DETECTED_FOR_THIS_OPERATOR.</p>
           </div>
           
           <div class="session-card" *ngFor="let session of sessions">
             <div class="card-inner">
               <div class="session-header">
                 <span class="tag-mini">{{ session.language }}</span>
                 <span class="diff-pill" [class]="'tag-' + session.level.toLowerCase()">{{ session.level }}</span>
               </div>
               <h3 class="problem-title">{{ session.title }}</h3>
               <p class="session-desc-text">{{ session.description }}</p>
               
               <div class="session-info-box">
                 <div class="info-item"><span class="lbl">TIMESTAMP:</span> {{ session.scheduledAt | date:'medium' }}</div>
                 <div class="info-item"><span class="lbl">RUNTIME:</span> {{ session.durationMinutes }} Min</div>
                 <div class="info-item"><span class="lbl">LOAD:</span> {{ session.currentParticipants }}/{{ session.maxParticipants }}</div>
               </div>

               <div class="card-footer">
                 <button class="action-neon-btn sync" 
                   (click)="handleAction(session)"
                   [disabled]="session.status !== 'COMPLETED' && session.currentParticipants >= session.maxParticipants"
                   [ngClass]="{'evaluate-mode': session.status === 'COMPLETED'}">
                   {{ session.status === 'COMPLETED' ? 'CALIBRATE_FEEDBACK' : (session.currentParticipants >= session.maxParticipants ? 'LOAD_FULL' : 'INITIALIZE_SYNC') }}
                 </button>
               </div>
             </div>
           </div>
        </div>

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

    .sessions-view-container { max-width: 1400px; margin: 0 auto; padding: 3rem 2rem; position: relative; z-index: 1; }
    
    .back-link { 
      background: transparent; color: var(--muted); border: none; font-family: 'Orbitron', monospace; 
      font-size: 11px; cursor: pointer; margin-bottom: 3rem; letter-spacing: 2px;
      transition: all 0.3s; padding: 0;
    }
    .back-link:hover { color: var(--neon2); text-shadow: 0 0 10px var(--neon2); }

    .header-section { margin-bottom: 4rem; padding-bottom: 3rem; border-bottom: 1px solid var(--border); }
    
    .mentor-details { display: flex; gap: 3rem; align-items: flex-start; }
    .avatar-box { position: relative; flex-shrink: 0; }
    .avatar { 
      width: 100px; height: 100px; background: var(--card); color: var(--text); border: 2px solid var(--neon); 
      display: flex; align-items: center; justify-content: center; font-size: 2.5rem; font-family: 'Orbitron', sans-serif;
      clip-path: polygon(15% 0, 100% 0, 100% 85%, 85% 100%, 0 100%, 0 15%);
    }
    .avatar-glow { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: var(--neon); filter: blur(20px); opacity: 0.1; }

    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin: 0 0 1rem; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 15px var(--neon); }
    
    .trust-stats { display: flex; align-items: center; margin-bottom: 1.5rem; font-family: 'Orbitron', monospace; }
    .stars { display: flex; color: var(--border); font-size: 1rem; margin-right: 15px; }
    .star.filled { color: var(--neon); text-shadow: 0 0 10px var(--neon); }
    .rating-val { color: var(--neon2); font-weight: 700; }
    .separator { color: var(--border); margin: 0 1rem; }
    .sess-count { color: var(--muted); font-size: 10px; letter-spacing: 2px; }
    
    .bio-desc { font-size: 1.2rem; line-height: 1.8; color: var(--text); margin-bottom: 2rem; max-width: 800px; font-weight: 300; }
    
    .specs { display: flex; flex-wrap: wrap; gap: 0.8rem; }
    .spec-tag { 
      background: rgba(255,255,255,0.03); color: var(--muted); padding: 5px 12px; border: 1px solid var(--border); 
      font-size: 10px; font-weight: 700; font-family: 'Orbitron', monospace; letter-spacing: 1px;
    }

    .sessions-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 2.5rem; }
    .session-card {
      background: var(--card); border: 1px solid var(--border); position: relative; overflow: hidden;
      clip-path: polygon(25px 0%, 100% 0%, 100% calc(100% - 25px), calc(100% - 25px) 100%, 0% 100%, 0% 25px);
      transition: all 0.3s;
    }
    .session-card:hover { transform: translateY(-8px); border-color: var(--neon); box-shadow: 0 15px 40px rgba(0,0,0,0.6); }
    .card-inner { padding: 2.5rem; }
    
    .session-header { display: flex; justify-content: space-between; margin-bottom: 2rem; }
    .tag-mini { padding: 4px 12px; background: rgba(139,92,246,0.1); color: var(--neon); font-family: 'Orbitron', monospace; font-size: 10px; border: 1px solid rgba(139,92,246,0.3); }
    
    .diff-pill { font-family: 'Orbitron', monospace; font-size: 9px; font-weight: 700; padding: 4px 12px; border-radius: 2px; }
    .tag-basique { color: var(--neon3); border: 1px solid var(--neon3); background: rgba(16,185,129,0.05); }
    .tag-intermediaire { color: var(--neon2); border: 1px solid var(--neon2); background: rgba(6,182,212,0.05); }
    .tag-avance { color: #f43f5e; border: 1px solid #f43f5e; background: rgba(244,63,94,0.05); }

    .problem-title { font-family: 'Orbitron', sans-serif; color: var(--text); margin: 0 0 1.2rem; font-size: 1.4rem; letter-spacing: 1px; }
    .session-desc-text { color: var(--muted); font-size: 0.95rem; line-height: 1.7; margin-bottom: 2rem; }
    
    .session-info-box { background: rgba(0,0,0,0.4); border: 1px solid var(--border); padding: 1.2rem; margin-bottom: 2rem; }
    .info-item { color: var(--text); font-size: 0.85rem; font-family: monospace; display: flex; gap: 10px; margin-bottom: 6px; }
    .info-item .lbl { color: var(--muted); font-family: 'Orbitron', sans-serif; font-size: 9px; min-width: 80px; }

    .card-footer { display: flex; justify-content: flex-end; }
    .action-neon-btn.sync {
      padding: 12px 25px; font-family: 'Orbitron', monospace; font-size: 11px; font-weight: 900;
      border: none; cursor: pointer; transition: all 0.3s; background: var(--neon); color: #fff;
      clip-path: polygon(15px 0%, 100% 0%, calc(100% - 15px) 100%, 0% 100%);
      letter-spacing: 2px;
    }
    .action-neon-btn.sync:hover:not(:disabled) { background: var(--neon2); box-shadow: 0 0 25px var(--neon2); transform: scale(1.05); }
    .action-neon-btn.sync:disabled { opacity: 0.3; cursor: not-allowed; }
    .action-neon-btn.evaluate-mode { background: var(--neon3); }

    .lc-loading, .lc-empty { text-align: center; padding: 6rem; font-family: 'Orbitron', monospace; color: var(--muted); letter-spacing: 3px; }
    .spinner { width: 50px; height: 50px; border: 3px solid var(--border); border-top-color: var(--neon); border-radius: 50%; margin: 0 auto 2rem; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class CoachSessionsViewComponent implements OnInit {
    coachUserId: string = '';
    coach: Coach | null = null;
    sessions: CoachingSession[] = [];
    loading = true;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private coachingService: CoachingService,
        private alertService: AlertService
    ) { }

    ngOnInit() {
        this.coachUserId = this.route.snapshot.paramMap.get('userId') || '';
        if (this.coachUserId) {
            this.fetchData();
        } else {
            this.goBack();
        }
    }

    fetchData() {
        this.loading = true;

        this.coachingService.getAllCoaches().subscribe({
            next: (coaches) => {
                const found = coaches.find(c => c.userId === this.coachUserId);
                if (found) {
                    this.coach = found;
                    this.fetchSessions();
                } else {
                    this.goBack();
                }
            },
            error: () => this.goBack()
        });
    }

    fetchSessions() {
        this.coachingService.getAllSessions().subscribe({
            next: (data) => {
                this.sessions = data.filter(s => s.coachId === this.coachUserId);
                this.loading = false;
            },
            error: () => {
                this.loading = false;
                this.alertService.error('Error fetching sessions.');
            }
        });
    }

    getStars(rating: number): boolean[] {
        const stars = [];
        for (let i = 1; i <= 5; i++) {
            stars.push(i <= Math.round(rating));
        }
        return stars;
    }

    handleAction(session: CoachingSession) {
        if (session.status === 'COMPLETED') {
             this.router.navigate(['/coaching-quiz/evaluate', session.coachId]);
        } else {
             this.bookSession(session.id);
        }
    }

    bookSession(sessionId: string) {
        this.coachingService.bookSession(sessionId).subscribe({
            next: () => {
                this.alertService.success('Session booked successfully!');
                this.router.navigate(['/coaching-quiz/sessions']);
            },
            error: (err) => {
                this.alertService.error(err.error?.message || 'Error occurred while booking the session.');
            }
        });
    }

    goBack() {
        this.router.navigate(['/coaching-quiz/coaches']);
    }
}
