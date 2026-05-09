import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CoachingService } from '../../services/coaching.service';
import { AlertService } from '../../services/alert.service';
import { Coach, SessionFeedback } from '../../models/coaching-session.model';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';

@Component({
  selector: 'app-coach-evaluate',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="evaluate-container">
        <div class="header-section">
           <button class="back-link" (click)="goBack()">[ RETURN_TO_SYSTEM ]</button>
           <h1 class="glitch-title">NEURAL_<span>CALIBRATION</span></h1>
           <p class="subtitle">Upload your performance metrics and mentor synchronization feedback to the central database.</p>
        </div>

        <div class="lc-loading" *ngIf="loading">
          <div class="spinner"></div>
          <p>ESTABLISHING_UPLINK...</p>
        </div>

        <!-- Success state -->
        <div class="success-state" *ngIf="submitted">
          <div class="success-icon-box">
            <div class="success-icon">✓</div>
            <div class="icon-glow"></div>
          </div>
          <h2 class="neon-text">UPLINK_SUCCESSFUL</h2>
          <p>Your synchronization data has been integrated into the operator's profile.</p>
          <button class="action-neon-btn" (click)="goBack()">RETURN_TO_BASE</button>
        </div>

        <div class="evaluation-card" *ngIf="!loading && coach && !submitted">
          <div class="mentor-info">
             <div class="mentor-header">
                <div class="avatar-box">
                   <div class="avatar">{{ coach.userId.substring(0, 2).toUpperCase() }}</div>
                   <div class="avatar-glow"></div>
                </div>
                <div class="mentor-title">
                   <h2>OPERATOR_<span>{{ coach.userId }}</span></h2>
                   <div class="trust-stats">
                      <div class="stars">
                         <span class="star" *ngFor="let s of getStars(coach.rating)" [class.filled]="s">★</span>
                      </div>
                      <span class="rating-val">{{ coach.rating | number:'1.1-1' }}</span>
                   </div>
                </div>
             </div>
             <p class="bio-desc">{{ coach.bio }}</p>
             <div class="specs">
                <span class="spec-tag" *ngFor="let spec of coach.specializations">{{ spec }}</span>
             </div>
          </div>

          <form class="feedback-form" (ngSubmit)="submitFeedback()">
             <div class="form-group">
               <label>SYNCHRONIZATION_SCORE</label>
               
               <div class="rating-slider-container">
                 <div class="slider-visual">
                   <div class="slider-track">
                     <div class="slider-fill" [style.width.%]="(feedback.rating / 5) * 100"></div>
                   </div>
                   <input 
                     type="range" 
                     min="0" 
                     max="5" 
                     step="0.1" 
                     [(ngModel)]="feedback.rating" 
                     name="rating"
                     class="rating-slider"
                     (input)="onSliderChange($event)">
                 </div>
                 <div class="rating-display">
                   <div class="rating-number">{{ feedback.rating | number:'1.1-1' }}</div>
                   <div class="rating-max">/ 5.0</div>
                 </div>
               </div>

               <div class="stars-display">
                 <span class="star-icon" *ngFor="let star of [1, 2, 3, 4, 5]"
                   [class.filled]="star <= feedback.rating"
                   [class.half]="star > feedback.rating && star - 1 < feedback.rating">
                   ★
                 </span>
               </div>
             </div>

             <div class="form-group">
               <label>SYNC_LOGS & COMMENTS</label>
               <textarea 
                  [(ngModel)]="feedback.comment" 
                  name="comment" 
                  rows="5" 
                  placeholder="Input detailed observation logs. Be constructive for future mission briefings." 
                  required></textarea>
               <div class="textarea-corner"></div>
             </div>

             <button type="submit" class="action-neon-btn submit" [disabled]="feedback.rating === 0 || !feedback.comment || submitting">
               {{ submitting ? 'EXECUTING_UPLINK...' : 'TRANSMIT_EVALUATION' }}
             </button>
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

    .evaluate-container { max-width: 900px; margin: 0 auto; padding: 4rem 2rem; position: relative; z-index: 1; }
    
    .header-section { margin-bottom: 4rem; text-align: left; }
    .back-link { 
      background: transparent; color: var(--muted); border: none; font-family: 'Orbitron', monospace; 
      font-size: 11px; cursor: pointer; margin-bottom: 2rem; letter-spacing: 2px; transition: all 0.3s; padding: 0;
    }
    .back-link:hover { color: var(--neon2); text-shadow: 0 0 10px var(--neon2); }

    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3.2rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin: 0 0 1rem; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 15px var(--neon); }
    .subtitle { color: var(--muted); font-size: 1.1rem; max-width: 600px; line-height: 1.6; }

    .evaluation-card { 
      background: var(--card); border: 1px solid var(--border); padding: 3rem; 
      clip-path: polygon(30px 0%, 100% 0%, 100% calc(100% - 30px), calc(100% - 30px) 100%, 0% 100%, 0% 30px);
      position: relative;
    }
    
    .mentor-info { margin-bottom: 3rem; padding-bottom: 2rem; border-bottom: 1px solid var(--border); }
    .mentor-header { display: flex; align-items: center; gap: 2rem; margin-bottom: 2rem; }
    .avatar-box { position: relative; }
    .avatar { 
      width: 70px; height: 70px; background: #000; color: var(--text); border: 2px solid var(--neon);
      display: flex; align-items: center; justify-content: center; font-size: 1.8rem; font-family: 'Orbitron', sans-serif;
      clip-path: polygon(15% 0, 100% 0, 100% 85%, 85% 100%, 0 100%, 0 15%);
    }
    .avatar-glow { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: var(--neon); filter: blur(15px); opacity: 0.1; }

    .mentor-title h2 { font-family: 'Orbitron', sans-serif; color: var(--text); font-size: 1.5rem; margin: 0 0 0.5rem; }
    .mentor-title span { color: var(--neon2); }
    .trust-stats { display: flex; align-items: center; font-family: 'Orbitron', monospace; }
    .stars { display: flex; color: var(--border); margin-right: 15px; }
    .star.filled { color: var(--neon); text-shadow: 0 0 8px var(--neon); }
    .rating-val { color: var(--neon2); font-weight: 700; font-size: 0.9rem; }
    
    .bio-desc { font-size: 1.05rem; line-height: 1.7; color: var(--text); margin-bottom: 1.5rem; font-weight: 300; }
    .specs { display: flex; flex-wrap: wrap; gap: 0.6rem; }
    .spec-tag { 
      background: rgba(255,255,255,0.03); color: var(--muted); padding: 4px 10px; border: 1px solid var(--border); 
      font-size: 9px; font-weight: 700; font-family: 'Orbitron', monospace;
    }

    .form-group { margin-bottom: 3rem; position: relative; }
    label { display: block; margin-bottom: 1.5rem; color: var(--neon); font-family: 'Orbitron', sans-serif; font-size: 0.9rem; letter-spacing: 2px; }
    
    .rating-slider-container { display: flex; align-items: center; gap: 2.5rem; margin-bottom: 1.5rem; }
    .slider-visual { flex: 1; position: relative; }
    .slider-track { height: 4px; background: var(--border); overflow: hidden; position: relative; }
    .slider-fill { height: 100%; background: linear-gradient(90deg, var(--neon), var(--neon2)); transition: width 0.1s; }
    .rating-slider { 
      position: absolute; top: -10px; left: 0; width: 100%; height: 24px;
      -webkit-appearance: none; appearance: none; background: transparent; cursor: pointer; margin: 0; z-index: 2;
    }
    .rating-slider::-webkit-slider-thumb {
      -webkit-appearance: none; width: 16px; height: 16px; background: #fff; border: 2px solid var(--neon);
      clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%); cursor: pointer;
    }
    .rating-display { text-align: center; min-width: 100px; }
    .rating-number { font-size: 2.8rem; font-weight: 900; color: var(--neon2); font-family: 'Orbitron', sans-serif; line-height: 1; text-shadow: 0 0 15px var(--neon2); }
    .rating-max { font-size: 0.8rem; color: var(--muted); font-family: 'Orbitron', monospace; }

    .stars-display { display: flex; gap: 0.5rem; font-size: 1.8rem; color: var(--border); }
    .star-icon { transition: all 0.2s; }
    .star-icon.filled { color: var(--neon); text-shadow: 0 0 12px var(--neon); }
    .star-icon.half { color: var(--neon); opacity: 0.4; }

    textarea { 
      width: 100%; background: rgba(0,0,0,0.5); border: 1px solid var(--border); padding: 1.5rem; 
      color: var(--text); font-family: 'Rajdhani', sans-serif; font-size: 1.1rem; resize: vertical; 
      transition: all 0.3s; box-sizing: border-box; min-height: 150px;
    }
    textarea:focus { outline: none; border-color: var(--neon); box-shadow: 0 0 20px rgba(139, 92, 246, 0.2); }
    .textarea-corner { position: absolute; bottom: 0; right: 0; width: 20px; height: 20px; background: var(--border); clip-path: polygon(100% 0, 100% 100%, 0 100%); pointer-events: none; }

    .action-neon-btn {
      padding: 15px 40px; font-family: 'Orbitron', monospace; font-size: 12px; font-weight: 900;
      border: none; cursor: pointer; transition: all 0.3s; background: var(--neon); color: #fff;
      clip-path: polygon(20px 0%, 100% 0%, calc(100% - 20px) 100%, 0% 100%);
      letter-spacing: 3px; position: relative; overflow: hidden;
    }
    .action-neon-btn:hover:not(:disabled) { background: var(--neon2); box-shadow: 0 0 30px var(--neon2); transform: translateY(-2px); }
    .action-neon-btn:disabled { opacity: 0.3; cursor: not-allowed; }
    .action-neon-btn.submit { width: 100%; margin-top: 1rem; }

    .lc-loading { text-align: center; padding: 6rem; font-family: 'Orbitron', monospace; color: var(--muted); letter-spacing: 3px; }
    .spinner { width: 45px; height: 45px; border: 2px solid var(--border); border-top-color: var(--neon); border-radius: 50%; margin: 0 auto 2rem; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Success state */
    .success-state { text-align: center; padding: 6rem 2rem; background: var(--card); border: 1px solid var(--neon3); clip-path: polygon(40px 0, 100% 0, 100% calc(100% - 40px), calc(100% - 40px) 100%, 0 100%, 0 40px); }
    .success-icon-box { position: relative; width: 100px; height: 100px; margin: 0 auto 3rem; }
    .success-icon { width: 100%; height: 100%; background: #000; border: 2px solid var(--neon3); color: var(--neon3); font-size: 3rem; display: flex; align-items: center; justify-content: center; clip-path: circle(50%); z-index: 2; position: relative; }
    .icon-glow { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: var(--neon3); filter: blur(30px); opacity: 0.3; }
    .neon-text { font-family: 'Orbitron', sans-serif; color: var(--neon3); font-size: 2.22rem; letter-spacing: 5px; margin-bottom: 1.5rem; text-shadow: 0 0 20px var(--neon3); }
    .success-state p { color: var(--text); font-size: 1.1rem; margin-bottom: 3rem; font-weight: 300; }
  `]
})
export class CoachEvaluateComponent implements OnInit {
  coachId: string = '';
  coach: Coach | null = null;
  loading = true;
  submitting = false;
  submitted = false;
  hoverRating = 0;

  feedback: SessionFeedback = {
    coachId: '',
    userId: 'anonymous',
    rating: 0,
    comment: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private coachingService: CoachingService,
    private alertService: AlertService
  ) { }

  ngOnInit() {
    this.coachId = this.route.snapshot.paramMap.get('coachId') || '';
    if (this.coachId) {
      this.feedback.coachId = this.coachId;
      this.loadCoach();
    } else {
      this.goBack();
    }
  }

  loadCoach() {
    this.loading = true;
    this.coachingService.getAllCoaches().subscribe({
      next: (coaches) => {
        // Find by userId
        const found = coaches.find(c => c.userId === this.coachId);
        if (found) {
          this.coach = found;
          this.loading = false;
        } else {
          // Fallback check if it was actually a UUID id
          const foundById = coaches.find(c => c.id === this.coachId);
          if (foundById) {
            this.coach = foundById;
            this.feedback.coachId = this.coach?.userId || '';
            this.coachId = this.feedback.coachId;
            this.loading = false;
          } else {
            this.alertService.error('Mentor non trouvé.', 'SIGNAL_LOST');
            this.goBack();
          }
        }
      },
      error: () => {
        this.alertService.error('Erreur lors du chargement des mentors.', 'DATA_FETCH_ERROR');
        this.goBack();
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

  onSliderChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.feedback.rating = parseFloat(parseFloat(input.value).toFixed(1));
  }

  submitFeedback() {
    if (this.feedback.rating === 0 || !this.feedback.comment) return;

    this.submitting = true;

    const payload: SessionFeedback = {
      coachId: this.feedback.coachId,
      userId: this.feedback.userId || 'anonymous',
      rating: this.feedback.rating,
      comment: this.feedback.comment
    };

    this.coachingService.submitFeedback(payload).subscribe({
      next: (response: any) => {
        if (response && response.success === false) {
          console.error('Feedback submission failed:', response.message);
          this.alertService.error('Erreur: ' + (response.message || 'Soumission échouée'), 'TRANSMISSION_FAILURE');
          this.submitting = false;
        } else {
          this.submitting = false;
          this.submitted = true;
        }
      },
      error: (err) => {
        console.error('Feedback submission error:', err);
        this.alertService.error('Erreur lors de la soumission du feedback. Veuillez réessayer.', 'UPLINK_FAILURE');
        this.submitting = false;
      }
    });
  }

  goBack() {
    this.router.navigate(['/coaching-quiz/coaches']);
  }
}
