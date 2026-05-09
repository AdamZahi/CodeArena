import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { CoachingService } from '../../services/coaching.service';
import { Coach } from '../../models/coaching-session.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-coach-list',
  standalone: true,
  imports: [CommonModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="coaches-container">
        <div class="lc-hero">
          <h1 class="glitch-title">MENTOR_<span>DIRECTORY</span></h1>
          <p class="hero-desc">Access the Neural Network of top-tier developers. Choose your guide to the Shodan rank.</p>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>SCANNING_NETWORK...</p>
        </div>

        <div class="carousel-container" *ngIf="!loading && coaches.length > 0">
          <button class="carousel-btn prev" (click)="scrollCarousel(-1)">
             <span class="btn-box"> < </span>
          </button>
          
          <div class="carousel-viewport" #carouselViewport>
            <div class="carousel-track">
              <div class="coach-card" *ngFor="let coach of coaches">
                <div class="card-inner">
                  <div class="coach-avatar">
                   <div class="avatar-box"> {{ (coach.name || coach.userId).substring(0, 1).toUpperCase() }} </div>
                  </div>
                  
                  <div class="coach-header">
                    <h3 class="problem-title">{{ coach.name || coach.userId }}</h3>
                    <div class="stars">
                      <span class="star" *ngFor="let s of getStars(coach.rating)" [class.filled]="s">★</span>
                      <span class="rating-val">LVL {{ coach.rating | number:'1.1-1' }}</span>
                    </div>
                  </div>

                  <p class="bio">{{ coach.bio }}</p>
                  
                  <div class="tag-list-mini">
                    <span class="tag-mini" *ngFor="let spec of coach.specializations">{{ spec }}</span>
                  </div>

                  <div class="coach-footer">
                    <div class="stat-group">
                      <span class="stat-val">{{ coach.totalSessions }}</span>
                      <span class="stat-lbl">UPTIME_SESSIONS</span>
                    </div>
                    
                    <button class="action-neon-btn" (click)="viewSessions($event, coach.userId)">
                      SELECT_TARGET
                    </button>
                  </div>

                  <div class="eval-strip" (click)="evaluateCoach($event, coach.userId)">
                    REPORT_FEEDBACK_DATA
                  </div>
                </div>
              </div>
            </div>
          </div>

          <button class="carousel-btn next" (click)="scrollCarousel(1)">
            <span class="btn-box"> > </span>
          </button>
        </div>

        <div class="lc-empty" *ngIf="coaches.length === 0 && !loading">
          <p>NO_MENTORS_DETECTED_IN_SECTOR.</p>
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

    .coaches-container { max-width: 1400px; margin: 0 auto; padding: 3rem 2rem; position: relative; z-index: 1; }

    .lc-hero { text-align: center; margin-bottom: 4rem; }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 1rem; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 15px var(--neon); }
    .hero-desc { font-size: 1.1rem; color: var(--muted); max-width: 600px; margin: 0 auto; letter-spacing: 0.5px; }

    /* CAROUSEL */
    .carousel-container { position: relative; display: flex; align-items: center; gap: 1.5rem; }
    .carousel-viewport { overflow-x: hidden; scroll-behavior: smooth; width: 100%; mask-image: linear-gradient(to right, transparent, black 10%, black 90%, transparent); }
    .carousel-track { display: flex; gap: 2rem; padding: 2rem 0; width: max-content; }
    
    .coach-card {
      width: 440px; background: var(--card); border: 1px solid var(--border); border-radius: 8px;
      position: relative; overflow: hidden; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      clip-path: polygon(20px 0%, 100% 0%, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0% 100%, 0% 20px);
    }
    .coach-card:hover { transform: translateY(-10px) scale(1.02); border-color: var(--neon2); box-shadow: 0 0 30px rgba(6, 182, 212, 0.2); }
    .card-inner { padding: 2rem; }

    .coach-avatar { margin-bottom: 1.5rem; }
    .avatar-box {
      width: 50px; height: 50px; background: rgba(139, 92, 246, 0.1); border: 1px solid var(--neon);
      color: var(--neon); display: flex; align-items: center; justify-content: center;
      font-family: 'Orbitron', monospace; font-weight: 900; font-size: 20px;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }

    .problem-title { font-family: 'Orbitron', sans-serif; color: var(--text); font-size: 1.5rem; margin: 0 0 0.5rem; }
    .stars { display: flex; gap: 4px; align-items: center; margin-bottom: 1rem; }
    .star { color: #1a1a2e; font-size: 0.9rem; }
    .star.filled { color: var(--neon); }
    .rating-val { color: var(--muted); font-size: 0.75rem; font-family: 'Orbitron', monospace; margin-left: 8px; letter-spacing: 1px; }

    .bio { font-size: 0.95rem; line-height: 1.6; color: #a1a1b5; margin-bottom: 1.5rem; min-height: 4.5rem; }
    
    .tag-list-mini { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 2rem; }
    .tag-mini {
      padding: 4px 10px; background: rgba(6, 182, 212, 0.1); border: 1px solid rgba(6, 182, 212, 0.2);
      color: var(--neon2); font-family: 'Orbitron', monospace; font-size: 9px; font-weight: 700;
      clip-path: polygon(6px 0%, 100% 0%, calc(100% - 6px) 100%, 0% 100%);
    }

    .coach-footer { 
      padding-top: 1.5rem; border-top: 1px solid var(--border);
      display: flex; justify-content: space-between; align-items: center; 
    }
    .stat-group { display: flex; flex-direction: column; }
    .stat-val { font-family: 'Orbitron', monospace; font-size: 1.2rem; color: var(--text); font-weight: 700; }
    .stat-lbl { font-size: 10px; color: var(--muted); letter-spacing: 1px; }

    .action-neon-btn {
      background: var(--neon); color: #fff; border: none; padding: 10px 24px;
      font-family: 'Orbitron', monospace; font-size: 11px; font-weight: 900;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
      cursor: pointer; transition: all 0.3s;
    }
    .action-neon-btn:hover { background: var(--neon2); box-shadow: 0 0 20px var(--neon2); transform: scale(1.05); }

    .eval-strip {
      margin-top: 1.5rem; padding: 8px; background: rgba(16, 185, 129, 0.05);
      border: 1px solid rgba(16, 185, 129, 0.1); color: var(--neon3);
      font-family: 'Orbitron', monospace; font-size: 9px; text-align: center;
      letter-spacing: 2px; cursor: pointer; transition: all 0.2s;
    }
    .eval-strip:hover { background: var(--neon3); color: #000; }

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
export class CoachListComponent implements OnInit {
  @ViewChild('carouselViewport') carouselViewport!: ElementRef;
  coaches: Coach[] = [];
  loading = true;

  constructor(private coachingService: CoachingService, private router: Router) { }

  ngOnInit() {
    this.coachingService.getAllCoaches().subscribe({
      next: (data) => {
        this.coaches = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  scrollCarousel(direction: number) {
    const scrollAmount = 460 * direction;
    this.carouselViewport.nativeElement.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  }

  getStars(rating: number): boolean[] {
    const stars = [];
    for (let i = 1; i <= 5; i++) {
      stars.push(i <= Math.round(rating));
    }
    return stars;
  }

  viewSessions(event: Event, coachUserId: string) {
    event.stopPropagation();
    this.router.navigate(['/coaching-quiz/coaches', coachUserId, 'sessions']);
  }

  evaluateCoach(event: Event, coachUserId: string) {
    event.stopPropagation();
    this.router.navigate(['/coaching-quiz/evaluate', coachUserId]);
  }
}

