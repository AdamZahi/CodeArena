import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { RouterLink } from '@angular/router';
import { QuizService } from '../../services/quiz.service';
import { Quiz } from '../../models/quiz.model';

@Component({
  selector: 'app-quiz-list',
  standalone: true,
  imports: [CommonModule, RouterLink, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="quiz-list-container">
        <div class="lc-hero">
          <h1 class="glitch-title">CHALLENGE_<span>MATRIX</span></h1>
          <p class="hero-desc">Achieve mastery through neural calibration. Break the encryption of logic.</p>
        </div>

        <div class="carousel-container" *ngIf="!loading && quizzes.length > 0">
          <button class="carousel-btn prev" (click)="scrollCarousel(-1)">
             <span class="btn-box"> < </span>
          </button>
          
          <div class="carousel-viewport" #carouselViewport>
            <div class="carousel-track">
              <div class="quiz-card" *ngFor="let quiz of quizzes">
                <div class="card-inner">
                  <div class="card-header">
                    <span class="diff-pill" [class]="quiz.difficulty.toLowerCase()">
                      {{ quiz.difficulty }}
                    </span>
                    <div class="lang-text">
                      <span class="lang-icon">{{ getLanguageIcon(quiz.language) }}</span>
                      <span>{{ quiz.language }}</span>
                    </div>
                  </div>
                  
                  <h3 class="problem-title">{{ quiz.title }}</h3>
                  <p class="card-desc">{{ quiz.description }}</p>
                  
                  <div class="pill-cloud">
                    <span class="topic-neon-pill">{{ quiz.category }}</span>
                  </div>

                  <div class="card-meta">
                    <div class="meta-item">
                      <span class="meta-val">{{ quiz.totalPoints }}</span>
                      <span class="meta-lbl">MAX_SCORE</span>
                    </div>
                  </div>
                  
                  <button class="action-neon-btn" [routerLink]="['/coaching-quiz/quizzes', quiz.id]">
                    EXECUTE_CORE
                  </button>
                </div>
              </div>
            </div>
          </div>

          <button class="carousel-btn next" (click)="scrollCarousel(1)">
            <span class="btn-box"> > </span>
          </button>
        </div>

        <div class="lc-empty" *ngIf="quizzes.length === 0 && !loading">
          <p>NO_DATA_STREAMS_AVAILABLE.</p>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>INITIALIZING_SIMULATION...</p>
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

    .quiz-list-container { max-width: 1400px; margin: 0 auto; padding: 3rem 2rem; position: relative; z-index: 1; }
    
    .lc-hero { text-align: center; margin-bottom: 5rem; }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3.5rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 1.5rem; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 15px var(--neon); }
    .hero-desc { font-size: 1.1rem; color: var(--muted); max-width: 700px; margin: 0 auto; line-height: 1.6; letter-spacing: 1px; }

    /* CAROUSEL */
    .carousel-container { position: relative; display: flex; align-items: center; gap: 1.5rem; }
    .carousel-viewport { overflow-x: hidden; scroll-behavior: smooth; width: 100%; mask-image: linear-gradient(to right, transparent, black 10%, black 90%, transparent); }
    .carousel-track { display: flex; gap: 2rem; padding: 2rem 0; width: max-content; }
    
    .quiz-card {
      width: 380px; background: var(--card); border: 1px solid var(--border); border-radius: 8px;
      position: relative; overflow: hidden; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      clip-path: polygon(20px 0%, 100% 0%, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0% 100%, 0% 20px);
    }
    .quiz-card:hover { transform: translateY(-10px) scale(1.02); border-color: var(--neon2); box-shadow: 0 0 30px rgba(6, 182, 212, 0.2); }
    .card-inner { padding: 2.5rem; }

    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
    .diff-pill { font-family: 'Orbitron', monospace; font-size: 10px; font-weight: 700; letter-spacing: 1px; padding: 4px 12px; background: rgba(255,255,255,0.05); }
    .diff-pill.easy { color: var(--neon3); border-left: 2px solid var(--neon3); }
    .diff-pill.medium { color: #f59e0b; border-left: 2px solid #f59e0b; }
    .diff-pill.hard { color: #ef4444; border-left: 2px solid #ef4444; }

    .lang-text { font-family: 'Orbitron', monospace; font-size: 11px; color: var(--neon2); letter-spacing: 1px; text-transform: uppercase; }
    
    .problem-title { font-family: 'Orbitron', sans-serif; font-size: 1.6rem; color: var(--text); margin: 0 0 1rem; font-weight: 900; letter-spacing: -0.5px; }
    .card-desc { color: #888; font-size: 0.95rem; line-height: 1.6; margin-bottom: 2rem; flex-grow: 1; min-height: 4.8rem; }
    
    .pill-cloud { margin-bottom: 2rem; }
    .topic-neon-pill {
      padding: 5px 14px; background: rgba(139, 92, 246, 0.1); border: 1px solid rgba(139, 92, 246, 0.3);
      border-radius: 100px; color: var(--neon); font-size: 11px; font-weight: 600; font-family: 'Orbitron', sans-serif;
    }

    .card-meta { border-top: 1px solid var(--border); padding-top: 1.5rem; margin-bottom: 1.5rem; }
    .meta-item { display: flex; flex-direction: column; }
    .meta-val { font-family: 'Orbitron', monospace; font-size: 1.2rem; color: var(--text); font-weight: 700; }
    .meta-lbl { font-size: 10px; color: var(--muted); letter-spacing: 1.5px; }
    
    .action-neon-btn {
      width: 100%; background: var(--neon); color: #fff; border: none; padding: 12px;
      font-family: 'Orbitron', monospace; font-size: 11px; font-weight: 900;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
      cursor: pointer; transition: all 0.3s;
    }
    .action-neon-btn:hover { background: var(--neon2); box-shadow: 0 0 20px var(--neon2); }

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
export class QuizListComponent implements OnInit {
  @ViewChild('carouselViewport') carouselViewport!: ElementRef;
  quizzes: Quiz[] = [];
  loading = true;

  constructor(private quizService: QuizService) { }

  ngOnInit() {
    this.quizService.getAllQuizzes().subscribe({
      next: (data) => { this.quizzes = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  scrollCarousel(direction: number) {
    const scrollAmount = 400 * direction;
    this.carouselViewport.nativeElement.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  }

  getLanguageIcon(lang: string): string {
    const icons: Record<string, string> = {
      JAVA: '☕', PYTHON: '🐍', JAVASCRIPT: '⚡', ANGULAR: '🅰️',
      CSS: '🎨', DOTNET: '🔷', MULTI: '🌐'
    };
    return icons[lang] || '💻';
  }
}

