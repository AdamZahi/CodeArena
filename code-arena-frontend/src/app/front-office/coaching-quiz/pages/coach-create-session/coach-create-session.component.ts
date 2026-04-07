import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CoachingService } from '../../services/coaching.service';
import { CoachingSession } from '../../models/coaching-session.model';
import { AuthService } from '@auth0/auth0-angular';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';

@Component({
  selector: 'app-coach-create-session',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="dashboard-container">
        
        <div class="lc-hero">
          <h1 class="glitch-title">CREATE_<span>NEW_SESSION</span></h1>
          <p class="hero-desc">Schedule a new neural synchronization slot for the participant network.</p>
        </div>

        <div class="form-card-wrapper">
          <form (ngSubmit)="onSubmit()" #sessionForm="ngForm" class="cyber-form">
            <div class="form-grid">
              
              <div class="form-group full-width">
                <label>SESSION_TITLE <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input type="text" name="title" [(ngModel)]="newSession.title" required 
                         placeholder="e.g. ADVANCED_STATE_MANAGEMENT" class="cyber-input" />
                  <div class="input-glow"></div>
                </div>
              </div>

              <div class="form-group full-width">
                <label>MISSION_OBJECTIVES <span class="req">*</span></label>
                <div class="input-wrapper">
                  <textarea name="description" [(ngModel)]="newSession.description" required rows="4" 
                            placeholder="Detail what learners will build or learn..." class="cyber-input"></textarea>
                  <div class="input-glow"></div>
                </div>
              </div>

              <div class="form-group">
                <label>COGNITIVE_LANGUAGE <span class="req">*</span></label>
                <div class="input-wrapper">
                  <select name="language" [(ngModel)]="newSession.language" required class="cyber-select">
                    <option value="JAVA">JAVA</option>
                    <option value="PYTHON">PYTHON</option>
                    <option value="JAVASCRIPT">JAVASCRIPT</option>
                    <option value="ANGULAR">ANGULAR</option>
                    <option value="DOTNET">.NET</option>
                  </select>
                  <div class="input-glow"></div>
                </div>
              </div>

              <div class="form-group">
                <label>DIFFICULTY_LEVEL <span class="req">*</span></label>
                <div class="input-wrapper">
                  <select name="level" [(ngModel)]="newSession.level" required class="cyber-select">
                    <option value="BASIQUE">BASIQUE</option>
                    <option value="INTERMEDIAIRE">INTERMÉDIAIRE</option>
                    <option value="AVANCE">AVANCÉ</option>
                  </select>
                  <div class="input-glow"></div>
                </div>
              </div>

              <div class="form-group">
                <label>UPLINK_TIMESTAMP <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input type="datetime-local" name="scheduledAt" [(ngModel)]="newSession.scheduledAt" 
                         [min]="minDate" required class="cyber-input" />
                  <div class="input-glow"></div>
                </div>
              </div>

              <div class="form-group">
                <label>DURATION_MS_MIN <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input type="number" name="durationMinutes" [(ngModel)]="newSession.durationMinutes" required min="15" max="300" class="cyber-input" />
                  <div class="input-glow"></div>
                </div>
              </div>

              <div class="form-group">
                <label>MAX_OPERATORS <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input type="number" name="maxParticipants" [(ngModel)]="newSession.maxParticipants" required min="1" max="100" class="cyber-input" />
                  <div class="input-glow"></div>
                </div>
              </div>

              <div class="form-group full-width">
                <label>MISSION_UPLINK_URL (GOOGLE_MEET) <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input type="url" name="meetingUrl" [(ngModel)]="newSession.meetingUrl" required 
                         placeholder="https://meet.google.com/..." class="cyber-input" />
                  <div class="input-glow"></div>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <button type="button" class="action-neon-btn secondary" (click)="cancel()">[ CANCEL_UPLINK ]</button>
              <button type="submit" class="action-neon-btn" [disabled]="!sessionForm.form.valid || loading">
                <span *ngIf="loading" class="spinner-small"></span>
                <span *ngIf="!loading">INITIALIZE_SESSION</span>
              </button>
            </div>
          </form>
        </div>

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

    .dashboard-container { max-width: 1000px; margin: 0 auto; padding: 4rem 2rem; position: relative; z-index: 1; }
    
    .lc-hero { text-align: center; margin-bottom: 5rem; }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 0.5rem; }
    .glitch-title span { color: var(--neon-red); text-shadow: 0 0 15px var(--neon-red); }
    .hero-desc { font-size: 1rem; color: var(--muted); max-width: 600px; margin: 0 auto; line-height: 1.6; letter-spacing: 0.5px; text-transform: uppercase; }

    .form-card-wrapper {
      background: var(--card); border: 1px solid var(--border); padding: 4rem;
      clip-path: polygon(40px 0, 100% 0, 100% calc(100% - 40px), calc(100% - 40px) 100%, 0 100%, 0 40px);
      position: relative; overflow: hidden;
    }
    .form-card-wrapper::after { content: ''; position: absolute; top: 0; right: 0; width: 300px; height: 300px; background: var(--neon); filter: blur(150px); opacity: 0.05; }

    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2.5rem; margin-bottom: 3rem; }
    .full-width { grid-column: 1 / -1; }
    
    .form-group { display: flex; flex-direction: column; gap: 0.8rem; }
    .form-group label { color: var(--muted); font-size: 10px; font-family: 'Orbitron', monospace; letter-spacing: 2px; font-weight: 700; }
    .req { color: var(--neon-red); }
    
    .input-wrapper { position: relative; }
    .cyber-input, .cyber-select {
      background: rgba(0,0,0,0.4); border: 1px solid var(--border); color: #fff; padding: 1.2rem;
      font-family: 'Fira Code', monospace; font-size: 0.9rem;
      transition: all 0.3s; width: 100%; box-sizing: border-box; outline: none;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }
    .cyber-input:focus, .cyber-select:focus { border-color: var(--neon2); }
    .input-glow { position: absolute; bottom: 0; left: 10px; right: 10px; height: 1px; background: var(--neon2); transform: scaleX(0); transition: transform 0.3s; box-shadow: 0 0 10px var(--neon2); }
    .cyber-input:focus ~ .input-glow, .cyber-select:focus ~ .input-glow { transform: scaleX(1); }
    
    .form-actions { display: flex; justify-content: flex-end; gap: 2rem; border-top: 1px solid var(--border); padding-top: 3rem; }
    
    .action-neon-btn { 
      background: var(--neon-red); color: #fff; border: none; padding: 15px 35px; font-family: 'Orbitron', monospace; font-size: 11px; font-weight: 900; 
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); cursor: pointer; transition: all 0.3s; letter-spacing: 2px;
    }
    .action-neon-btn:hover:not(:disabled) { background: #fff; color: #000; box-shadow: 0 0 25px rgba(255,255,255,0.3); transform: translateY(-2px); }
    .action-neon-btn:disabled { opacity: 0.3; cursor: not-allowed; }
    
    .action-neon-btn.secondary { background: transparent; border: 1px solid var(--border); color: var(--muted); }
    .action-neon-btn.secondary:hover { border-color: var(--text); color: var(--text); }

    .spinner-small {
      display: inline-block; width: 1.2rem; height: 1.2rem;
      border: 3px solid rgba(255,255,255,0.3); border-radius: 50%;
      border-top-color: #fff; animation: spin 1s ease-in-out infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    @media (max-width: 768px) {
      .form-grid { grid-template-columns: 1fr; }
      .form-card-wrapper { padding: 2rem; }
    }
  `]
})
export class CoachCreateSessionComponent {
  newSession: Partial<CoachingSession> = {
    title: '',
    description: '',
    language: 'JAVA',
    level: 'INTERMEDIAIRE',
    scheduledAt: '',
    durationMinutes: 60,
    maxParticipants: 10,
    meetingUrl: ''
  };

  loading = false;
  coachUserId = '';
  minDate = '';

  constructor(
    private coachingService: CoachingService,
    private auth: AuthService,
    private router: Router
  ) {
    this.minDate = new Date().toISOString().slice(0, 16);
    this.auth.user$.subscribe((user: any) => {
      if (user && user.sub) {
        this.coachUserId = user.sub;
      }
    });
  }

  onSubmit() {
    this.loading = true;
    this.newSession.coachId = this.coachUserId;

    this.coachingService.createSession(this.newSession).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/coaching-quiz/coach-reservations']);
      },
      error: () => {
        this.loading = false;
        alert('Erreur lors de la création de la session. Vérifiez vos droits ou la connexion.');
      }
    });
  }

  cancel() {
    this.router.navigate(['/coaching-quiz/coach-dashboard']);
  }
}
