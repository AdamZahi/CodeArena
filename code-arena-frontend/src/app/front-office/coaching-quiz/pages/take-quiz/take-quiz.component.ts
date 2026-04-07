import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { QuizService } from '../../services/quiz.service';
import { Quiz, Question, QuizResult, SubmitQuizRequest } from '../../models/quiz.model';

@Component({
  selector: 'app-take-quiz',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="lc-container">
      <!-- HUD / TOP NAV -->
      <div class="mission-hud" *ngIf="quiz && !result">
         <div class="hud-left">
           <span class="mission-tag">MISSION_ACTIVE</span>
           <h1 class="quiz-title">{{ quiz.title }}</h1>
         </div>
         <div class="hud-right">
           <div class="timer-box">
             <span class="lbl">SYNC_STATUS</span>
             <span class="val">{{ currentIndex + 1 }} / {{ quiz.questions?.length || 0 }}</span>
           </div>
         </div>
         <div class="progress-container">
           <div class="progress-bar-fill" [style.width.%]="getProgress()"></div>
           <div class="progress-glitch"></div>
         </div>
      </div>

      <!-- QUIZ IN PROGRESS -->
      <div class="quiz-main-view" *ngIf="quiz && !result">
        <div class="question-wrapper" *ngIf="currentQuestion">
          <div class="meta-strip">
            <span class="type-badge" [class]="'type-' + currentQuestion.type.toLowerCase()">
              {{ getTypeLabel(currentQuestion.type) }}
            </span>
            <span class="lang-badge">{{ getLanguageIcon(currentQuestion.language) }} {{ currentQuestion.language }}</span>
            <span class="diff-badge" [class]="'diff-' + currentQuestion.difficulty.toLowerCase()">
              {{ currentQuestion.difficulty }}
            </span>
            <span class="pts-badge">+{{ currentQuestion.points }} ENERGY_UNITS</span>
          </div>

          <div class="terminal-content">
            <h2 class="question-text">{{ currentQuestion.content }}</h2>
            
            <div class="code-execution-block" *ngIf="currentQuestion.codeSnippet">
              <div class="block-header">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                <span class="file-name">challenge_source.{{ currentQuestion.language.toLowerCase() }}</span>
              </div>
              <pre class="code-pre"><code>{{ currentQuestion.codeSnippet }}</code></pre>
            </div>

            <!-- MCQ / CODE_ANALYSIS Options -->
            <div class="options-module" *ngIf="currentQuestion.options && currentQuestion.type !== 'CODE_COMPLETION'">
               <div class="option-grid">
                  <button
                    *ngFor="let option of getOptions(currentQuestion); let i = index"
                    class="option-module-btn"
                    [class.selected]="userAnswers[currentQuestion.id] === option"
                    (click)="selectAnswer(currentQuestion.id, option)">
                    <span class="opt-index">0{{ i + 1 }}</span>
                    <span class="opt-text">{{ option }}</span>
                  </button>
               </div>
            </div>

            <!-- CODE_COMPLETION text input -->
            <div class="input-module" *ngIf="currentQuestion.type === 'CODE_COMPLETION'">
              <div class="cyber-input-group">
                <label>INPUT_REQUIRED:</label>
                <div class="input-glow-wrapper">
                  <input
                    type="text"
                    class="cyber-field"
                    placeholder="Provide missing code segment..."
                    [ngModel]="userAnswers[currentQuestion.id] || ''"
                    (ngModelChange)="selectAnswer(currentQuestion.id, $event)" />
                  <div class="input-focus-line"></div>
                </div>
              </div>
            </div>
          </div>

          <div class="control-deck">
            <button class="deck-btn prev" (click)="prevQuestion()" [disabled]="currentIndex === 0">
              [ SKIP_SEQUENCE ]
            </button>
            
            <div class="deck-main">
              <button class="deck-btn next neon-action" (click)="nextQuestion()"
                *ngIf="currentIndex < (quiz.questions?.length || 0) - 1">
                NEXT_DATA_NODE »
              </button>
              <button class="deck-btn submit-final" (click)="submitQuiz()"
                *ngIf="currentIndex === (quiz.questions?.length || 0) - 1"
                [disabled]="submitting">
                {{ submitting ? 'DECRYPTING...' : 'INITIATE_UPLINK' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- QUIZ RESULTS -->
      <div class="results-view" *ngIf="result">
        <div class="result-header" [class]="'level-' + result.level.toLowerCase()">
          <div class="hologram-circle">
            <div class="circle-inner">
               <span class="percent">{{ result.percentage | number:'1.0-0' }}</span>
               <span class="percent-sign">%</span>
            </div>
            <div class="rotating-ring"></div>
          </div>
          <h1 class="neon-title">{{ result.percentage >= 70 ? 'SYNCHRONIZATION_COMPLETE' : 'MISSION_FAILED' }}</h1>
          
          <div class="stats-ribbon">
            <div class="stat-box">
              <span class="val">{{ result.score }}</span>
              <span class="lbl">SCORE</span>
            </div>
            <div class="stat-box">
              <span class="val">{{ result.totalPoints }}</span>
              <span class="lbl">TOTAL_CELLS</span>
            </div>
            <div class="stat-box">
              <span class="rank-chip">{{ result.level }}</span>
              <span class="lbl">STATUS_LEVEL</span>
            </div>
          </div>
        </div>

        <div class="debriefing-section">
          <div class="weakness-module" *ngIf="result.weakTopics.length > 0">
            <h3><span class="warn-icon">!</span> NEURAL_GLITCHES_DETECTED</h3>
            <div class="topic-tags">
              <span class="topic-pill" *ngFor="let topic of result.weakTopics">{{ topic }}</span>
            </div>
            <p class="intel-text">These sectors require manual calibration through prioritized coaching sessions.</p>
          </div>

          <div class="logs-module">
            <h3>CALIBRATION_LOGS</h3>
            <div class="log-entry" *ngFor="let ans of result.answerResults; let i = index"
              [class.pass]="ans.isCorrect" [class.fail]="!ans.isCorrect">
              <div class="log-header">
                <span class="node-id">NODE_{{ i + 1 }}</span>
                <span class="node-status">{{ ans.isCorrect ? 'PASS' : 'CRITICAL_ERROR' }}</span>
                <span class="pts">{{ ans.points }}u</span>
              </div>
              <div class="log-body">
                <div class="data-row"><span class="label">INPUT:</span> {{ ans.userAnswer }}</div>
                <div class="data-row" *ngIf="!ans.isCorrect"><span class="label">EXPECTED:</span> {{ ans.correctAnswer }}</div>
                <div class="explanation-box">REPORT: {{ ans.explanation }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="final-actions">
          <button class="action-btn-neon primary" (click)="goToRecommendations()">
            SEEK_CALIBRATION
          </button>
          <button class="action-btn-neon secondary" (click)="goToQuizList()">
            NEXT_OPERATIONS
          </button>
        </div>
      </div>

      <!-- LOADING -->
      <div class="lc-loading-overlay" *ngIf="loading">
        <div class="loader-box">
           <div class="loader-ring"></div>
           <p>SYNCHRONIZING_PROTOCOLS...</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      --neon: #8b5cf6; --neon2: #06b6d4; --neon3: #10b981;
      --dark: #0a0a0f; --card: #0d0d15; --border: #1a1a2e;
      --text: #e2e8f0; --muted: #64748b;
    }

    @keyframes scan-line { 0% { transform: translateY(-100%); } 100% { transform: translateY(100vh); } }
    @keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
    @keyframes pulse { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }

    .lc-container {
      min-height: 100vh; background: var(--dark); font-family: 'Rajdhani', sans-serif; color: var(--text);
      position: relative; overflow-x: hidden; padding-bottom: 5rem;
    }
    .lc-container::before {
      content: ''; position: fixed; top: 0; left: 0; width: 100%; height: 2px;
      background: linear-gradient(90deg, transparent, var(--neon), transparent);
      animation: scan-line 4s linear infinite; opacity: 0.1; pointer-events: none; z-index: 999;
    }

    /* HUD */
    .mission-hud {
      background: rgba(13, 13, 21, 0.9); border-bottom: 1px solid var(--border);
      padding: 1.5rem 3rem; display: flex; justify-content: space-between; align-items: center;
      position: sticky; top: 0; z-index: 100; backdrop-filter: blur(10px);
    }
    .mission-tag { font-family: 'Orbitron', monospace; font-size: 10px; color: var(--neon); letter-spacing: 2px; background: rgba(139,92,246,0.1); padding: 4px 10px; border: 1px solid var(--neon); margin-bottom: 8px; display: inline-block; }
    .quiz-title { margin: 0; font-family: 'Orbitron', sans-serif; font-size: 1.5rem; letter-spacing: 2px; }
    
    .timer-box { text-align: right; }
    .timer-box .lbl { display: block; font-size: 9px; color: var(--muted); font-family: 'Orbitron', monospace; letter-spacing: 1px; }
    .timer-box .val { font-family: 'Orbitron', monospace; color: var(--neon2); font-size: 1.4rem; font-weight: 700; }

    .progress-container { position: absolute; bottom: -1px; left: 0; width: 100%; height: 2px; background: rgba(255,255,255,0.05); }
    .progress-bar-fill { height: 100%; background: linear-gradient(90deg, var(--neon), var(--neon2)); transition: width 0.4s; position: relative; }

    /* Main Content */
    .quiz-main-view { max-width: 1000px; margin: 3rem auto; padding: 0 2rem; }
    .question-wrapper { background: var(--card); border: 1px solid var(--border); padding: 3rem; position: relative; clip-path: polygon(40px 0, 100% 0, 100% calc(100% - 40px), calc(100% - 40px) 100%, 0 100%, 0 40px); }
    
    .meta-strip { display: flex; gap: 1rem; margin-bottom: 2.5rem; }
    .meta-strip span { font-family: 'Orbitron', monospace; font-size: 10px; padding: 5px 12px; border: 1px solid var(--border); font-weight: 700; letter-spacing: 1px; }
    .type-mcq { color: var(--neon2); border-color: var(--neon2); }
    .diff-easy { color: var(--neon3); }
    .diff-hard { color: #f43f5e; border-color: #f43f5e; }
    .pts-badge { background: rgba(139,92,246,0.05); color: var(--neon); }

    .question-text { font-family: 'Rajdhani', sans-serif; font-size: 1.8rem; line-height: 1.4; color: var(--text); margin-bottom: 2rem; font-weight: 500; }

    .code-execution-block { background: #000; border: 1px solid var(--border); margin-bottom: 2.5rem; }
    .block-header { background: #111; padding: 8px 15px; display: flex; align-items: center; border-bottom: 1px solid var(--border); }
    .block-header .dot { width: 8px; height: 8px; border-radius: 50%; background: #333; margin-right: 6px; }
    .file-name { color: var(--muted); font-size: 11px; margin-left: 10px; font-family: monospace; }
    .code-pre { padding: 1.5rem; margin: 0; color: #a1a1aa; font-family: 'Fira Code', 'Consolas', monospace; font-size: 0.95rem; line-height: 1.6; }

    /* Options */
    .option-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin-bottom: 3rem; }
    .option-module-btn { 
      background: rgba(255,255,255,0.02); border: 1px solid var(--border); padding: 1.5rem; 
      color: var(--text); text-align: left; cursor: pointer; transition: all 0.3s;
      position: relative; overflow: hidden; display: flex; gap: 15px; align-items: center;
    }
    .option-module-btn:hover { border-color: var(--neon2); background: rgba(6,182,212,0.05); }
    .option-module-btn.selected { border-color: var(--neon); background: rgba(139,92,246,0.08); box-shadow: 0 0 20px rgba(139,92,246,0.1); }
    .opt-index { font-family: 'Orbitron', monospace; font-size: 12px; color: var(--muted); opacity: 0.5; }
    .option-module-btn.selected .opt-index { color: var(--neon); opacity: 1; }

    /* Input */
    .cyber-input-group label { display: block; font-family: 'Orbitron', monospace; font-size: 11px; color: var(--neon2); margin-bottom: 15px; letter-spacing: 2px; }
    .input-glow-wrapper { position: relative; }
    .cyber-field { width: 100%; background: transparent; border: none; border-bottom: 2px solid var(--border); padding: 15px 0; color: var(--text); font-family: 'Fira Code', monospace; font-size: 1.2rem; transition: all 0.3s; }
    .cyber-field:focus { outline: none; border-bottom-color: var(--neon); }
    .input-focus-line { position: absolute; bottom: 0; left: 0; width: 0; height: 2px; background: var(--neon); transition: width 0.3s; }
    .cyber-field:focus + .input-focus-line { width: 100%; }

    /* Deck Controls */
    .control-deck { border-top: 1px solid var(--border); padding-top: 2.5rem; display: flex; justify-content: space-between; }
    .deck-btn { 
      background: transparent; color: var(--muted); border: 1px solid var(--border); padding: 12px 24px; 
      font-family: 'Orbitron', monospace; font-size: 11px; font-weight: 700; cursor: pointer; transition: all 0.3s;
      letter-spacing: 2px;
    }
    .deck-btn:hover:not(:disabled) { border-color: var(--text); color: var(--text); }
    .deck-btn:disabled { opacity: 0.2; cursor: not-allowed; }
    .neon-action { background: rgba(6,182,212,0.1); color: var(--neon2); border-color: var(--neon2); }
    .neon-action:hover { background: var(--neon2); color: #000; box-shadow: 0 0 20px var(--neon2); }
    .submit-final { background: var(--neon); color: #fff; border-color: var(--neon); padding: 12px 35px; }
    .submit-final:hover:not(:disabled) { background: #fff; color: #000; box-shadow: 0 0 30px #fff; }

    /* Results */
    .results-view { max-width: 900px; margin: 4rem auto; padding: 0 2rem; }
    .result-header { text-align: center; margin-bottom: 5rem; }
    .hologram-circle { position: relative; width: 180px; height: 180px; margin: 0 auto 3rem; }
    .circle-inner { width: 100%; height: 100%; background: rgba(255,255,255,0.03); border-radius: 50%; border: 2px solid var(--border); display: flex; flex-direction: column; align-items: center; justify-content: center; font-family: 'Orbitron', sans-serif; position: relative; z-index: 2; }
    .circle-inner .percent { font-size: 4rem; font-weight: 900; color: #fff; line-height: 1; }
    .circle-inner .percent-sign { font-size: 1.2rem; color: var(--neon2); }
    .rotating-ring { position: absolute; top: -10px; left: -10px; right: -10px; bottom: -10px; border: 2px dashed var(--neon2); border-radius: 50%; opacity: 0.3; animation: rotate 10s linear infinite; }

    .neon-title { font-family: 'Orbitron', sans-serif; font-size: 2.2rem; font-weight: 900; letter-spacing: 4px; color: var(--text); margin-bottom: 3rem; }
    .level-basique .circle-inner { border-color: var(--neon3); }
    .level-avance .circle-inner { border-color: #f43f5e; box-shadow: 0 0 40px rgba(244,63,94,0.1); }

    .stats-ribbon { display: flex; justify-content: center; gap: 4rem; background: var(--card); border: 1px solid var(--border); padding: 2rem; clip-path: polygon(20px 0, 100% 0, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0 100%, 0 20px); }
    .stat-box .val { display: block; font-size: 2rem; font-weight: 900; font-family: 'Orbitron', sans-serif; color: var(--text); }
    .stat-box .lbl { font-size: 9px; color: var(--muted); font-family: 'Orbitron', monospace; letter-spacing: 2px; }
    .rank-chip { color: var(--neon); font-size: 1.5rem; font-weight: 900; display: block; font-family: 'Orbitron', sans-serif; }

    .debriefing-section { display: grid; grid-template-columns: 1fr 1fr; gap: 3rem; margin-bottom: 4rem; }
    .debriefing-section h3 { font-family: 'Orbitron', sans-serif; font-size: 0.9rem; border-bottom: 1px solid var(--border); padding-bottom: 1rem; margin-bottom: 2rem; color: var(--muted); letter-spacing: 2px; }
    
    .weakness-module { background: rgba(244,63,94,0.03); border: 1px solid rgba(244,63,94,0.1); padding: 2rem; position: relative; }
    .topic-pill { background: rgba(244,63,94,0.1); color: #f43f5e; border: 1px solid #f43f5e; padding: 4px 10px; font-family: 'Orbitron', monospace; font-size: 10px; margin: 0 8px 8px 0; display: inline-block; }

    .log-entry { background: var(--card); border: 1px solid var(--border); margin-bottom: 1.5rem; }
    .log-header { background: #111; padding: 10px 15px; display: flex; justify-content: space-between; font-family: 'Orbitron', monospace; font-size: 10px; }
    .pass .node-status { color: var(--neon3); }
    .fail .node-status { color: #f43f5e; }
    .log-body { padding: 1.5rem; }
    .data-row { font-family: monospace; font-size: 0.85rem; margin-bottom: 5px; color: var(--muted); }
    .data-row .label { color: var(--text); margin-right: 10px; width: 80px; display: inline-block; }
    .explanation-box { margin-top: 15px; padding-top: 15px; border-top: 1px dashed var(--border); font-size: 0.8rem; font-style: italic; color: var(--muted); }

    .final-actions { display: flex; gap: 2rem; justify-content: flex-end; }
    .action-btn-neon { padding: 15px 35px; font-family: 'Orbitron', monospace; font-size: 12px; font-weight: 900; border: none; cursor: pointer; transition: all 0.3s; letter-spacing: 2px; }
    .primary { background: var(--neon); color: #fff; clip-path: polygon(15% 0, 100% 0, 100% 85%, 85% 100%, 0 100%, 0 15%); }
    .primary:hover { transform: scale(1.05); box-shadow: 0 0 25px var(--neon); }
    .secondary { background: transparent; color: var(--muted); border: 1px solid var(--border); }
    
    .lc-loading-overlay { position: fixed; inset: 0; background: var(--dark); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .loader-box { text-align: center; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 3px; color: var(--neon); }
    .loader-ring { width: 60px; height: 60px; border: 2px solid var(--border); border-top-color: var(--neon); border-radius: 50%; margin: 0 auto 2rem; animation: rotate 1s linear infinite; }
  `]
})
export class TakeQuizComponent implements OnInit {
  quiz: Quiz | null = null;
  currentIndex = 0;
  userAnswers: Record<string, string> = {};
  result: QuizResult | null = null;
  loading = true;
  submitting = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private quizService: QuizService
  ) { }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.quizService.getQuizById(id).subscribe({
        next: (quiz) => { this.quiz = quiz; this.loading = false; },
        error: () => { this.loading = false; }
      });
    }
  }

  get currentQuestion(): Question | null {
    return this.quiz?.questions?.[this.currentIndex] || null;
  }

  getProgress(): number {
    if (!this.quiz?.questions?.length) return 0;
    return ((this.currentIndex + 1) / this.quiz.questions.length) * 100;
  }

  getOptions(q: Question): string[] {
    return q.options ? q.options.split(',').map(o => o.trim()) : [];
  }

  selectAnswer(questionId: string, answer: string) {
    this.userAnswers[questionId] = answer;
  }

  nextQuestion() {
    if (this.quiz?.questions && this.currentIndex < this.quiz.questions.length - 1) {
      this.currentIndex++;
    }
  }

  prevQuestion() {
    if (this.currentIndex > 0) this.currentIndex--;
  }

  submitQuiz() {
    if (!this.quiz) return;
    const request: SubmitQuizRequest = {
      quizId: this.quiz.id,
      answers: this.userAnswers
    };
    this.submitting = true;
    this.quizService.submitQuiz(request).subscribe({
      next: (res) => { this.result = res; this.submitting = false; },
      error: () => { this.submitting = false; alert('Erreur lors de la soumission'); }
    });
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      MCQ: 'QCM', CODE_COMPLETION: 'Code à compléter', CODE_ANALYSIS: 'Analyse de code'
    };
    return labels[type] || type;
  }

  getLanguageIcon(lang: string): string {
    const icons: Record<string, string> = {
      JAVA: '☕', PYTHON: '🐍', JAVASCRIPT: '⚡', ANGULAR: '🅰️',
      CSS: '🎨', DOTNET: '🔷', MULTI: '🌐'
    };
    return icons[lang] || '💻';
  }

  goToRecommendations() { this.router.navigate(['/coaching-quiz/sessions']); }
  goToQuizList() { this.router.navigate(['/coaching-quiz/quizzes']); }
}
