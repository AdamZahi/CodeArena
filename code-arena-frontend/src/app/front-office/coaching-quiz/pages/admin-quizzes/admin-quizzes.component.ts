import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QuizService } from '../../services/quiz.service';
import { AlertService } from '../../services/alert.service';
import { Quiz } from '../../models/quiz.model';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-quizzes',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent, RouterLink],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="dashboard-container">
        
        <div class="lc-header-main">
          <div class="header-text">
            <h1 class="glitch-title">QUIZ_<span>MANAGER</span></h1>
            <p class="hero-desc">Monitor and manage all training assessments in the neural network.</p>
          </div>
          <div class="tab-row">
            <button class="tab-btn active">ALL_QUIZZES</button>
            <a routerLink="/coaching-quiz/admin/applications" class="tab-btn">COACH_APPLICATIONS</a>
          </div>
        </div>

        <div class="search-bar-container">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input type="text" [(ngModel)]="searchTerm" (input)="filterQuizzes()" placeholder="Filter by title, language or difficulty..." class="cyber-search" />
          </div>
          <div class="stats-row">
            <div class="stat-item">
              <span class="stat-lbl">TOTAL_NODES:</span>
              <span class="stat-val">{{ quizzes.length }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-lbl">FILTERED:</span>
              <span class="stat-val">{{ filteredQuizzes.length }}</span>
            </div>
          </div>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>SYNCHRONIZING_QUIZ_REPOSITORY...</p>
        </div>

        <ng-container *ngIf="!loading">
          <div class="quiz-grid" *ngIf="filteredQuizzes.length > 0">
            <div class="quiz-card" *ngFor="let quiz of filteredQuizzes" [ngClass]="quiz.difficulty.toLowerCase()">
              <div class="card-edge"></div>
              <div class="card-inner">
                <div class="quiz-meta">
                  <span class="difficulty-tag">{{ quiz.difficulty }}</span>
                  <span class="language-tag">{{ quiz.language }}</span>
                </div>
                
                <h3 class="quiz-title">{{ quiz.title }}</h3>
                <p class="quiz-desc">{{ quiz.description }}</p>

                <div class="quiz-info-row">
                  <div class="info-item">
                    <span class="info-lbl">POINTS</span>
                    <span class="info-val">{{ quiz.totalPoints }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-lbl">CREATED</span>
                    <span class="info-val">{{ quiz.createdAt | date:'shortDate' }}</span>
                  </div>
                </div>

                <div class="card-actions">
                  <button class="action-btn delete" (click)="deleteQuiz(quiz)" [disabled]="deletingId === quiz.id">
                    <span *ngIf="deletingId !== quiz.id">🗑️ DELETE_DATA</span>
                    <span *ngIf="deletingId === quiz.id">DELETING...</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="lc-empty" *ngIf="filteredQuizzes.length === 0">
            <div class="empty-icon">📂</div>
            <p>NO_QUIZZES_FOUND_IN_SELECTED_MATRIX.</p>
          </div>
        </ng-container>

      </div>
    </div>
  `,
  styles: [`
    :host {
      --neon: #8b5cf6;
      --neon2: #06b6d4;
      --neon-red: #f43f5e;
      --dark: #0a0a0f;
      --card: #0d0d15;
      --border: #1a1a2e;
      --text: #e2e8f0;
      --muted: #64748b;
      --easy: #10b981;
      --medium: #f59e0b;
      --hard: #ef4444;
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

    .lc-header-main { margin-bottom: 3rem; padding-bottom: 2rem; border-bottom: 1px solid var(--border); }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 2.5rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 0.5rem; }
    .glitch-title span { color: var(--neon2); text-shadow: 0 0 15px var(--neon2); }
    .hero-desc { font-size: 1rem; color: var(--muted); letter-spacing: 0.5px; margin-bottom: 2rem; }

    .tab-row { display: flex; gap: 1rem; }
    .tab-btn { background: var(--card); border: 1px solid var(--border); color: var(--muted); padding: 10px 24px;
      font-family: 'Orbitron', monospace; font-size: 10px; font-weight: 700; letter-spacing: 2px; cursor: pointer; transition: all 0.3s;
      clip-path: polygon(8px 0%, 100% 0%, calc(100% - 8px) 100%, 0% 100%); text-decoration: none; display: inline-block;
    }
    .tab-btn.active { background: var(--neon2); color: #000; border-color: var(--neon2); }
    .tab-btn:hover:not(.active) { border-color: var(--text); color: var(--text); }

    .search-bar-container { margin-bottom: 3rem; display: flex; justify-content: space-between; align-items: center; gap: 2rem; }
    .search-box { flex: 1; position: relative; }
    .search-icon { position: absolute; left: 1.5rem; top: 50%; transform: translateY(-50%); color: var(--neon2); }
    .cyber-search { width: 100%; background: var(--card); border: 1px solid var(--border); padding: 1rem 1rem 1rem 3.5rem;
      color: #fff; font-family: 'Fira Code', monospace; font-size: 0.9rem; outline: none; transition: all 0.3s;
      clip-path: polygon(15px 0%, 100% 0%, 100% calc(100% - 15px), calc(100% - 15px) 100%, 0% 100%, 0% 15px);
    }
    .cyber-search:focus { border-color: var(--neon2); box-shadow: 0 0 15px rgba(6, 182, 212, 0.2); }

    .stats-row { display: flex; gap: 2rem; }
    .stat-lbl { font-size: 9px; color: var(--muted); font-family: 'Orbitron', monospace; letter-spacing: 1.5px; margin-right: 0.5rem; }
    .stat-val { font-size: 1.1rem; color: var(--neon); font-family: 'Fira Code', monospace; font-weight: bold; }

    .quiz-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 2rem; }
    
    .quiz-card {
      background: var(--card); border: 1px solid var(--border); position: relative; transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      clip-path: polygon(30px 0%, 100% 0%, 100% calc(100% - 30px), calc(100% - 30px) 100%, 0% 100%, 0% 30px);
    }
    .quiz-card:hover { transform: translateY(-10px); border-color: var(--neon); box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
    
    .card-edge { position: absolute; top: 0; left: 0; width: 4px; height: 100%; }
    .quiz-card.easy .card-edge { background: var(--easy); }
    .quiz-card.medium .card-edge { background: var(--medium); }
    .quiz-card.hard .card-edge { background: var(--hard); }

    .card-inner { padding: 2rem; }
    
    .quiz-meta { display: flex; gap: 1rem; margin-bottom: 1.5rem; }
    .difficulty-tag { font-size: 9px; font-family: 'Orbitron', monospace; letter-spacing: 1px; padding: 2px 10px; border: 1px solid; border-radius: 4px; }
    .quiz-card.easy .difficulty-tag { color: var(--easy); border-color: rgba(16, 185, 129, 0.3); }
    .quiz-card.medium .difficulty-tag { color: var(--medium); border-color: rgba(245, 158, 11, 0.3); }
    .quiz-card.hard .difficulty-tag { color: var(--hard); border-color: rgba(239, 68, 68, 0.3); }
    
    .language-tag { font-size: 10px; color: var(--neon2); font-family: 'Fira Code', monospace; background: rgba(6, 182, 212, 0.1); padding: 2px 10px; border-radius: 4px; }

    .quiz-title { font-family: 'Orbitron', sans-serif; font-size: 1.25rem; color: #fff; margin-bottom: 1rem; letter-spacing: 1px; }
    .quiz-desc { color: var(--muted); font-size: 0.9rem; line-height: 1.6; margin-bottom: 2rem; height: 3rem; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }

    .quiz-info-row { display: flex; gap: 2rem; margin-bottom: 2rem; padding-top: 1rem; border-top: 1px solid var(--border); }
    .info-item { display: flex; flex-direction: column; gap: 0.3rem; }
    .info-lbl { font-size: 8px; color: var(--muted); font-family: 'Orbitron', monospace; letter-spacing: 1px; }
    .info-val { font-size: 0.9rem; color: #fff; font-family: 'Fira Code', monospace; }

    .card-actions { border-top: 1px solid var(--border); padding-top: 1.5rem; }
    .action-btn { width: 100%; padding: 12px; font-family: 'Orbitron', monospace; font-size: 10px; font-weight: 900; letter-spacing: 1px;
      cursor: pointer; transition: all 0.3s; border: none; clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }
    .action-btn.delete { background: rgba(244, 63, 94, 0.1); color: var(--neon-red); border: 1px solid var(--neon-red); }
    .action-btn.delete:hover:not(:disabled) { background: var(--neon-red); color: #fff; box-shadow: 0 0 15px var(--neon-red); }
    .action-btn:disabled { opacity: 0.5; cursor: not-allowed; }

    .lc-loading, .lc-empty { text-align: center; padding: 10rem 0; color: var(--muted); font-family: 'Orbitron', monospace; letter-spacing: 2px; }
    .spinner { width: 40px; height: 40px; border: 2px solid var(--border); border-top-color: var(--neon2); border-radius: 50%; margin: 0 auto 2rem; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .empty-icon { font-size: 3rem; margin-bottom: 2rem; opacity: 0.3; }
  `]
})
export class AdminQuizzesComponent implements OnInit {
  quizzes: Quiz[] = [];
  filteredQuizzes: Quiz[] = [];
  loading = true;
  searchTerm = '';
  deletingId: string | null = null;

  constructor(private quizService: QuizService, private alertService: AlertService) {}

  ngOnInit() {
    this.loadQuizzes();
  }

  loadQuizzes() {
    this.loading = true;
    this.quizService.getAllQuizzes().subscribe({
      next: (data) => {
        this.quizzes = data;
        this.filterQuizzes();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error fetching quizzes:', err);
        this.loading = false;
        this.alertService.error('ACCESS_DENIED: Failed to retrieve quiz matrix.', 'SECURITY_FAILURE');
      }
    });
  }

  filterQuizzes() {
    if (!this.searchTerm.trim()) {
      this.filteredQuizzes = [...this.quizzes];
      return;
    }
    const term = this.searchTerm.toLowerCase();
    this.filteredQuizzes = this.quizzes.filter(q => 
      q.title.toLowerCase().includes(term) || 
      q.language.toLowerCase().includes(term) || 
      q.difficulty.toLowerCase().includes(term)
    );
  }

  async deleteQuiz(quiz: Quiz) {
    const confirmed = await this.alertService.showConfirm(
      'DELETION_PROTOCOL',
      `CAUTION: Are you sure you want to PERMANENTLY DELETE current quiz nodes for "${quiz.title}"? This action cannot be undone.`
    );

    if (!confirmed) return;

    this.deletingId = quiz.id;
    this.quizService.deleteQuiz(quiz.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.quizzes = this.quizzes.filter(q => q.id !== quiz.id);
        this.filterQuizzes();
        this.alertService.success(`SUCCESS: Quiz nodes for "${quiz.title}" have been wiped from the mainframe.`);
      },
      error: (err) => {
        this.deletingId = null;
        console.error('Error deleting quiz:', err);
        this.alertService.error('ERROR: Failed to delete quiz. Integrity check failed.', 'DELETION_ERROR');
      }
    });
  }
}
