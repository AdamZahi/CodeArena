import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { CoachingService } from '../../services/coaching.service';
import { AlertService } from '../../services/alert.service';
import { Dashboard, CoachingSession } from '../../models/coaching-session.model';

@Component({
  selector: 'app-book-session',
  standalone: true,
  imports: [CommonModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="dashboard-container">
        
        <div class="lc-hero">
          <h1 class="glitch-title">NEURAL_<span>COMMAND_CENTER</span></h1>
          <p class="hero-desc">Access your cognitive profile. Synchronize your neural network with peak performance kata and elite mentors to reach grandmaster status.</p>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>SCANNING_COGNITIVE_SECTORS...</p>
        </div>

        <ng-container *ngIf="!loading && dashboard">
          <!-- AI NEURAL ADVISOR panel -->
          <div class="ai-advisor-panel">
            <div class="advisor-header">
              <div class="advisor-icon-box">
                <div class="brain-icon">🧠</div>
                <div class="icon-glow"></div>
              </div>
              <div class="advisor-title">
                <h2>AI_NEURAL_ADVISOR</h2>
                <div class="status-indicator">
                  <span class="pulse-dot"></span>
                  <span class="active-badge">ANALYZING_COGNITIVE_PATTERNS...</span>
                </div>
              </div>
            </div>
            
            <div class="advisor-content">
              <!-- VIZ (LEFT) -->
              <div class="neural-viz">
                <div class="radar-shell">
                  <svg viewBox="0 0 400 400" class="skill-radar">
                    <circle cx="200" cy="200" r="150" class="radar-line" />
                    <circle cx="200" cy="200" r="100" class="radar-line" />
                    <circle cx="200" cy="200" r="50" class="radar-line" />
                    <line x1="200" y1="50" x2="200" y2="350" class="axis-line" />
                    <line x1="50" y1="200" x2="350" y2="200" class="axis-line" />
                    <polygon [attr.points]="getSkillPoints()" class="skill-polygon" />
                    <g *ngFor="let skill of dashboard.skills; let i = index">
                      <circle [attr.cx]="getNodeX(i)" [attr.cy]="getNodeY(i)" r="5" class="node-dot" />
                      <text [attr.x]="getNodeX(i, 25)" [attr.y]="getNodeY(i, 10)" class="node-label">
                        {{ skill.language.toUpperCase() }}
                      </text>
                    </g>
                  </svg>
                </div>
              </div>

              <!-- INSIGHTS (RIGHT) -->
              <div class="neural-insights">
                <div class="insight-box">
                  <div class="insight-item strength" *ngIf="getStrengths()">
                     <span class="lbl tag-stre">STRENGTH_PROFILES</span>
                     <p>{{ getStrengths() }}</p>
                  </div>
                  <div class="insight-item weakness" *ngIf="getWeaknesses()">
                     <span class="lbl tag-weak">VULNERABILITY_LOGS</span>
                     <p>{{ getWeaknesses() }}</p>
                  </div>
                  <div class="insight-item advisor-prediction">
                     <span class="lbl tag-ai">PREDICTIVE_SYNC_PATH</span>
                     <p class="prediction-text">
                       "Based on your current trajectory, you are 3 sessions away from reaching <b>{{ getNextRank() }}</b> status. Focus on 
                       <span>{{ dashboard.skills.length > 0 ? dashboard.skills[0].language : 'SECURITY' }} & PROFILING</span> to balance your profile."
                     </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- REFINED STATS SECTION -->
          <div class="stats-row">
            <div class="stat-cyber-item" *ngFor="let item of [
              {icon:'🧬', val: dashboard.totalQuizzesTaken, lbl: 'KATA_SYNCED', color: 'purple'},
              {icon:'📈', val: (dashboard.averageScore | number:'1.1-1') + '%', lbl: 'SYNC_ACCURACY', color: 'blue'},
              {icon:'🎖️', val: dashboard.overallLevel, lbl: 'OPERATOR_RANK', color: 'yellow', glow: true},
              {icon:'🔔', val: dashboard.unreadNotifications, lbl: 'SYSTEM_ALERTS', color: 'red'}
            ]">
              <div class="stat-icon-box" [class]="item.color">{{ item.icon }}</div>
              <div class="stat-info">
                <h3 [class.rank-glow]="item.glow">{{ item.val }}</h3>
                <p>{{ item.lbl }}</p>
              </div>
            </div>
          </div>

          <!-- LANGUAGES SECTION - CAROUSEL STYLE -->
          <div class="dashboard-section">
            <h2 class="section-glitch-title">YOUR_LANGUAGE_<span>NETWORK</span></h2>
            <div class="carousel-module" *ngIf="dashboard.skills.length > 0">
              <button class="nav-btn prev" (click)="scrollLangCarousel(-1)">❮</button>
              
              <div class="carousel-viewport" #langCarouselViewport>
                <div class="carousel-track">
                  <div class="coach-style-card" *ngFor="let skill of dashboard.skills">
                    <div class="card-inner">
                      <div class="coach-avatar">
                         <div class="avatar-box">{{ skill.language.substring(0,1).toUpperCase() }}</div>
                      </div>
                      <div class="coach-header">
                        <h3 class="problem-title">{{ skill.language }}</h3>
                        <div class="stars">
                          <span class="star filled">★</span><span class="star filled">★</span><span class="star filled">★</span>
                          <span class="star" [class.filled]="skill.scoreAverage > 60">★</span><span class="star" [class.filled]="skill.scoreAverage > 80">★</span>
                          <span class="rating-val">LVL {{ skill.level }}</span>
                        </div>
                      </div>
                      <p class="bio">Proficiency synchronized at {{ skill.scoreAverage | number:'1.0-0' }}%. Neural link shows high compatibility with {{ skill.level }} requirements.</p>
                      
                      <div class="tag-list-mini">
                        <span class="tag-mini">STABLE</span>
                        <span class="tag-mini">{{ skill.level }}</span>
                      </div>

                      <div class="coach-footer">
                        <div class="stat-group">
                          <span class="stat-val">{{ skill.scoreAverage | number:'1.0-0' }}%</span>
                          <span class="stat-lbl">EFFICIENCY_SYNC</span>
                        </div>
                        <div class="action-placeholder"></div>
                      </div>
                      <div class="eval-strip">DATA_NODE_SECURED</div>
                    </div>
                  </div>
                </div>
              </div>

              <button class="nav-btn next" (click)="scrollLangCarousel(1)">❯</button>
            </div>
          </div>

          <!-- NEXT CHALLENGE SECTION - MENTOR CARD STYLE -->
          <div class="dashboard-section mt-5">
            <div class="section-title-badge">
              <h2 class="section-glitch-title">YOUR_NEXT_<span>CHALLENGE</span></h2>
              <span class="badge ai-match">AI_MATCHED</span>
            </div>
            <p class="section-description">High-priority synchronization slots detected by the Neural Advisor.</p>
            
            <div class="carousel-module" *ngIf="dashboard.recommendedSessions.length > 0">
              <button class="nav-btn prev" (click)="scrollCarousel(-1)">❮</button>
              
              <div class="carousel-viewport" #carouselViewport>
                <div class="carousel-track">
                  <div class="coach-style-card highlight" *ngFor="let session of dashboard.recommendedSessions">
                    <div class="card-inner">
                      <div class="coach-avatar">
                         <div class="avatar-box">{{ session.language.substring(0,1).toUpperCase() }}</div>
                      </div>
                      <div class="coach-header">
                        <h3 class="problem-title">{{ session.title }}</h3>
                        <div class="stars">
                          <span class="star filled">★</span><span class="star filled">★</span><span class="star filled">★</span><span class="star filled">★</span>
                          <span class="star">★</span>
                          <span class="rating-val">{{ session.level }}</span>
                        </div>
                      </div>
                      <p class="bio">{{ session.description }}</p>
                      
                      <div class="tag-list-mini">
                        <span class="tag-mini">{{ session.language }}</span>
                        <span class="tag-mini">{{ session.durationMinutes }}MIN</span>
                      </div>

                      <div class="coach-footer">
                        <div class="stat-group">
                          <span class="stat-val">{{ session.currentParticipants }}/{{ session.maxParticipants }}</span>
                          <span class="stat-lbl">UPTIME_SESSIONS</span>
                        </div>
                        <button class="action-neon-btn purple" 
                          (click)="bookSession(session.id)"
                          [disabled]="session.currentParticipants >= session.maxParticipants">
                          TRAIN_NOW
                        </button>
                      </div>
                      <div class="eval-strip">INITIALIZE_DATA_STREAM</div>
                    </div>
                  </div>
                </div>
              </div>

              <button class="nav-btn next" (click)="scrollCarousel(1)">❯</button>
            </div>
            
            <div class="lc-empty" *ngIf="dashboard.recommendedSessions.length === 0">
              <p>NO_MISSION_DETECTED. COMPLETE_INITIAL_KATA_TO_CALIBRATE.</p>
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
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3.5rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 0.5rem; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 15px var(--neon); }
    .hero-desc { font-size: 1.1rem; color: var(--muted); max-width: 700px; margin: 0 auto; line-height: 1.6; letter-spacing: 0.5px; }

    /* ADVISOR PANEL */
    .ai-advisor-panel { 
      background: var(--card); border: 1px solid var(--border); padding: 3rem; margin-bottom: 5rem;
      clip-path: polygon(40px 0, 100% 0, 100% calc(100% - 40px), calc(100% - 40px) 100%, 0 100%, 0 40px);
      position: relative; overflow: hidden;
    }
    .ai-advisor-panel::after { content: ''; position: absolute; top: 0; right: 0; width: 250px; height: 250px; background: var(--neon); filter: blur(150px); opacity: 0.05; }

    .advisor-header { display: flex; align-items: center; gap: 2rem; margin-bottom: 3rem; }
    .advisor-icon-box { position: relative; }
    .brain-icon { width: 60px; height: 60px; background: #000; border: 2px solid var(--neon); color: var(--neon); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 2rem; z-index: 2; position: relative; }
    .icon-glow { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: var(--neon); filter: blur(20px); opacity: 0.3; animation: pulse 2s infinite; }
    
    .advisor-title h2 { font-family: 'Orbitron', sans-serif; color: #fff; margin: 0 0 8px; font-size: 1.8rem; letter-spacing: 3px; }
    .status-indicator { display: flex; align-items: center; gap: 10px; }
    .pulse-dot { width: 8px; height: 8px; background: var(--neon2); border-radius: 50%; animation: pulse 1s infinite; }
    .active-badge { color: var(--neon2); font-size: 10px; font-family: 'Orbitron', monospace; letter-spacing: 2px; font-weight: 700; }
    
    .advisor-content { display: grid; grid-template-columns: 450px 1fr; gap: 4rem; align-items: center; }
    .skill-radar { width: 100%; height: 400px; overflow: visible; filter: drop-shadow(0 0 10px rgba(139, 92, 246, 0.2)); }
    .radar-line { fill: none; stroke: var(--border); stroke-width: 1; opacity: 0.5; }
    .axis-line { stroke: var(--border); stroke-width: 1; stroke-dasharray: 4; }
    .skill-polygon { fill: rgba(139, 92, 246, 0.1); stroke: var(--neon); stroke-width: 2; filter: drop-shadow(0 0 15px var(--neon)); }
    .node-dot { fill: var(--neon-red); filter: drop-shadow(0 0 5px var(--neon-red)); }
    .node-label { fill: var(--muted); font-size: 9px; font-weight: 700; font-family: 'Orbitron', monospace; letter-spacing: 1px; }

    .insight-box { display: flex; flex-direction: column; gap: 1.5rem; }
    .insight-item { background: rgba(0,0,0,0.4); padding: 1.5rem; border: 1px solid var(--border); border-left: 2px solid var(--border); position: relative; }
    .insight-item.strength { border-left-color: var(--neon3); }
    .insight-item.weakness { border-left-color: var(--neon-red); }
    .insight-item p { margin: 12px 0 0; font-size: 0.95rem; line-height: 1.6; color: var(--text); font-weight: 300; }
    .lbl { font-size: 9px; font-family: 'Orbitron', monospace; font-weight: 900; letter-spacing: 2px; padding: 4px 10px; border: 1px solid transparent; display: inline-block; }
    .tag-stre { color: var(--neon3); border-color: var(--neon3); background: rgba(16, 185, 129, 0.05); }
    .tag-weak { color: var(--neon-red); border-color: var(--neon-red); background: rgba(244, 63, 94, 0.05); }
    .tag-ai { color: var(--neon2); border-color: var(--neon2); background: rgba(6, 182, 212, 0.05); }
    
    .advisor-prediction { border-left-color: var(--neon2); background: linear-gradient(90deg, rgba(6,182,212,0.05), transparent); }
    .prediction-text { color: var(--muted); }
    .prediction-text span { color: var(--neon2); font-weight: 700; }
    .prediction-text b { color: var(--text); font-weight: 700; }

    /* STATS SECTION */
    .stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1.5rem; margin-bottom: 6rem; }
    .stat-cyber-item { background: var(--card); border: 1px solid var(--border); padding: 2rem; display: flex; align-items: center; gap: 1.5rem; transition: all 0.3s; clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); }
    .stat-cyber-item:hover { border-color: var(--neon2); transform: translateY(-5px); }
    .stat-icon-box { width: 55px; height: 55px; background: rgba(255,255,255,0.03); display: flex; align-items: center; justify-content: center; font-size: 1.8rem; border: 1px solid var(--border); }
    .stat-icon-box.purple { color: var(--neon); }
    .stat-icon-box.blue { color: var(--neon2); }
    .stat-icon-box.yellow { color: #ecc94b; }
    .stat-icon-box.red { color: var(--neon-red); }
    
    .stat-info h3 { font-family: 'Orbitron', monospace; font-size: 2rem; font-weight: 900; margin: 0; color: #fff; }
    .stat-info p { margin: 0; font-size: 9px; font-family: 'Orbitron', monospace; color: var(--muted); letter-spacing: 1px; font-weight: 700; }
    .rank-glow { color: var(--neon2) !important; text-shadow: 0 0 15px var(--neon2); }

    /* SECTION TITLES */
    .dashboard-section { margin-bottom: 6rem; }
    .section-glitch-title { font-family: 'Orbitron', sans-serif; font-size: 1.8rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 3rem; text-align: left; }
    .section-glitch-title span { color: var(--neon2); text-shadow: 0 0 10px var(--neon2); }

    /* MENTOR CARD STYLE */
    .cards-carousel { display: flex; flex-wrap: wrap; gap: 2rem; }
    .coach-style-card { 
      width: 440px; background: var(--card); border: 1px solid var(--border); position: relative; overflow: hidden; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      clip-path: polygon(20px 0%, 100% 0%, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0% 100%, 0% 20px);
    }
    .coach-style-card:hover { transform: translateY(-10px) scale(1.02); border-color: var(--neon2); box-shadow: 0 0 30px rgba(6, 182, 212, 0.2); }
    .card-inner { padding: 2.5rem; }

    .coach-avatar { margin-bottom: 2rem; }
    .avatar-box { width: 50px; height: 50px; background: rgba(139, 92, 246, 0.1); border: 1px solid var(--neon); color: var(--neon); display: flex; align-items: center; justify-content: center; font-family: 'Orbitron', monospace; font-weight: 900; font-size: 20px; clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); }
    
    .problem-title { font-family: 'Orbitron', sans-serif; color: var(--text); font-size: 1.6rem; margin: 0 0 0.8rem; }
    .stars { display: flex; gap: 4px; align-items: center; margin-bottom: 1.5rem; }
    .star { color: #1a1a2e; font-size: 0.9rem; }
    .star.filled { color: var(--neon); }
    .rating-val { color: var(--muted); font-size: 0.75rem; font-family: 'Orbitron', monospace; margin-left: 10px; letter-spacing: 1px; font-weight: 700; }

    .bio { font-size: 0.95rem; line-height: 1.6; color: #a1a1b5; margin-bottom: 1.5rem; min-height: 3rem; }
    
    .progress-bar-wrap { height: 2px; background: var(--border); width: 100%; margin: 2rem 0; position: relative; }
    .progress-fill { height: 100%; background: linear-gradient(90deg, var(--neon), var(--neon2)); box-shadow: 0 0 10px var(--neon2); }

    .tag-list-mini { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 2.5rem; }
    .tag-mini { padding: 4px 12px; background: rgba(6, 182, 212, 0.1); border: 1px solid rgba(6, 182, 212, 0.2); color: var(--neon2); font-family: 'Orbitron', monospace; font-size: 9px; font-weight: 700; clip-path: polygon(6px 0%, 100% 0%, calc(100% - 6px) 100%, 0% 100%); letter-spacing: 1px; }

    .coach-footer { padding-top: 1.5rem; border-top: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
    .stat-val { font-family: 'Orbitron', monospace; font-size: 1.3rem; color: var(--text); font-weight: 700; }
    .stat-lbl { font-size: 10px; color: var(--muted); letter-spacing: 1px; display: block; }
    
    .action-neon-btn { 
      background: var(--neon); color: #fff; border: none; padding: 10px 24px; font-family: 'Orbitron', monospace; font-size: 11px; font-weight: 900; 
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); cursor: pointer; transition: all 0.3s; letter-spacing: 1px;
    }
    .action-neon-btn:hover:not(:disabled) { background: var(--neon2); box-shadow: 0 0 20px var(--neon2); transform: scale(1.05); }
    .action-neon-btn.purple { background: var(--neon); }

    .eval-strip { margin-top: 2rem; padding: 10px; background: rgba(16, 185, 129, 0.05); border: 1px solid rgba(16, 185, 129, 0.1); color: var(--neon3); font-family: 'Orbitron', monospace; font-size: 9px; text-align: center; letter-spacing: 2px; font-weight: 900; }

    /* CAROUSEL */
    .section-title-badge { display: flex; align-items: center; gap: 2rem; margin-bottom: 0.5rem; }
    .ai-match { background: var(--neon-red); color: #fff; font-family: 'Orbitron', monospace; font-size: 9px; padding: 4px 12px; letter-spacing: 2px; font-weight: 900; box-shadow: 0 0 10px var(--neon-red); }
    .section-description { color: var(--muted); font-size: 1.1rem; margin-bottom: 4rem; }

    .carousel-module { position: relative; display: flex; align-items: center; gap: 1rem; }
    .carousel-viewport { overflow-x: hidden; scroll-behavior: smooth; width: 100%; padding: 2rem 0; }
    .carousel-track { display: flex; gap: 2.5rem; width: max-content; }
    .nav-btn { width: 50px; height: 50px; border: 1px solid var(--border); background: var(--card); color: var(--muted); cursor: pointer; transition: all 0.3s; z-index: 5; font-family: 'Orbitron', monospace; }
    .nav-btn:hover { border-color: var(--neon); color: var(--neon); box-shadow: 0 0 15px var(--neon); }

    .lc-loading, .lc-empty { text-align: center; padding: 5rem; font-family: 'Orbitron', monospace; color: var(--muted); letter-spacing: 3px; font-weight: 700; }
    .spinner { width: 50px; height: 50px; border: 2px solid var(--border); border-top-color: var(--neon); border-radius: 50%; margin: 0 auto 3rem; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes pulse { 0% { opacity: 0.4; } 50% { opacity: 1; } 100% { opacity: 0.4; } }

    .mt-5 { margin-top: 5rem; }

    @media (max-width: 1200px) {
      .stats-row { grid-template-columns: repeat(2, 1fr); }
      .advisor-content { grid-template-columns: 1fr; gap: 3rem; }
      .coach-style-card { width: 340px; }
    }
  `]
})
export class BookSessionComponent implements OnInit {
  @ViewChild('carouselViewport') carouselViewport!: ElementRef;
  @ViewChild('langCarouselViewport') langCarouselViewport!: ElementRef;
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
        this.alertService.error('Erreur lors du chargement du tableau de bord.', 'DATA_FETCH_ERROR');
      }
    });
  }

  scrollCarousel(direction: number) {
    const scrollAmount = 400 * direction;
    this.carouselViewport.nativeElement.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  }

  scrollLangCarousel(direction: number) {
    const scrollAmount = 400 * direction;
    this.langCarouselViewport.nativeElement.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  }

  bookSession(sessionId: string) {
    this.coachingService.bookSession(sessionId).subscribe({
      next: () => {
        this.alertService.success('Session réservée avec succès !', 'PROTOCOL_COMPLETE');
        this.loadDashboard();
      },
      error: (err) => {
        this.alertService.error(err.error?.message || 'Erreur lors de la réservation.', 'SYSTEM_ERROR');
      }
    });
  }

  /* NEURAL HELPERS */
  getSkillPoints(): string {
    if (!this.dashboard || !this.dashboard.skills.length) return '';
    const center = 200;
    const maxVal = 100;
    const maxRadius = 150;
    return this.dashboard.skills.map((skill, i) => {
      const angle = (Math.PI * 2 * i) / this.dashboard!.skills.length - Math.PI / 2;
      const radius = (skill.scoreAverage / maxVal) * maxRadius;
      const x = center + radius * Math.cos(angle);
      const y = center + radius * Math.sin(angle);
      return `${x},${y}`;
    }).join(' ');
  }

  getNodeX(index: number, offset = 0): number {
    const center = 200;
    const angle = (Math.PI * 2 * index) / this.dashboard!.skills.length - Math.PI / 2;
    return center + (150 + offset) * Math.cos(angle);
  }

  getNodeY(index: number, offset = 0): number {
    const center = 200;
    const angle = (Math.PI * 2 * index) / this.dashboard!.skills.length - Math.PI / 2;
    return center + (150 + offset) * Math.sin(angle);
  }

  getStrengths(): string {
    if (!this.dashboard) return '';
    const elite = this.dashboard.skills.filter(s => s.scoreAverage >= 85);
    if (!elite.length) return 'Mastering the fundamentals. Keep pushing to break the 85% barrier.';
    return `Exceptional mastery in ${elite.map(s => s.language).join(' and ')}. Your neural patterns show high efficiency in these domains.`;
  }

  getWeaknesses(): string {
    if (!this.dashboard) return '';
    const low = this.dashboard.skills.filter(s => s.scoreAverage < 60);
    if (!low.length) return 'Balanced profile detected. No critical vulnerabilities found in scanned languages.';
    return `Detected logic gaps in ${low.map(s => s.language).join(', ')}. Scanned sessions indicate a need for focused practice.`;
  }

  getNextRank(): string {
    const ranks = ['SHODAN', 'SENSEI', 'GRANDMASTER', 'LEGEND'];
    const idx = ranks.indexOf(this.dashboard?.overallLevel || 'BASIQUE');
    return idx === -1 ? 'SENSEI' : ranks[Math.min(idx + 1, ranks.length - 1)];
  }
}
