import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { AiService, AiRequest } from '../../services/ai.service';
import { QuizService } from '../../services/quiz.service';
import { Quiz } from '../../models/quiz.model';
import { marked } from 'marked';

interface ChatMessage {
  role: 'user' | 'ai';
  content: string;
  timestamp: Date;
  isTyping?: boolean;
}

@Component({
  selector: 'app-coach-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="ai-universe">
      <!-- Animated background particles -->
      <div class="particles">
        <div class="particle" *ngFor="let p of particles" [style.left.%]="p.x" [style.top.%]="p.y"
             [style.animationDelay.s]="p.delay" [style.width.px]="p.size" [style.height.px]="p.size"></div>
      </div>

      <div class="ai-container">
        <!-- PAGE HEADER -->
        <div class="ai-hero">
          <div class="ai-logo-ring">
            <div class="ring ring-1"></div>
            <div class="ring ring-2"></div>
            <div class="ring ring-3"></div>
            <div class="ai-core">🧠</div>
          </div>
          <h1 class="ai-title">
            <span class="prefix">AI</span> TRAINING_<span class="accent">ARCHITECT</span>
          </h1>
          <p class="ai-subtitle">
            Powered by neural intelligence — Design sessions, forge quizzes, and unlock coaching mastery
          </p>
        </div>

        <!-- MODE SELECTOR -->
        <div class="mode-selector">
          <button class="mode-btn" [class.active]="activeMode === 'SESSION_PLAN'"
                  (click)="setMode('SESSION_PLAN')">
            <span class="mode-icon">🏗️</span>
            <span class="mode-label">SESSION BLUEPRINT</span>
            <span class="mode-desc">Generate complete session plans</span>
          </button>
          <button class="mode-btn" [class.active]="activeMode === 'QUIZ_GENERATE'"
                  (click)="setMode('QUIZ_GENERATE')">
            <span class="mode-icon">⚡</span>
            <span class="mode-label">QUIZ FORGE</span>
            <span class="mode-desc">Auto-generate quiz questions</span>
          </button>
          <button class="mode-btn" [class.active]="activeMode === 'CHAT'"
                  (click)="setMode('CHAT')">
            <span class="mode-icon">💬</span>
            <span class="mode-label">ARIA CHAT</span>
            <span class="mode-desc">AI coaching assistant</span>
          </button>
        </div>

        <!-- SESSION PLAN MODE -->
        <div class="mode-panel" *ngIf="activeMode === 'SESSION_PLAN'">
          <div class="panel-header">
            <div class="panel-icon">🏗️</div>
            <div>
              <h2>SESSION BLUEPRINT GENERATOR</h2>
              <p>Generate a complete, structured coaching session plan with exercises, agenda, and learning objectives</p>
            </div>
          </div>

          <div class="config-grid">
            <div class="config-field">
              <label>TOPIC / SUBJECT</label>
              <input type="text" [(ngModel)]="sessionPlan.topic" class="neon-input"
                     placeholder="e.g. Spring Boot REST APIs, React Hooks..." />
            </div>
            <div class="config-field">
              <label>LANGUAGE</label>
              <select [(ngModel)]="sessionPlan.language" class="neon-select">
                <option value="JAVA">Java</option>
                <option value="PYTHON">Python</option>
                <option value="JAVASCRIPT">JavaScript</option>
                <option value="ANGULAR">Angular</option>
                <option value="DOTNET">.NET</option>
              </select>
            </div>
            <div class="config-field">
              <label>LEVEL</label>
              <select [(ngModel)]="sessionPlan.level" class="neon-select">
                <option value="BASIQUE">Basique</option>
                <option value="INTERMEDIAIRE">Intermédiaire</option>
                <option value="AVANCE">Avancé</option>
              </select>
            </div>
            <div class="config-field">
              <label>DURATION (MIN)</label>
              <input type="number" [(ngModel)]="sessionPlan.durationMinutes" class="neon-input"
                     min="15" max="300" />
            </div>
          </div>

          <button class="generate-btn" (click)="generateSessionPlan()"
                  [disabled]="isGenerating || !sessionPlan.topic">
            <span class="btn-glow"></span>
            <span *ngIf="!isGenerating">⚡ GENERATE BLUEPRINT</span>
            <span *ngIf="isGenerating" class="generating">
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              NEURAL PROCESSING...
            </span>
          </button>

          <!-- Result Display -->
          <div class="result-display" *ngIf="generatedContent && activeMode === 'SESSION_PLAN'">
            <div class="result-header">
              <span class="result-badge">✅ BLUEPRINT GENERATED</span>
              <button class="copy-btn" (click)="copyToClipboard(generatedContent)">📋 COPY</button>
            </div>
            <div class="markdown-body" [innerHTML]="renderedMarkdown"></div>
          </div>
        </div>

        <!-- QUIZ FORGE MODE -->
        <div class="mode-panel" *ngIf="activeMode === 'QUIZ_GENERATE'">
          <div class="panel-header">
            <div class="panel-icon">⚡</div>
            <div>
              <h2>QUIZ FORGE</h2>
              <p>Generate quiz questions with answers, explanations, and code snippets — ready for your sessions</p>
            </div>
          </div>

          <div class="config-grid">
            <div class="config-field">
              <label>QUIZ TOPIC</label>
              <input type="text" [(ngModel)]="quizConfig.topic" class="neon-input"
                     placeholder="e.g. OOP Concepts, Array Manipulation..." />
            </div>
            <div class="config-field">
              <label>LANGUAGE</label>
              <select [(ngModel)]="quizConfig.language" class="neon-select">
                <option value="JAVA">Java</option>
                <option value="PYTHON">Python</option>
                <option value="JAVASCRIPT">JavaScript</option>
                <option value="ANGULAR">Angular</option>
                <option value="DOTNET">.NET</option>
              </select>
            </div>
            <div class="config-field">
              <label>DIFFICULTY</label>
              <select [(ngModel)]="quizConfig.level" class="neon-select">
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>
            <div class="config-field">
              <label>QUESTION COUNT</label>
              <input type="number" [(ngModel)]="quizConfig.questionCount" class="neon-input"
                     min="1" max="20" />
            </div>
          </div>

          <button class="generate-btn quiz-gen" (click)="generateQuiz()"
                  [disabled]="isGenerating || !quizConfig.topic">
            <span class="btn-glow"></span>
            <span *ngIf="!isGenerating">⚡ FORGE QUIZ</span>
            <span *ngIf="isGenerating" class="generating">
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              FORGING QUESTIONS...
            </span>
          </button>

          <!-- Quiz Result -->
          <div class="result-display" *ngIf="generatedQuiz && activeMode === 'QUIZ_GENERATE'">
            <div class="result-header">
              <span class="result-badge quiz-badge">⚡ QUIZ FORGED</span>
              <div class="header-actions">
                <button class="copy-btn" (click)="copyToClipboard(generatedContent)">📋 COPY RAW</button>
                <button class="publish-btn" (click)="publishQuiz()" [disabled]="isPublishing || publishSuccess">
                  <span *ngIf="!isPublishing && !publishSuccess">🚀 PUBLISH QUIZ</span>
                  <span *ngIf="isPublishing">WAIT...</span>
                  <span *ngIf="publishSuccess">✅ PUBLISHED</span>
                </button>
              </div>
            </div>

            <div class="quiz-preview" *ngIf="generatedQuiz.quizTitle">
              <h3 class="quiz-title">{{ generatedQuiz.quizTitle }}</h3>
              <p class="quiz-desc">{{ generatedQuiz.quizDescription }}</p>

              <div class="question-card" *ngFor="let q of generatedQuiz.questions; let i = index">
                <div class="q-header">
                  <span class="q-number">Q{{ i + 1 }}</span>
                  <span class="q-type" [class]="q.type?.toLowerCase()">{{ q.type }}</span>
                  <span class="q-points">{{ q.points }} pts</span>
                </div>
                <p class="q-content">{{ q.content }}</p>
                <pre class="q-code" *ngIf="q.codeSnippet">{{ q.codeSnippet }}</pre>
                <div class="q-options" *ngIf="q.options">
                  <div class="q-option" *ngFor="let opt of splitOptions(q.options); let j = index"
                       [class.correct]="opt === q.correctAnswer">
                    <span class="opt-letter">{{ ['A','B','C','D'][j] }}</span>
                    {{ opt }}
                    <span class="correct-indicator" *ngIf="opt === q.correctAnswer">✓</span>
                  </div>
                </div>
                <div class="q-explanation">
                  <strong>💡 Explanation:</strong> {{ q.explanation }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- CHAT MODE -->
        <div class="mode-panel chat-mode" *ngIf="activeMode === 'CHAT'">
          <div class="panel-header">
            <div class="panel-icon pulse">💬</div>
            <div>
              <h2>ARIA — AI COACHING ASSISTANT</h2>
              <p>Ask anything about teaching, session planning, or coding pedagogy</p>
            </div>
          </div>

          <div class="chat-container" id="chatContainer">
            <div class="chat-messages">
              <!-- Welcome message -->
              <div class="chat-msg ai" *ngIf="chatMessages.length === 0">
                <div class="msg-avatar">🤖</div>
                <div class="msg-body">
                  <div class="msg-name">ARIA</div>
                  <div class="msg-text">
                    Hello, Coach! I'm <strong>ARIA</strong> — your AI Coaching Assistant. 🚀<br><br>
                    I can help you with:<br>
                    • 📚 Teaching methodologies & strategies<br>
                    • 🎯 Session planning and improvement<br>
                    • 💻 Code review and explanation techniques<br>
                    • 🧩 Problem-solving pedagogy<br><br>
                    <em>What would you like to discuss?</em>
                  </div>
                </div>
              </div>

              <div class="chat-msg" [class.ai]="msg.role === 'ai'" [class.user]="msg.role === 'user'"
                   *ngFor="let msg of chatMessages">
                <div class="msg-avatar">{{ msg.role === 'ai' ? '🤖' : '👤' }}</div>
                <div class="msg-body">
                  <div class="msg-name">{{ msg.role === 'ai' ? 'ARIA' : 'YOU' }}</div>
                  <div class="msg-text" *ngIf="msg.role === 'user'">{{ msg.content }}</div>
                  <div class="msg-text markdown-body" *ngIf="msg.role === 'ai' && !msg.isTyping"
                       [innerHTML]="renderMarkdown(msg.content)"></div>
                  <div class="msg-text typing-indicator" *ngIf="msg.isTyping">
                    <span class="typing-dot"></span>
                    <span class="typing-dot"></span>
                    <span class="typing-dot"></span>
                  </div>
                  <div class="msg-time">{{ msg.timestamp | date:'HH:mm' }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input-bar">
            <input type="text" [(ngModel)]="chatInput" class="chat-input"
                   placeholder="Ask ARIA anything about coaching..."
                   (keydown.enter)="sendChatMessage()"
                   [disabled]="isGenerating" />
            <button class="send-btn" (click)="sendChatMessage()"
                    [disabled]="isGenerating || !chatInput.trim()">
              <span *ngIf="!isGenerating">▶</span>
              <span *ngIf="isGenerating" class="send-spinner"></span>
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;700;900&family=Rajdhani:wght@300;400;500;600;700&family=Fira+Code:wght@300;400;500&display=swap');

    :host {
      --neon: #8b5cf6;
      --neon2: #06b6d4;
      --neon3: #10b981;
      --neon-pink: #f43f5e;
      --neon-amber: #f59e0b;
      --dark: #050508;
      --dark2: #0a0a12;
      --card: #0c0c18;
      --border: #1a1a30;
      --text: #e2e8f0;
      --muted: #64748b;
    }

    /* ═══════ BACKGROUND UNIVERSE ═══════ */
    .ai-universe {
      min-height: 100vh;
      background: var(--dark);
      position: relative;
      overflow: hidden;
      font-family: 'Rajdhani', sans-serif;
      color: var(--text);
    }

    .ai-universe::before {
      content: '';
      position: fixed;
      top: 0; left: 0;
      width: 100%; height: 100%;
      background:
        radial-gradient(ellipse at 20% 50%, rgba(139, 92, 246, 0.08) 0%, transparent 50%),
        radial-gradient(ellipse at 80% 20%, rgba(6, 182, 212, 0.06) 0%, transparent 50%),
        radial-gradient(ellipse at 50% 80%, rgba(16, 185, 129, 0.04) 0%, transparent 50%);
      pointer-events: none;
      z-index: 0;
    }

    /* Floating particles */
    .particles {
      position: fixed;
      top: 0; left: 0; width: 100%; height: 100%;
      pointer-events: none; z-index: 0;
    }

    .particle {
      position: absolute;
      background: var(--neon);
      border-radius: 50%;
      opacity: 0.15;
      animation: float-particle 15s ease-in-out infinite;
    }

    @keyframes float-particle {
      0%, 100% { transform: translateY(0) translateX(0); opacity: 0.1; }
      25% { transform: translateY(-30px) translateX(20px); opacity: 0.3; }
      50% { transform: translateY(-60px) translateX(-10px); opacity: 0.15; }
      75% { transform: translateY(-30px) translateX(30px); opacity: 0.25; }
    }

    .ai-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 3rem 2rem 6rem;
      position: relative;
      z-index: 1;
    }

    /* ═══════ HERO SECTION ═══════ */
    .ai-hero {
      text-align: center;
      margin-bottom: 4rem;
      position: relative;
    }

    .ai-logo-ring {
      position: relative;
      width: 120px;
      height: 120px;
      margin: 0 auto 2rem;
    }

    .ring {
      position: absolute;
      border: 2px solid transparent;
      border-radius: 50%;
      animation: orbit 6s linear infinite;
    }

    .ring-1 {
      inset: 0;
      border-top-color: var(--neon);
      border-right-color: var(--neon);
      animation-duration: 3s;
    }
    .ring-2 {
      inset: 10px;
      border-bottom-color: var(--neon2);
      border-left-color: var(--neon2);
      animation-duration: 4s;
      animation-direction: reverse;
    }
    .ring-3 {
      inset: 20px;
      border-top-color: var(--neon3);
      animation-duration: 5s;
    }

    @keyframes orbit {
      to { transform: rotate(360deg); }
    }

    .ai-core {
      position: absolute;
      inset: 30px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 2.5rem;
      background: radial-gradient(circle, rgba(139,92,246,0.15), transparent);
      border-radius: 50%;
      animation: pulse-core 2s ease-in-out infinite;
    }

    @keyframes pulse-core {
      0%, 100% { transform: scale(1); filter: brightness(1); }
      50% { transform: scale(1.1); filter: brightness(1.3); }
    }

    .ai-title {
      font-family: 'Orbitron', sans-serif;
      font-size: 2.8rem;
      font-weight: 900;
      letter-spacing: 4px;
      margin-bottom: 1rem;
    }

    .ai-title .prefix {
      color: var(--neon2);
      text-shadow: 0 0 20px rgba(6, 182, 212, 0.5);
    }

    .ai-title .accent {
      color: var(--neon);
      text-shadow: 0 0 20px rgba(139, 92, 246, 0.5);
    }

    .ai-subtitle {
      color: var(--muted);
      font-size: 1.1rem;
      letter-spacing: 1px;
      max-width: 600px;
      margin: 0 auto;
    }

    /* ═══════ MODE SELECTOR ═══════ */
    .mode-selector {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.5rem;
      margin-bottom: 3rem;
    }

    .mode-btn {
      background: var(--card);
      border: 1px solid var(--border);
      padding: 2rem;
      cursor: pointer;
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 0.8rem;
      clip-path: polygon(15px 0, 100% 0, 100% calc(100% - 15px), calc(100% - 15px) 100%, 0 100%, 0 15px);
    }

    .mode-btn::before {
      content: '';
      position: absolute;
      top: 0; left: 0;
      width: 100%; height: 100%;
      background: linear-gradient(135deg, rgba(139,92,246,0.05), transparent);
      opacity: 0;
      transition: opacity 0.4s;
    }

    .mode-btn:hover::before { opacity: 1; }
    .mode-btn:hover { border-color: var(--neon); transform: translateY(-4px); }

    .mode-btn.active {
      border-color: var(--neon);
      background: linear-gradient(135deg, rgba(139,92,246,0.1), rgba(6,182,212,0.05));
      box-shadow: 0 0 30px rgba(139, 92, 246, 0.15), inset 0 0 30px rgba(139, 92, 246, 0.05);
    }

    .mode-btn.active::after {
      content: '';
      position: absolute;
      bottom: 0; left: 0;
      width: 100%; height: 3px;
      background: linear-gradient(90deg, var(--neon), var(--neon2));
    }

    .mode-icon { font-size: 2.5rem; }
    .mode-label {
      font-family: 'Orbitron', sans-serif;
      font-size: 12px;
      font-weight: 700;
      color: var(--text);
      letter-spacing: 2px;
    }
    .mode-desc {
      font-size: 0.8rem;
      color: var(--muted);
      letter-spacing: 0.5px;
    }

    /* ═══════ MODE PANELS ═══════ */
    .mode-panel {
      background: var(--card);
      border: 1px solid var(--border);
      padding: 3rem;
      position: relative;
      overflow: hidden;
      animation: panel-in 0.4s ease-out;
      clip-path: polygon(25px 0, 100% 0, 100% calc(100% - 25px), calc(100% - 25px) 100%, 0 100%, 0 25px);
    }

    @keyframes panel-in {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .mode-panel::before {
      content: '';
      position: absolute;
      top: 0; right: 0;
      width: 400px; height: 400px;
      background: radial-gradient(circle, rgba(139,92,246,0.06), transparent);
      pointer-events: none;
    }

    .panel-header {
      display: flex;
      align-items: center;
      gap: 1.5rem;
      margin-bottom: 3rem;
      padding-bottom: 2rem;
      border-bottom: 1px solid var(--border);
    }

    .panel-icon {
      font-size: 2.5rem;
      width: 70px; height: 70px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(139,92,246,0.08);
      border: 1px solid var(--border);
      border-radius: 12px;
    }

    .panel-icon.pulse { animation: pulse-core 2s ease-in-out infinite; }

    .panel-header h2 {
      font-family: 'Orbitron', sans-serif;
      font-size: 18px;
      color: var(--neon2);
      letter-spacing: 2px;
      margin: 0 0 0.5rem;
    }

    .panel-header p {
      color: var(--muted);
      margin: 0;
      font-size: 0.95rem;
    }

    /* ═══════ CONFIG GRID ═══════ */
    .config-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 2rem;
      margin-bottom: 2.5rem;
    }

    .config-field { display: flex; flex-direction: column; gap: 0.7rem; }

    .config-field label {
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      color: var(--muted);
      letter-spacing: 2px;
      font-weight: 700;
    }

    .neon-input, .neon-select {
      background: rgba(0,0,0,0.5);
      border: 1px solid var(--border);
      color: var(--text);
      padding: 1rem 1.2rem;
      font-family: 'Fira Code', monospace;
      font-size: 0.9rem;
      outline: none;
      transition: all 0.3s;
      width: 100%;
      box-sizing: border-box;
    }

    .neon-input:focus, .neon-select:focus {
      border-color: var(--neon);
      box-shadow: 0 0 15px rgba(139, 92, 246, 0.15);
    }

    .neon-input::placeholder { color: rgba(100, 116, 139, 0.5); }

    .neon-select option {
      background: var(--dark);
      color: var(--text);
    }

    /* ═══════ GENERATE BUTTON ═══════ */
    .generate-btn {
      position: relative;
      width: 100%;
      padding: 1.5rem;
      background: linear-gradient(135deg, var(--neon), var(--neon2));
      border: none;
      color: #fff;
      font-family: 'Orbitron', sans-serif;
      font-size: 14px;
      font-weight: 700;
      letter-spacing: 3px;
      cursor: pointer;
      transition: all 0.4s;
      overflow: hidden;
      clip-path: polygon(15px 0, 100% 0, calc(100% - 15px) 100%, 0 100%);
    }

    .generate-btn.quiz-gen {
      background: linear-gradient(135deg, var(--neon-amber), var(--neon-pink));
    }

    .generate-btn:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 10px 40px rgba(139, 92, 246, 0.3);
    }

    .generate-btn:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .btn-glow {
      position: absolute;
      top: -50%; left: -10%;
      width: 120%;
      height: 200%;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
      animation: shimmer 3s infinite;
    }

    @keyframes shimmer {
      0% { transform: translateX(-100%); }
      100% { transform: translateX(100%); }
    }

    .generating {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .gen-dot {
      width: 8px; height: 8px;
      background: #fff;
      border-radius: 50%;
      animation: gen-bounce 1.4s infinite ease-in-out;
    }
    .gen-dot:nth-child(1) { animation-delay: 0s; }
    .gen-dot:nth-child(2) { animation-delay: 0.2s; }
    .gen-dot:nth-child(3) { animation-delay: 0.4s; }

    @keyframes gen-bounce {
      0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
      40% { transform: scale(1); opacity: 1; }
    }

    /* ═══════ RESULT DISPLAY ═══════ */
    .result-display {
      margin-top: 3rem;
      animation: panel-in 0.5s ease-out;
    }

    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
      padding-bottom: 1rem;
      border-bottom: 1px solid var(--border);
    }

    .result-badge {
      font-family: 'Orbitron', sans-serif;
      font-size: 12px;
      color: var(--neon3);
      letter-spacing: 2px;
      padding: 8px 16px;
      background: rgba(16, 185, 129, 0.1);
      border: 1px solid rgba(16, 185, 129, 0.3);
    }

    .result-badge.quiz-badge {
      color: var(--neon-amber);
      background: rgba(245, 158, 11, 0.1);
      border-color: rgba(245, 158, 11, 0.3);
    }

    .copy-btn {
      background: rgba(255,255,255,0.05);
      border: 1px solid var(--border);
      color: var(--muted);
      padding: 8px 16px;
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      cursor: pointer;
      transition: all 0.3s;
      letter-spacing: 1px;
    }

    .copy-btn:hover {
      border-color: var(--neon2);
      color: var(--neon2);
    }

    .header-actions {
      display: flex;
      gap: 1rem;
      align-items: center;
    }

    .publish-btn {
      background: rgba(16, 185, 129, 0.1);
      border: 1px solid rgba(16, 185, 129, 0.3);
      color: var(--neon3);
      padding: 8px 16px;
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      cursor: pointer;
      transition: all 0.3s;
      letter-spacing: 1px;
    }

    .publish-btn:hover:not(:disabled) {
      background: rgba(16, 185, 129, 0.2);
      border-color: var(--neon3);
      color: #fff;
    }

    .publish-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    /* ═══════ MARKDOWN BODY ═══════ */
    .markdown-body {
      line-height: 1.8;
      font-size: 0.95rem;
      color: var(--text);
    }

    .markdown-body :first-child { margin-top: 0; }

    :host ::ng-deep .markdown-body h1,
    :host ::ng-deep .markdown-body h2,
    :host ::ng-deep .markdown-body h3 {
      font-family: 'Orbitron', sans-serif;
      color: var(--neon2);
      margin-top: 2rem;
      margin-bottom: 1rem;
      letter-spacing: 1px;
    }

    :host ::ng-deep .markdown-body h2 { font-size: 1.2rem; }
    :host ::ng-deep .markdown-body h3 { font-size: 1rem; color: var(--neon); }

    :host ::ng-deep .markdown-body code {
      background: rgba(139, 92, 246, 0.1);
      color: var(--neon);
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Fira Code', monospace;
      font-size: 0.85rem;
    }

    :host ::ng-deep .markdown-body pre {
      background: #0a0a14;
      border: 1px solid var(--border);
      padding: 1.5rem;
      overflow-x: auto;
      margin: 1rem 0;
    }

    :host ::ng-deep .markdown-body pre code {
      background: transparent;
      padding: 0;
    }

    :host ::ng-deep .markdown-body ul,
    :host ::ng-deep .markdown-body ol {
      padding-left: 1.5rem;
    }

    :host ::ng-deep .markdown-body li {
      margin-bottom: 0.5rem;
    }

    :host ::ng-deep .markdown-body table {
      width: 100%;
      border-collapse: collapse;
      margin: 1rem 0;
    }

    :host ::ng-deep .markdown-body th,
    :host ::ng-deep .markdown-body td {
      border: 1px solid var(--border);
      padding: 0.8rem 1rem;
      text-align: left;
    }

    :host ::ng-deep .markdown-body th {
      background: rgba(139, 92, 246, 0.08);
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      letter-spacing: 1px;
      color: var(--neon);
    }

    :host ::ng-deep .markdown-body blockquote {
      border-left: 3px solid var(--neon);
      padding-left: 1rem;
      margin: 1rem 0;
      color: var(--muted);
      font-style: italic;
    }

    :host ::ng-deep .markdown-body strong {
      color: var(--neon2);
    }

    :host ::ng-deep .markdown-body hr {
      border: none;
      border-top: 1px solid var(--border);
      margin: 2rem 0;
    }

    /* ═══════ QUIZ PREVIEW ═══════ */
    .quiz-preview {}

    .quiz-title {
      font-family: 'Orbitron', sans-serif;
      color: var(--neon-amber);
      font-size: 1.3rem;
      letter-spacing: 2px;
      margin-bottom: 0.5rem;
    }

    .quiz-desc {
      color: var(--muted);
      margin-bottom: 2rem;
    }

    .question-card {
      background: rgba(0,0,0,0.3);
      border: 1px solid var(--border);
      padding: 2rem;
      margin-bottom: 1.5rem;
      transition: border-color 0.3s;
    }

    .question-card:hover { border-color: rgba(139, 92, 246, 0.3); }

    .q-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1.2rem;
    }

    .q-number {
      font-family: 'Orbitron', sans-serif;
      font-size: 14px;
      font-weight: 900;
      color: var(--neon);
      background: rgba(139, 92, 246, 0.1);
      padding: 4px 12px;
    }

    .q-type {
      font-family: 'Orbitron', sans-serif;
      font-size: 9px;
      letter-spacing: 1px;
      padding: 4px 10px;
      border: 1px solid var(--border);
    }

    .q-type.mcq { color: var(--neon2); border-color: var(--neon2); }
    .q-type.code_analysis { color: var(--neon-amber); border-color: var(--neon-amber); }
    .q-type.code_completion { color: var(--neon3); border-color: var(--neon3); }

    .q-points {
      font-family: 'Orbitron', sans-serif;
      font-size: 11px;
      color: var(--neon-amber);
      margin-left: auto;
    }

    .q-content {
      font-size: 1rem;
      line-height: 1.6;
      margin-bottom: 1rem;
    }

    .q-code {
      background: #060610;
      border: 1px solid var(--border);
      padding: 1.2rem;
      font-family: 'Fira Code', monospace;
      font-size: 0.85rem;
      overflow-x: auto;
      margin-bottom: 1rem;
      white-space: pre-wrap;
      color: var(--neon2);
    }

    .q-options {
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
      margin-bottom: 1.2rem;
    }

    .q-option {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.8rem 1rem;
      background: rgba(0,0,0,0.3);
      border: 1px solid var(--border);
      font-size: 0.9rem;
      transition: all 0.3s;
    }

    .q-option.correct {
      border-color: var(--neon3);
      background: rgba(16, 185, 129, 0.08);
    }

    .opt-letter {
      font-family: 'Orbitron', sans-serif;
      font-size: 11px;
      font-weight: 700;
      color: var(--neon);
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(139,92,246,0.1);
      flex-shrink: 0;
    }

    .correct-indicator {
      margin-left: auto;
      color: var(--neon3);
      font-weight: 700;
    }

    .q-explanation {
      padding: 1rem;
      background: rgba(6, 182, 212, 0.05);
      border-left: 3px solid var(--neon2);
      font-size: 0.9rem;
      color: var(--muted);
      line-height: 1.6;
    }

    .q-explanation strong { color: var(--neon2); }

    /* ═══════ CHAT MODE ═══════ */
    .chat-mode .panel-header { margin-bottom: 0; }

    .chat-container {
      background: rgba(0,0,0,0.3);
      border: 1px solid var(--border);
      height: 500px;
      overflow-y: auto;
      margin-top: 2rem;
      padding: 2rem;
      scroll-behavior: smooth;
    }

    .chat-container::-webkit-scrollbar { width: 6px; }
    .chat-container::-webkit-scrollbar-track { background: transparent; }
    .chat-container::-webkit-scrollbar-thumb { background: var(--border); border-radius: 3px; }

    .chat-messages { display: flex; flex-direction: column; gap: 1.5rem; }

    .chat-msg {
      display: flex;
      gap: 1rem;
      animation: msg-in 0.3s ease-out;
    }

    @keyframes msg-in {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .chat-msg.user { flex-direction: row-reverse; }

    .msg-avatar {
      font-size: 1.5rem;
      width: 45px;
      height: 45px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(139,92,246,0.08);
      border: 1px solid var(--border);
      border-radius: 50%;
      flex-shrink: 0;
    }

    .chat-msg.user .msg-avatar {
      background: rgba(6,182,212,0.08);
    }

    .msg-body {
      max-width: 75%;
    }

    .msg-name {
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      color: var(--muted);
      letter-spacing: 2px;
      margin-bottom: 0.5rem;
    }

    .chat-msg.user .msg-name { text-align: right; }

    .msg-text {
      background: rgba(0,0,0,0.4);
      border: 1px solid var(--border);
      padding: 1.2rem;
      line-height: 1.7;
      font-size: 0.95rem;
    }

    .chat-msg.user .msg-text {
      background: rgba(139, 92, 246, 0.1);
      border-color: rgba(139, 92, 246, 0.2);
    }

    .msg-time {
      font-size: 0.75rem;
      color: var(--muted);
      margin-top: 0.3rem;
      font-family: 'Fira Code', monospace;
    }

    .chat-msg.user .msg-time { text-align: right; }

    .typing-indicator {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 1.2rem 1.5rem !important;
    }

    .typing-dot {
      width: 8px; height: 8px;
      background: var(--neon);
      border-radius: 50%;
      animation: typing-pulse 1.4s ease-in-out infinite;
    }
    .typing-dot:nth-child(2) { animation-delay: 0.2s; }
    .typing-dot:nth-child(3) { animation-delay: 0.4s; }

    @keyframes typing-pulse {
      0%, 60%, 100% { transform: scale(0.6); opacity: 0.3; }
      30% { transform: scale(1); opacity: 1; }
    }

    /* Chat input bar */
    .chat-input-bar {
      display: flex;
      gap: 0;
      margin-top: 1.5rem;
    }

    .chat-input {
      flex: 1;
      background: rgba(0,0,0,0.5);
      border: 1px solid var(--border);
      border-right: none;
      color: var(--text);
      padding: 1.2rem 1.5rem;
      font-family: 'Rajdhani', sans-serif;
      font-size: 1rem;
      outline: none;
      transition: border-color 0.3s;
    }

    .chat-input:focus { border-color: var(--neon); }
    .chat-input::placeholder { color: rgba(100,116,139,0.5); }
    .chat-input:disabled { opacity: 0.5; }

    .send-btn {
      width: 60px;
      background: linear-gradient(135deg, var(--neon), var(--neon2));
      border: none;
      color: #fff;
      font-size: 1.2rem;
      cursor: pointer;
      transition: all 0.3s;
    }

    .send-btn:hover:not(:disabled) {
      box-shadow: 0 0 20px rgba(139, 92, 246, 0.3);
    }

    .send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

    .send-spinner {
      display: inline-block;
      width: 18px; height: 18px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: #fff;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    /* ═══════ RESPONSIVE ═══════ */
    @media (max-width: 768px) {
      .mode-selector { grid-template-columns: 1fr; }
      .config-grid { grid-template-columns: 1fr; }
      .ai-title { font-size: 1.8rem; }
      .mode-panel { padding: 1.5rem; }
      .msg-body { max-width: 85%; }
    }

    .level-text { font-size: 1rem; }
  `]
})
export class CoachAiAssistantComponent {
  activeMode: 'SESSION_PLAN' | 'QUIZ_GENERATE' | 'CHAT' = 'SESSION_PLAN';
  isGenerating = false;
  generatedContent = '';
  renderedMarkdown = '';

  // Session Plan config
  sessionPlan = {
    topic: '',
    language: 'JAVA',
    level: 'INTERMEDIAIRE',
    durationMinutes: 60
  };

  // Quiz config
  quizConfig = {
    topic: '',
    language: 'JAVA',
    level: 'MEDIUM',
    questionCount: 5
  };

  generatedQuiz: any = null;

  // Chat
  chatMessages: ChatMessage[] = [];
  chatInput = '';
  chatContext = '';

  // Particles
  particles = Array.from({ length: 20 }, (_, i) => ({
    x: Math.random() * 100,
    y: Math.random() * 100,
    delay: Math.random() * 10,
    size: Math.random() * 4 + 2
  }));

  isPublishing = false;
  publishSuccess = false;

  constructor(private aiService: AiService, private quizService: QuizService) {}

  setMode(mode: 'SESSION_PLAN' | 'QUIZ_GENERATE' | 'CHAT') {
    this.activeMode = mode;
  }

  generateSessionPlan() {
    this.isGenerating = true;
    this.generatedContent = '';
    this.renderedMarkdown = '';

    const request: AiRequest = {
      mode: 'SESSION_PLAN',
      topic: this.sessionPlan.topic,
      language: this.sessionPlan.language,
      level: this.sessionPlan.level,
      durationMinutes: this.sessionPlan.durationMinutes
    };

    this.aiService.generate(request).subscribe({
      next: (res) => {
        this.generatedContent = res.content;
        this.renderedMarkdown = this.renderMarkdown(res.content);
        this.isGenerating = false;
      },
      error: (err) => {
        console.error('AI generation error:', err);
        this.isGenerating = false;
        this.generatedContent = '❌ Error generating session plan. Please try again.';
        this.renderedMarkdown = this.generatedContent;
      }
    });
  }

  generateQuiz() {
    this.isGenerating = true;
    this.generatedContent = '';
    this.generatedQuiz = null;
    this.publishSuccess = false;
    this.isPublishing = false;

    const request: AiRequest = {
      mode: 'QUIZ_GENERATE',
      topic: this.quizConfig.topic,
      language: this.quizConfig.language,
      level: this.quizConfig.level,
      questionCount: this.quizConfig.questionCount
    };

    this.aiService.generate(request).subscribe({
      next: (res) => {
        this.generatedContent = res.content;
        try {
          // Try to parse JSON from the response
          let jsonStr = res.content;
          // Remove markdown code blocks if present
          const jsonMatch = jsonStr.match(/```json?\s*([\s\S]*?)\s*```/);
          if (jsonMatch) {
            jsonStr = jsonMatch[1];
          }
          // Try to find JSON object
          const firstBrace = jsonStr.indexOf('{');
          const lastBrace = jsonStr.lastIndexOf('}');
          if (firstBrace !== -1 && lastBrace !== -1) {
            jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
          }
          this.generatedQuiz = JSON.parse(jsonStr);
        } catch (e) {
          console.error('Failed to parse quiz JSON:', e);
          this.generatedQuiz = {
            quizTitle: 'Generated Quiz',
            quizDescription: 'Quiz content generated by AI',
            questions: []
          };
          // Display as markdown fallback
          this.renderedMarkdown = this.renderMarkdown(res.content);
        }
        this.isGenerating = false;
      },
      error: (err) => {
        console.error('AI generation error:', err);
        this.isGenerating = false;
      }
    });
  }

  publishQuiz() {
    if (!this.generatedQuiz || this.isPublishing || this.publishSuccess) return;
    
    this.isPublishing = true;
    
    const newQuiz: Partial<Quiz> = {
      title: this.generatedQuiz.quizTitle,
      description: this.generatedQuiz.quizDescription,
      difficulty: this.quizConfig.level as 'EASY' | 'MEDIUM' | 'HARD',
      language: this.quizConfig.language,
      category: this.quizConfig.topic,
      totalPoints: this.generatedQuiz.questions.reduce((sum: number, q: any) => sum + (q.points || 10), 0),
      questions: this.generatedQuiz.questions.map((q: any) => ({
        content: q.content,
        type: q.type || 'MCQ',
        options: q.options ? (Array.isArray(q.options) ? q.options.join(' ||| ') : q.options) : '',
        correctAnswer: q.correctAnswer,
        explanation: q.explanation,
        codeSnippet: q.codeSnippet || null,
        points: q.points || 10,
        language: this.quizConfig.language,
        difficulty: this.quizConfig.level as 'EASY' | 'MEDIUM' | 'HARD'
      }))
    };
    
    this.quizService.createQuiz(newQuiz).subscribe({
      next: (res) => {
        this.isPublishing = false;
        this.publishSuccess = true;
      },
      error: (err) => {
        console.error('Failed to publish quiz', err);
        this.isPublishing = false;
      }
    });
  }

  sendChatMessage() {
    if (!this.chatInput.trim() || this.isGenerating) return;

    const userMsg: ChatMessage = {
      role: 'user',
      content: this.chatInput.trim(),
      timestamp: new Date()
    };
    this.chatMessages.push(userMsg);

    const typingMsg: ChatMessage = {
      role: 'ai',
      content: '',
      timestamp: new Date(),
      isTyping: true
    };
    this.chatMessages.push(typingMsg);

    const request: AiRequest = {
      mode: 'CHAT',
      message: this.chatInput.trim(),
      context: this.chatContext
    };

    this.chatInput = '';
    this.isGenerating = true;
    this.scrollToBottom();

    this.aiService.generate(request).subscribe({
      next: (res) => {
        // Remove typing indicator and add real message
        const typingIdx = this.chatMessages.indexOf(typingMsg);
        if (typingIdx > -1) {
          this.chatMessages.splice(typingIdx, 1);
        }

        const aiMsg: ChatMessage = {
          role: 'ai',
          content: res.content,
          timestamp: new Date()
        };
        this.chatMessages.push(aiMsg);

        // Build context for follow-up messages
        this.chatContext = this.chatMessages
          .slice(-4)
          .map(m => `${m.role}: ${m.content.substring(0, 200)}`)
          .join('\n');

        this.isGenerating = false;
        this.scrollToBottom();
      },
      error: (err) => {
        const typingIdx = this.chatMessages.indexOf(typingMsg);
        if (typingIdx > -1) {
          this.chatMessages.splice(typingIdx, 1);
        }

        this.chatMessages.push({
          role: 'ai',
          content: '❌ I encountered an error. Please try again.',
          timestamp: new Date()
        });
        this.isGenerating = false;
        this.scrollToBottom();
      }
    });
  }

  renderMarkdown(content: string): string {
    if (!content) return '';
    try {
      return marked.parse(content, { async: false }) as string;
    } catch {
      return content;
    }
  }

  splitOptions(options: string): string[] {
    if (!options) return [];
    return options.split('|||').map(o => o.trim());
  }

  copyToClipboard(text: string) {
    navigator.clipboard.writeText(text).then(() => {
      // Could add a toast/notification here
    });
  }

  private scrollToBottom() {
    setTimeout(() => {
      const container = document.getElementById('chatContainer');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 100);
  }
}
