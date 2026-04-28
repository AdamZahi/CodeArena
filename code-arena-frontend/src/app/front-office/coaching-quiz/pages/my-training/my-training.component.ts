import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { CoachingService } from '../../services/coaching.service';
import { AlertService } from '../../services/alert.service';
import { Dashboard, CoachingSession } from '../../models/coaching-session.model';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-my-training',
  standalone: true,
  imports: [CommonModule, CoachingNavbarComponent, RouterModule],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="dashboard-container">
        
        <div class="lc-hero">
          <h1 class="glitch-title">NEURAL_<span>SCHEDULE</span></h1>
          <p class="hero-desc">Monitor your synchronization with mentors. Maintain peak performance levels.</p>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>RETRIEVING_USER_DATA...</p>
        </div>

        <ng-container *ngIf="!loading && dashboard">
          <div class="section">
            <h2 class="section-label">RESERVED_CONNECTIONS</h2>
            
            <div class="carousel-container" *ngIf="dashboard.upcomingSessions.length > 0">
              <button class="carousel-btn prev" (click)="scrollCarousel(-1)">
                <span class="btn-box"> < </span>
              </button>
              
              <div class="carousel-viewport" #carouselViewport>
                <div class="carousel-track">
                  <div class="session-card booked" *ngFor="let session of dashboard.upcomingSessions">
                    <div class="card-inner">
                      <div class="session-header">
                        <span class="tag-mini">{{ session.language }}</span>
                        <span class="diff-pill easy">LIVE_LINK_ACTIVE</span>
                      </div>
                      <h3 class="problem-title">{{ session.title }}</h3>
                      
                      <div class="session-info-box">
                        <div class="info-item">
                           <span class="lbl">TIMESTAMP:</span> 
                           <span class="val">{{ session.scheduledAt | date:'medium' }}</span>
                        </div>
                        <div class="info-item" *ngIf="session.meetingUrl">
                          <span class="lbl">LINK:</span>
                          <a [href]="session.meetingUrl" target="_blank" class="sim-link">JOIN_DOJO</a>
                        </div>
                        <div class="info-item" *ngIf="!session.meetingUrl">
                          <span class="lbl">STATUS:</span>
                          <span class="val-muted">AWAITING_UPLOADER...</span>
                        </div>
                      </div>

                      <div class="card-footer">
                        <button class="action-neon-btn pay" [routerLink]="['/coaching-quiz/pay', session.id]">
                          INITIALIZE_PAYMENT
                        </button>
                        <button class="action-neon-btn abort" (click)="cancelSession(session.id)">
                          TERMINATE
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <button class="carousel-btn next" (click)="scrollCarousel(1)">
                <span class="btn-box"> > </span>
              </button>
            </div>

            <div class="lc-empty" *ngIf="dashboard.upcomingSessions.length === 0">
              <p>NO_UPCOMING_SYNC_DETECTED.</p>
              <button class="action-neon-btn" routerLink="/coaching-quiz/coaches">FIND_NEURAL_LINK</button>
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

    .lc-hero { text-align: center; margin-bottom: 4rem; padding-bottom: 2rem; border-bottom: 1px solid var(--border); }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 1rem; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 15px var(--neon); }
    .hero-desc { font-size: 1.1rem; color: var(--muted); max-width: 600px; margin: 0 auto; letter-spacing: 0.5px; }

    .section-label { font-family: 'Orbitron', sans-serif; font-size: 14px; font-weight: 700; color: var(--neon2); letter-spacing: 2px; margin-bottom: 2rem; }

    /* CAROUSEL */
    .carousel-container { position: relative; display: flex; align-items: center; gap: 1.5rem; }
    .carousel-viewport { overflow-x: hidden; scroll-behavior: smooth; width: 100%; mask-image: linear-gradient(to right, transparent, black 10%, black 90%, transparent); }
    .carousel-track { display: flex; gap: 2rem; padding: 2rem 0; width: max-content; }
    
    .session-card {
      width: 400px; background: var(--card); border: 1px solid var(--border); border-radius: 8px;
      position: relative; overflow: hidden; transition: all 0.3s;
      clip-path: polygon(20px 0%, 100% 0%, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0% 100%, 0% 20px);
    }
    .session-card.booked { border-left: 4px solid var(--neon3); }
    .session-card:hover { transform: translateY(-8px); border-color: var(--neon2); }
    
    .card-inner { padding: 2rem; }

    .session-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .tag-mini { padding: 4px 10px; background: rgba(139,92,246,0.1); color: var(--neon); font-family: 'Orbitron', monospace; font-size: 9px; clip-path: polygon(4px 0%, 100% 0%, calc(100% - 4px) 100%, 0% 100%); }
    .diff-pill { font-family: 'Orbitron', monospace; font-size: 9px; font-weight: 700; padding: 4px 10px; letter-spacing: 1px; }
    .diff-pill.easy { background: rgba(16,185,129,0.1); color: var(--neon3); border: 1px solid var(--neon3); }

    .problem-title { font-family: 'Orbitron', sans-serif; color: var(--text); font-size: 1.4rem; margin: 0 0 1.5rem; }
    
    .session-info-box { background: rgba(10,10,15,0.6); border: 1px solid var(--border); border-radius: 4px; padding: 1.2rem; margin-bottom: 2rem; }
    .info-item { display: flex; gap: 1rem; margin-bottom: 0.5rem; font-size: 0.85rem; }
    .info-item .lbl { color: var(--muted); font-family: 'Orbitron', monospace; font-size: 9px; }
    .info-item .val { color: var(--text); font-family: monospace; }
    .info-item .val-muted { color: #444; font-style: italic; }
    
    .sim-link { color: var(--neon2); font-weight: 700; text-decoration: none; border: 1px solid var(--neon2); padding: 2px 8px; font-size: 10px; font-family: 'Orbitron', monospace; }
    .sim-link:hover { background: var(--neon2); color: #000; box-shadow: 0 0 10px var(--neon2); }

    .card-footer { display: flex; gap: 0.5rem; justify-content: flex-end; padding-top: 1.5rem; border-top: 1px solid var(--border); }
    .action-neon-btn {
      padding: 10px 16px; font-family: 'Orbitron', monospace; font-size: 10px; font-weight: 900; 
      border: none; cursor: pointer; transition: all 0.3s;
      clip-path: polygon(8px 0%, 100% 0%, calc(100% - 8px) 100%, 0% 100%);
    }
    .action-neon-btn.pay { background: var(--text); color: #000; }
    .action-neon-btn.pay:hover { background: var(--neon2); box-shadow: 0 0 15px var(--neon2); }
    .action-neon-btn.abort { background: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.3); }
    .action-neon-btn.abort:hover { background: #ef4444; color: #fff; box-shadow: 0 0 15px #ef4444; }

    .carousel-btn {
      width: 50px; height: 50px; background: var(--card); border: 1px solid var(--border);
      color: var(--neon2); cursor: pointer; display: flex; align-items: center; justify-content: center;
      transition: all 0.2s; z-index: 10; font-family: 'Orbitron', monospace;
    }
    .carousel-btn:hover { border-color: var(--neon); color: var(--neon); box-shadow: 0 0 15px var(--neon); }

    .lc-loading, .lc-empty { text-align: center; padding: 5rem; font-family: 'Orbitron', monospace; letter-spacing: 4px; color: var(--muted); }
    .spinner { width: 40px; height: 40px; border: 2px solid var(--border); border-top-color: var(--neon); border-radius: 50%; margin: 0 auto 1.5rem; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class MyTrainingComponent implements OnInit {
  @ViewChild('carouselViewport') carouselViewport!: ElementRef;
  dashboard: Dashboard | null = null;
  loading = true;

  constructor(private coachingService: CoachingService, private alertService: AlertService) { }

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    this.loading = true;
    this.coachingService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.alertService.error('Erreur lors du chargement des sessions.', 'DATA_FETCH_ERROR');
      }
    });
  }

  scrollCarousel(direction: number) {
    const scrollAmount = 400 * direction;
    this.carouselViewport.nativeElement.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  }

  async cancelSession(sessionId: string) {
    const confirmed = await this.alertService.showConfirm('TERMINATION_PROTOCOL', 'Êtes-vous sûr de vouloir annuler cette réservation ?');
    if (confirmed) {
      this.coachingService.cancelReservation(sessionId).subscribe({
        next: () => {
          this.alertService.success('Réservation annulée avec succès.', 'PROTOCOL_COMPLETE');
          this.loadDashboard();
        },
        error: () => {
          this.alertService.error('Erreur lors de l\'annulation.', 'TERMINATION_FAILURE');
        }
      });
    }
  }
}
