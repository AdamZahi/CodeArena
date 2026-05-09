import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { AiService, AiRequest } from '../../services/ai.service';
import { AlertService } from '../../services/alert.service';
import { marked } from 'marked';

type MentorMode = 'CODE_EXPLAIN' | 'CODE_REVIEW' | 'PRACTICE_EXERCISE' | 'DEBUG_HELP';

@Component({
  selector: 'app-ai-code-mentor',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="mentor-universe">
      <!-- Animated background particles -->
      <div class="particles">
        <div class="particle" *ngFor="let p of particles" [style.left.%]="p.x" [style.top.%]="p.y"
             [style.animationDelay.s]="p.delay" [style.width.px]="p.size" [style.height.px]="p.size"></div>
      </div>

      <!-- Neural grid overlay -->
      <div class="neural-grid"></div>

      <div class="mentor-container">
        <!-- PAGE HEADER -->
        <div class="mentor-hero">
          <div class="ai-brain-container">
            <div class="brain-ring ring-outer"></div>
            <div class="brain-ring ring-mid"></div>
            <div class="brain-ring ring-inner"></div>
            <div class="brain-core">🎓</div>
            <div class="brain-pulse"></div>
          </div>
          <h1 class="mentor-title">
            <span class="prefix">AI</span> CODE_<span class="accent">MENTOR</span>
          </h1>
          <p class="mentor-subtitle">
            Your personal AI-powered coding tutor — Learn smarter, code faster, debug like a pro
          </p>
          <div class="hero-stats">
            <div class="stat-chip">
              <span class="stat-icon">🧠</span>
              <span class="stat-text">Neural Powered</span>
            </div>
            <div class="stat-chip">
              <span class="stat-icon">⚡</span>
              <span class="stat-text">Instant Analysis</span>
            </div>
            <div class="stat-chip">
              <span class="stat-icon">🎯</span>
              <span class="stat-text">Personalized</span>
            </div>
          </div>
        </div>

        <!-- MODE SELECTOR -->
        <div class="mode-selector">
          <button class="mode-btn" [class.active]="activeMode === 'CODE_EXPLAIN'"
                  (click)="setMode('CODE_EXPLAIN')">
            <div class="mode-icon-wrap explain">
              <span class="mode-icon">📖</span>
            </div>
            <span class="mode-label">CODE EXPLAINER</span>
            <span class="mode-desc">Paste code and get step-by-step explanations</span>
            <div class="mode-glow"></div>
          </button>
          <button class="mode-btn" [class.active]="activeMode === 'CODE_REVIEW'"
                  (click)="setMode('CODE_REVIEW')">
            <div class="mode-icon-wrap review">
              <span class="mode-icon">🔍</span>
            </div>
            <span class="mode-label">CODE REVIEW</span>
            <span class="mode-desc">Get AI-powered code review & improvements</span>
            <div class="mode-glow"></div>
          </button>
          <button class="mode-btn" [class.active]="activeMode === 'PRACTICE_EXERCISE'"
                  (click)="setMode('PRACTICE_EXERCISE')">
            <div class="mode-icon-wrap practice">
              <span class="mode-icon">🏋️</span>
            </div>
            <span class="mode-label">PRACTICE LAB</span>
            <span class="mode-desc">Generate exercises tailored to your level</span>
            <div class="mode-glow"></div>
          </button>
          <button class="mode-btn" [class.active]="activeMode === 'DEBUG_HELP'"
                  (click)="setMode('DEBUG_HELP')">
            <div class="mode-icon-wrap debug">
              <span class="mode-icon">🐛</span>
            </div>
            <span class="mode-label">DEBUG HELPER</span>
            <span class="mode-desc">Paste buggy code, get fixes & explanations</span>
            <div class="mode-glow"></div>
          </button>
        </div>

        <!-- ═══════ CODE EXPLAINER MODE ═══════ -->
        <div class="mode-panel" *ngIf="activeMode === 'CODE_EXPLAIN'">
          <div class="panel-header">
            <div class="panel-icon explain-icon">📖</div>
            <div>
              <h2>CODE EXPLAINER</h2>
              <p>Paste any code snippet and get a clear, step-by-step explanation with annotations</p>
            </div>
          </div>

          <div class="config-grid">
            <div class="config-field full-width">
              <label>YOUR CODE</label>
              <textarea [(ngModel)]="codeInput" class="code-textarea" rows="10"
                        placeholder="// Paste your code here...&#10;// Example:&#10;public class Example {&#10;    public static void main(String[] args) {&#10;        System.out.println(&quot;Hello World&quot;);&#10;    }&#10;}"></textarea>
            </div>
            <div class="config-field">
              <label>PROGRAMMING LANGUAGE</label>
              <select [(ngModel)]="selectedLanguage" class="neon-select">
                <option value="JAVA">Java</option>
                <option value="PYTHON">Python</option>
                <option value="JAVASCRIPT">JavaScript</option>
                <option value="ANGULAR">Angular / TypeScript</option>
                <option value="DOTNET">.NET / C#</option>
                <option value="CSS">CSS</option>
              </select>
            </div>
            <div class="config-field">
              <label>EXPLANATION DEPTH</label>
              <select [(ngModel)]="explainDepth" class="neon-select">
                <option value="beginner">Beginner — Explain everything</option>
                <option value="intermediate">Intermediate — Focus on logic</option>
                <option value="advanced">Advanced — Deep dive only</option>
              </select>
            </div>
          </div>

          <button class="generate-btn explain-gen" (click)="generateExplanation()"
                  [disabled]="isGenerating || !codeInput.trim()">
            <span class="btn-glow"></span>
            <span *ngIf="!isGenerating">📖 EXPLAIN THIS CODE</span>
            <span *ngIf="isGenerating" class="generating">
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              ANALYZING CODE...
            </span>
          </button>
        </div>

        <!-- ═══════ CODE REVIEW MODE ═══════ -->
        <div class="mode-panel" *ngIf="activeMode === 'CODE_REVIEW'">
          <div class="panel-header">
            <div class="panel-icon review-icon">🔍</div>
            <div>
              <h2>AI CODE REVIEWER</h2>
              <p>Submit your code for a professional AI review — get quality scores, suggestions, and best practices</p>
            </div>
          </div>

          <div class="config-grid">
            <div class="config-field full-width">
              <label>CODE TO REVIEW</label>
              <textarea [(ngModel)]="codeInput" class="code-textarea" rows="10"
                        placeholder="// Paste your code here for review..."></textarea>
            </div>
            <div class="config-field">
              <label>PROGRAMMING LANGUAGE</label>
              <select [(ngModel)]="selectedLanguage" class="neon-select">
                <option value="JAVA">Java</option>
                <option value="PYTHON">Python</option>
                <option value="JAVASCRIPT">JavaScript</option>
                <option value="ANGULAR">Angular / TypeScript</option>
                <option value="DOTNET">.NET / C#</option>
              </select>
            </div>
            <div class="config-field">
              <label>REVIEW FOCUS</label>
              <select [(ngModel)]="reviewFocus" class="neon-select">
                <option value="all">Full Review (All Aspects)</option>
                <option value="performance">Performance & Optimization</option>
                <option value="security">Security Vulnerabilities</option>
                <option value="clean-code">Clean Code & Readability</option>
                <option value="best-practices">Best Practices & Patterns</option>
              </select>
            </div>
          </div>

          <button class="generate-btn review-gen" (click)="generateReview()"
                  [disabled]="isGenerating || !codeInput.trim()">
            <span class="btn-glow"></span>
            <span *ngIf="!isGenerating">🔍 START CODE REVIEW</span>
            <span *ngIf="isGenerating" class="generating">
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              REVIEWING CODE...
            </span>
          </button>
        </div>

        <!-- ═══════ PRACTICE LAB MODE ═══════ -->
        <div class="mode-panel" *ngIf="activeMode === 'PRACTICE_EXERCISE'">
          <div class="panel-header">
            <div class="panel-icon practice-icon">🏋️</div>
            <div>
              <h2>AI PRACTICE LAB</h2>
              <p>Generate personalized coding exercises with solutions — practice what you need to improve</p>
            </div>
          </div>

          <div class="config-grid">
            <div class="config-field">
              <label>TOPIC / CONCEPT</label>
              <input type="text" [(ngModel)]="practiceConfig.topic" class="neon-input"
                     placeholder="e.g. Recursion, Sorting, OOP, REST API..." />
            </div>
            <div class="config-field">
              <label>PROGRAMMING LANGUAGE</label>
              <select [(ngModel)]="selectedLanguage" class="neon-select">
                <option value="JAVA">Java</option>
                <option value="PYTHON">Python</option>
                <option value="JAVASCRIPT">JavaScript</option>
                <option value="ANGULAR">Angular / TypeScript</option>
                <option value="DOTNET">.NET / C#</option>
              </select>
            </div>
            <div class="config-field">
              <label>DIFFICULTY LEVEL</label>
              <select [(ngModel)]="practiceConfig.level" class="neon-select">
                <option value="BASIQUE">Basique — Foundation</option>
                <option value="INTERMEDIAIRE">Intermédiaire — Applied</option>
                <option value="AVANCE">Avancé — Challenge</option>
              </select>
            </div>
            <div class="config-field">
              <label>NUMBER OF EXERCISES</label>
              <select [(ngModel)]="practiceConfig.count" class="neon-select">
                <option [ngValue]="1">1 Exercise</option>
                <option [ngValue]="2">2 Exercises</option>
                <option [ngValue]="3">3 Exercises</option>
                <option [ngValue]="5">5 Exercises</option>
              </select>
            </div>
          </div>

          <button class="generate-btn practice-gen" (click)="generatePractice()"
                  [disabled]="isGenerating || !practiceConfig.topic">
            <span class="btn-glow"></span>
            <span *ngIf="!isGenerating">🏋️ GENERATE EXERCISES</span>
            <span *ngIf="isGenerating" class="generating">
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              CRAFTING EXERCISES...
            </span>
          </button>
        </div>

        <!-- ═══════ DEBUG HELPER MODE ═══════ -->
        <div class="mode-panel" *ngIf="activeMode === 'DEBUG_HELP'">
          <div class="panel-header">
            <div class="panel-icon debug-icon">🐛</div>
            <div>
              <h2>AI DEBUG HELPER</h2>
              <p>Paste buggy code with error messages — get root cause analysis and step-by-step fixes</p>
            </div>
          </div>

          <div class="config-grid">
            <div class="config-field full-width">
              <label>BUGGY CODE</label>
              <textarea [(ngModel)]="codeInput" class="code-textarea" rows="8"
                        placeholder="// Paste your buggy code here..."></textarea>
            </div>
            <div class="config-field full-width">
              <label>ERROR MESSAGE / DESCRIPTION (optional)</label>
              <textarea [(ngModel)]="errorMessage" class="code-textarea error-textarea" rows="3"
                        placeholder="Paste error message or describe what's going wrong...&#10;e.g. NullPointerException at line 42"></textarea>
            </div>
            <div class="config-field">
              <label>PROGRAMMING LANGUAGE</label>
              <select [(ngModel)]="selectedLanguage" class="neon-select">
                <option value="JAVA">Java</option>
                <option value="PYTHON">Python</option>
                <option value="JAVASCRIPT">JavaScript</option>
                <option value="ANGULAR">Angular / TypeScript</option>
                <option value="DOTNET">.NET / C#</option>
              </select>
            </div>
            <div class="config-field">
              <label>FIX VERBOSITY</label>
              <select [(ngModel)]="debugVerbosity" class="neon-select">
                <option value="quick">Quick Fix — Just the solution</option>
                <option value="detailed">Detailed — Explain why & how</option>
                <option value="educational">Educational — Learn from the bug</option>
              </select>
            </div>
          </div>

          <button class="generate-btn debug-gen" (click)="generateDebugHelp()"
                  [disabled]="isGenerating || !codeInput.trim()">
            <span class="btn-glow"></span>
            <span *ngIf="!isGenerating">🐛 FIND & FIX BUGS</span>
            <span *ngIf="isGenerating" class="generating">
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              <span class="gen-dot"></span>
              DEBUGGING...
            </span>
          </button>
        </div>

        <!-- ═══════ RESULT DISPLAY ═══════ -->
        <div class="result-display" *ngIf="generatedContent">
          <div class="result-header">
            <span class="result-badge" [class]="getResultBadgeClass()">
              {{ getResultBadgeText() }}
            </span>
            <div class="header-actions">
              <button class="copy-btn" (click)="copyToClipboard(generatedContent)">
                {{ copySuccess ? '✅ COPIED' : '📋 COPY' }}
              </button>
              <button class="reset-btn" (click)="resetResult()">🗑️ CLEAR</button>
            </div>
          </div>
          <div class="markdown-body" [innerHTML]="renderedMarkdown"></div>
        </div>

        <!-- ERROR DISPLAY -->
        <div class="error-display" *ngIf="errorDisplay">
          <div class="error-icon">⚠️</div>
          <p>{{ errorDisplay }}</p>
          <button class="retry-btn" (click)="errorDisplay = ''">DISMISS</button>
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
      --neon-blue: #3b82f6;
      --dark: #050508;
      --dark2: #0a0a12;
      --card: #0c0c18;
      --border: #1a1a30;
      --text: #e2e8f0;
      --muted: #64748b;
    }

    /* ═══════ BACKGROUND UNIVERSE ═══════ */
    .mentor-universe {
      min-height: 100vh;
      background: var(--dark);
      position: relative;
      overflow: hidden;
      font-family: 'Rajdhani', sans-serif;
      color: var(--text);
    }

    .mentor-universe::before {
      content: '';
      position: fixed;
      top: 0; left: 0;
      width: 100%; height: 100%;
      background:
        radial-gradient(ellipse at 15% 30%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
        radial-gradient(ellipse at 85% 60%, rgba(139, 92, 246, 0.06) 0%, transparent 50%),
        radial-gradient(ellipse at 50% 90%, rgba(6, 182, 212, 0.04) 0%, transparent 50%);
      pointer-events: none;
      z-index: 0;
    }

    /* Neural grid */
    .neural-grid {
      position: fixed;
      top: 0; left: 0; width: 100%; height: 100%;
      background-image:
        linear-gradient(rgba(139,92,246,0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(139,92,246,0.03) 1px, transparent 1px);
      background-size: 60px 60px;
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
      background: var(--neon-blue);
      border-radius: 50%;
      opacity: 0.12;
      animation: float-particle 18s ease-in-out infinite;
    }

    @keyframes float-particle {
      0%, 100% { transform: translateY(0) translateX(0) scale(1); opacity: 0.08; }
      25% { transform: translateY(-40px) translateX(25px) scale(1.2); opacity: 0.25; }
      50% { transform: translateY(-70px) translateX(-15px) scale(0.8); opacity: 0.12; }
      75% { transform: translateY(-35px) translateX(35px) scale(1.1); opacity: 0.2; }
    }

    .mentor-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 3rem 2rem 6rem;
      position: relative;
      z-index: 1;
    }

    /* ═══════ HERO SECTION ═══════ */
    .mentor-hero {
      text-align: center;
      margin-bottom: 4rem;
      position: relative;
    }

    .ai-brain-container {
      position: relative;
      width: 140px;
      height: 140px;
      margin: 0 auto 2rem;
    }

    .brain-ring {
      position: absolute;
      border: 2px solid transparent;
      border-radius: 50%;
    }

    .ring-outer {
      inset: 0;
      border-top-color: var(--neon-blue);
      border-right-color: var(--neon-blue);
      animation: orbit 3s linear infinite;
    }
    .ring-mid {
      inset: 15px;
      border-bottom-color: var(--neon);
      border-left-color: var(--neon);
      animation: orbit 4s linear infinite reverse;
    }
    .ring-inner {
      inset: 30px;
      border-top-color: var(--neon2);
      border-right-color: var(--neon2);
      animation: orbit 5s linear infinite;
    }

    @keyframes orbit { to { transform: rotate(360deg); } }

    .brain-core {
      position: absolute;
      inset: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 3rem;
      background: radial-gradient(circle, rgba(59,130,246,0.15), transparent);
      border-radius: 50%;
      animation: pulse-core 2s ease-in-out infinite;
    }

    .brain-pulse {
      position: absolute;
      inset: 20px;
      border-radius: 50%;
      border: 1px solid rgba(59,130,246,0.15);
      animation: pulse-expand 3s ease-in-out infinite;
    }

    @keyframes pulse-core {
      0%, 100% { transform: scale(1); filter: brightness(1); }
      50% { transform: scale(1.1); filter: brightness(1.3); }
    }

    @keyframes pulse-expand {
      0%, 100% { transform: scale(1); opacity: 0.5; }
      50% { transform: scale(1.3); opacity: 0; }
    }

    .mentor-title {
      font-family: 'Orbitron', sans-serif;
      font-size: 2.8rem;
      font-weight: 900;
      letter-spacing: 4px;
      margin-bottom: 1rem;
    }

    .mentor-title .prefix {
      color: var(--neon-blue);
      text-shadow: 0 0 20px rgba(59, 130, 246, 0.5);
    }

    .mentor-title .accent {
      color: var(--neon);
      text-shadow: 0 0 20px rgba(139, 92, 246, 0.5);
    }

    .mentor-subtitle {
      color: var(--muted);
      font-size: 1.1rem;
      letter-spacing: 1px;
      max-width: 650px;
      margin: 0 auto 2rem;
    }

    .hero-stats {
      display: flex;
      justify-content: center;
      gap: 1.5rem;
      flex-wrap: wrap;
    }

    .stat-chip {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 8px 18px;
      background: rgba(59,130,246,0.06);
      border: 1px solid rgba(59,130,246,0.15);
      font-family: 'Orbitron', sans-serif;
      font-size: 9px;
      letter-spacing: 1.5px;
      color: var(--muted);
      clip-path: polygon(8px 0%, 100% 0%, calc(100% - 8px) 100%, 0% 100%);
    }

    .stat-icon { font-size: 14px; }

    /* ═══════ MODE SELECTOR ═══════ */
    .mode-selector {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1.2rem;
      margin-bottom: 3rem;
    }

    .mode-btn {
      background: var(--card);
      border: 1px solid var(--border);
      padding: 1.8rem 1.2rem;
      cursor: pointer;
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 0.8rem;
      clip-path: polygon(12px 0, 100% 0, 100% calc(100% - 12px), calc(100% - 12px) 100%, 0 100%, 0 12px);
    }

    .mode-btn::before {
      content: '';
      position: absolute;
      top: 0; left: 0;
      width: 100%; height: 100%;
      background: linear-gradient(135deg, rgba(59,130,246,0.05), transparent);
      opacity: 0;
      transition: opacity 0.4s;
    }

    .mode-btn:hover::before { opacity: 1; }
    .mode-btn:hover { border-color: var(--neon-blue); transform: translateY(-6px); }

    .mode-btn.active {
      border-color: var(--neon-blue);
      background: linear-gradient(135deg, rgba(59,130,246,0.1), rgba(139,92,246,0.05));
      box-shadow: 0 0 30px rgba(59, 130, 246, 0.15), inset 0 0 30px rgba(59, 130, 246, 0.05);
    }

    .mode-btn.active::after {
      content: '';
      position: absolute;
      bottom: 0; left: 0;
      width: 100%; height: 3px;
      background: linear-gradient(90deg, var(--neon-blue), var(--neon));
    }

    .mode-glow {
      position: absolute;
      top: -50%; left: -50%;
      width: 200%; height: 200%;
      background: radial-gradient(circle, rgba(59,130,246,0.04), transparent 70%);
      opacity: 0;
      transition: opacity 0.4s;
    }

    .mode-btn:hover .mode-glow,
    .mode-btn.active .mode-glow { opacity: 1; }

    .mode-icon-wrap {
      width: 56px; height: 56px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 12px;
      border: 1px solid var(--border);
      transition: all 0.3s;
    }

    .mode-icon-wrap.explain { background: rgba(59,130,246,0.1); }
    .mode-icon-wrap.review { background: rgba(139,92,246,0.1); }
    .mode-icon-wrap.practice { background: rgba(16,185,129,0.1); }
    .mode-icon-wrap.debug { background: rgba(244,63,94,0.1); }

    .mode-btn.active .mode-icon-wrap {
      transform: scale(1.1);
      box-shadow: 0 0 20px rgba(59,130,246,0.2);
    }

    .mode-icon { font-size: 1.8rem; }
    .mode-label {
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      font-weight: 700;
      color: var(--text);
      letter-spacing: 1.5px;
    }
    .mode-desc {
      font-size: 0.75rem;
      color: var(--muted);
      letter-spacing: 0.3px;
      line-height: 1.4;
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
      background: radial-gradient(circle, rgba(59,130,246,0.06), transparent);
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
      border: 1px solid var(--border);
      border-radius: 12px;
      flex-shrink: 0;
    }

    .explain-icon { background: rgba(59,130,246,0.08); }
    .review-icon { background: rgba(139,92,246,0.08); }
    .practice-icon { background: rgba(16,185,129,0.08); }
    .debug-icon { background: rgba(244,63,94,0.08); }

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
    .config-field.full-width { grid-column: 1 / -1; }

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
      border-color: var(--neon-blue);
      box-shadow: 0 0 15px rgba(59, 130, 246, 0.15);
    }

    .neon-input::placeholder { color: rgba(100, 116, 139, 0.5); }
    .neon-select option { background: var(--dark); color: var(--text); }

    .code-textarea {
      background: rgba(0,0,0,0.6);
      border: 1px solid var(--border);
      color: var(--neon2);
      padding: 1.2rem 1.5rem;
      font-family: 'Fira Code', monospace;
      font-size: 0.85rem;
      line-height: 1.8;
      outline: none;
      transition: all 0.3s;
      width: 100%;
      box-sizing: border-box;
      resize: vertical;
      min-height: 120px;
    }

    .code-textarea:focus {
      border-color: var(--neon-blue);
      box-shadow: 0 0 20px rgba(59, 130, 246, 0.1);
    }

    .code-textarea::placeholder { color: rgba(100, 116, 139, 0.4); }

    .error-textarea {
      color: var(--neon-pink) !important;
      border-color: rgba(244,63,94,0.2);
    }

    .error-textarea:focus {
      border-color: var(--neon-pink) !important;
      box-shadow: 0 0 20px rgba(244, 63, 94, 0.1) !important;
    }

    /* ═══════ GENERATE BUTTONS ═══════ */
    .generate-btn {
      position: relative;
      width: 100%;
      padding: 1.5rem;
      border: none;
      color: #fff;
      font-family: 'Orbitron', sans-serif;
      font-size: 13px;
      font-weight: 700;
      letter-spacing: 3px;
      cursor: pointer;
      transition: all 0.4s;
      overflow: hidden;
      clip-path: polygon(15px 0, 100% 0, calc(100% - 15px) 100%, 0 100%);
    }

    .explain-gen { background: linear-gradient(135deg, var(--neon-blue), var(--neon)); }
    .review-gen { background: linear-gradient(135deg, var(--neon), #a78bfa); }
    .practice-gen { background: linear-gradient(135deg, var(--neon3), var(--neon2)); }
    .debug-gen { background: linear-gradient(135deg, var(--neon-pink), var(--neon-amber)); }

    .generate-btn:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 10px 40px rgba(59, 130, 246, 0.3);
    }

    .generate-btn:disabled { opacity: 0.4; cursor: not-allowed; }

    .btn-glow {
      position: absolute;
      top: -50%; left: -10%;
      width: 120%; height: 200%;
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
      background: var(--card);
      border: 1px solid var(--border);
      padding: 2.5rem;
      animation: panel-in 0.5s ease-out;
      clip-path: polygon(20px 0, 100% 0, 100% calc(100% - 20px), calc(100% - 20px) 100%, 0 100%, 0 20px);
    }

    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
      padding-bottom: 1.5rem;
      border-bottom: 1px solid var(--border);
    }

    .result-badge {
      font-family: 'Orbitron', sans-serif;
      font-size: 11px;
      letter-spacing: 2px;
      padding: 8px 18px;
      border: 1px solid;
    }

    .result-badge.explain-badge { color: var(--neon-blue); background: rgba(59,130,246,0.1); border-color: rgba(59,130,246,0.3); }
    .result-badge.review-badge { color: var(--neon); background: rgba(139,92,246,0.1); border-color: rgba(139,92,246,0.3); }
    .result-badge.practice-badge { color: var(--neon3); background: rgba(16,185,129,0.1); border-color: rgba(16,185,129,0.3); }
    .result-badge.debug-badge { color: var(--neon-pink); background: rgba(244,63,94,0.1); border-color: rgba(244,63,94,0.3); }

    .header-actions {
      display: flex;
      gap: 0.8rem;
      align-items: center;
    }

    .copy-btn, .reset-btn {
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

    .copy-btn:hover { border-color: var(--neon2); color: var(--neon2); }
    .reset-btn:hover { border-color: var(--neon-pink); color: var(--neon-pink); }

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
      background: rgba(59, 130, 246, 0.1);
      color: var(--neon-blue);
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Fira Code', monospace;
      font-size: 0.85rem;
    }

    :host ::ng-deep .markdown-body pre {
      background: #050510;
      border: 1px solid var(--border);
      padding: 1.5rem;
      overflow-x: auto;
      margin: 1rem 0;
      border-left: 3px solid var(--neon-blue);
    }

    :host ::ng-deep .markdown-body pre code {
      background: transparent;
      color: var(--neon2);
      padding: 0;
    }

    :host ::ng-deep .markdown-body strong {
      color: var(--neon);
    }

    :host ::ng-deep .markdown-body table {
      width: 100%;
      border-collapse: collapse;
      margin: 1rem 0;
    }

    :host ::ng-deep .markdown-body th,
    :host ::ng-deep .markdown-body td {
      padding: 0.8rem 1rem;
      border: 1px solid var(--border);
      text-align: left;
    }

    :host ::ng-deep .markdown-body th {
      background: rgba(59,130,246,0.08);
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      letter-spacing: 1px;
      color: var(--neon-blue);
    }

    :host ::ng-deep .markdown-body blockquote {
      border-left: 3px solid var(--neon);
      padding: 0.8rem 1.2rem;
      margin: 1rem 0;
      background: rgba(139,92,246,0.05);
      color: var(--muted);
    }

    :host ::ng-deep .markdown-body ul,
    :host ::ng-deep .markdown-body ol {
      padding-left: 1.5rem;
    }

    :host ::ng-deep .markdown-body li {
      margin-bottom: 0.4rem;
    }

    :host ::ng-deep .markdown-body hr {
      border: none;
      border-top: 1px solid var(--border);
      margin: 2rem 0;
    }

    /* ═══════ ERROR DISPLAY ═══════ */
    .error-display {
      margin-top: 2rem;
      background: rgba(244,63,94,0.05);
      border: 1px solid rgba(244,63,94,0.2);
      padding: 2rem;
      text-align: center;
      animation: panel-in 0.4s ease-out;
    }

    .error-icon { font-size: 2.5rem; margin-bottom: 1rem; }

    .error-display p {
      color: var(--neon-pink);
      font-size: 0.95rem;
      margin-bottom: 1.5rem;
    }

    .retry-btn {
      background: rgba(244,63,94,0.1);
      border: 1px solid rgba(244,63,94,0.3);
      color: var(--neon-pink);
      padding: 8px 24px;
      font-family: 'Orbitron', sans-serif;
      font-size: 10px;
      cursor: pointer;
      transition: all 0.3s;
      letter-spacing: 1px;
    }

    .retry-btn:hover {
      background: rgba(244,63,94,0.2);
    }

    /* ═══════ RESPONSIVE ═══════ */
    @media (max-width: 900px) {
      .mode-selector { grid-template-columns: repeat(2, 1fr); }
      .mentor-title { font-size: 2rem; }
      .config-grid { grid-template-columns: 1fr; }
    }

    @media (max-width: 600px) {
      .mode-selector { grid-template-columns: 1fr; }
      .mentor-title { font-size: 1.5rem; }
      .mentor-container { padding: 1.5rem 1rem 4rem; }
      .mode-panel { padding: 1.5rem; }
    }
  `]
})
export class AiCodeMentorComponent {
  activeMode: MentorMode = 'CODE_EXPLAIN';
  isGenerating = false;
  generatedContent = '';
  renderedMarkdown = '';
  errorDisplay = '';
  copySuccess = false;

  // Shared inputs
  codeInput = '';
  selectedLanguage = 'JAVA';

  // Code Explainer config
  explainDepth = 'intermediate';

  // Code Review config
  reviewFocus = 'all';

  // Practice Lab config
  practiceConfig = {
    topic: '',
    level: 'INTERMEDIAIRE',
    count: 3
  };

  // Debug Helper config
  errorMessage = '';
  debugVerbosity = 'detailed';

  // Background particles
  particles = Array.from({ length: 25 }, () => ({
    x: Math.random() * 100,
    y: Math.random() * 100,
    delay: Math.random() * 10,
    size: Math.random() * 4 + 2
  }));

  constructor(private aiService: AiService, private alertService: AlertService) {}

  setMode(mode: MentorMode) {
    this.activeMode = mode;
    this.generatedContent = '';
    this.renderedMarkdown = '';
    this.errorDisplay = '';
  }

  generateExplanation() {
    const request: AiRequest = {
      mode: 'CODE_EXPLAIN' as any,
      message: this.codeInput,
      language: this.selectedLanguage,
      level: this.explainDepth,
      context: `Explain this code step by step at ${this.explainDepth} level`
    };
    this.callAi(request);
  }

  generateReview() {
    const request: AiRequest = {
      mode: 'CODE_REVIEW' as any,
      message: this.codeInput,
      language: this.selectedLanguage,
      context: `Review focus: ${this.reviewFocus}`
    };
    this.callAi(request);
  }

  generatePractice() {
    const request: AiRequest = {
      mode: 'PRACTICE_EXERCISE' as any,
      topic: this.practiceConfig.topic,
      language: this.selectedLanguage,
      level: this.practiceConfig.level,
      questionCount: this.practiceConfig.count,
      context: `Generate ${this.practiceConfig.count} practice exercises`
    };
    this.callAi(request);
  }

  generateDebugHelp() {
    const request: AiRequest = {
      mode: 'DEBUG_HELP' as any,
      message: this.codeInput,
      language: this.selectedLanguage,
      level: this.debugVerbosity,
      context: this.errorMessage ? `Error message: ${this.errorMessage}` : 'No error message provided'
    };
    this.callAi(request);
  }

  private callAi(request: AiRequest) {
    this.isGenerating = true;
    this.generatedContent = '';
    this.renderedMarkdown = '';
    this.errorDisplay = '';

    this.aiService.generate(request).subscribe({
      next: (res) => {
        this.isGenerating = false;
        if (res && res.content) {
          this.generatedContent = res.content;
          this.renderedMarkdown = this.renderMarkdown(res.content);
        } else {
          this.errorDisplay = 'No content was generated. Please try again.';
          this.alertService.error('No content was generated. Please try again.', 'AI_GENERATION_FAILED');
        }
      },
      error: (err) => {
        this.isGenerating = false;
        this.errorDisplay = err.error?.message || 'An error occurred while generating content. Please try again.';
        this.alertService.error(this.errorDisplay, 'AI_CONNECTION_ERROR');
      }
    });
  }

  renderMarkdown(text: string): string {
    if (!text) return '';
    try {
      // Clean thinking tags from Qwen model
      let cleaned = text.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
      return marked.parse(cleaned) as string;
    } catch {
      return text;
    }
  }

  copyToClipboard(text: string) {
    navigator.clipboard.writeText(text).then(() => {
      this.copySuccess = true;
      setTimeout(() => this.copySuccess = false, 2000);
    });
  }

  resetResult() {
    this.generatedContent = '';
    this.renderedMarkdown = '';
  }

  getResultBadgeClass(): string {
    switch (this.activeMode) {
      case 'CODE_EXPLAIN': return 'explain-badge';
      case 'CODE_REVIEW': return 'review-badge';
      case 'PRACTICE_EXERCISE': return 'practice-badge';
      case 'DEBUG_HELP': return 'debug-badge';
    }
  }

  getResultBadgeText(): string {
    switch (this.activeMode) {
      case 'CODE_EXPLAIN': return '📖 EXPLANATION READY';
      case 'CODE_REVIEW': return '🔍 REVIEW COMPLETE';
      case 'PRACTICE_EXERCISE': return '🏋️ EXERCISES GENERATED';
      case 'DEBUG_HELP': return '🐛 DEBUG REPORT READY';
    }
  }
}
