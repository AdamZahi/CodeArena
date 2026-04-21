import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CoachingService } from '../../services/coaching.service';
import { AlertService } from '../../services/alert.service';
import { CoachApplication } from '../../models/coaching-session.model';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-coach-apply',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="dashboard-container">
        
        <div class="lc-hero">
          <h1 class="glitch-title">BECOME_A_<span>COACH</span></h1>
          <p class="hero-desc">Submit your credentials and join the elite mentor network. Your application will be reviewed by the admin.</p>
        </div>

        <!-- ALREADY SUBMITTED -->
        <div class="status-panel" *ngIf="existingApplication && existingApplication.status !== 'NONE'">
          <div class="status-card" [ngClass]="existingApplication.status?.toLowerCase()">
            <div class="status-header">
              <div class="status-icon-box">
                <span *ngIf="existingApplication.status === 'PENDING'">⏳</span>
                <span *ngIf="existingApplication.status === 'APPROVED'">✅</span>
                <span *ngIf="existingApplication.status === 'REJECTED'">❌</span>
              </div>
              <div>
                <h2>APPLICATION_STATUS: <span class="status-text">{{ existingApplication.status }}</span></h2>
                <p class="status-date" *ngIf="existingApplication.createdAt">Submitted: {{ existingApplication.createdAt | date:'medium' }}</p>
              </div>
            </div>

            <div class="status-body" *ngIf="existingApplication.status === 'PENDING'">
              <p>Your application is under review by the administrator. You will be notified once a decision has been made.</p>
              <div class="eval-strip">AWAITING_ADMIN_REVIEW...</div>
            </div>

            <div class="status-body" *ngIf="existingApplication.status === 'APPROVED'">
              <p>Congratulations! You have been approved as a coach. You can now access your Coach Dashboard and create sessions.</p>
              <button class="action-neon-btn green" (click)="goToDashboard()">ACCESS_COACH_DASHBOARD</button>
            </div>

            <div class="status-body" *ngIf="existingApplication.status === 'REJECTED'">
              <p>Your application was not approved. You may improve your qualifications and reapply.</p>
              <p class="admin-note" *ngIf="existingApplication.adminNote">Admin note: "{{ existingApplication.adminNote }}"</p>
              <button class="action-neon-btn" (click)="resetForm()">REAPPLY</button>
            </div>
          </div>
        </div>

        <!-- APPLICATION FORM -->
        <div class="form-card-wrapper" *ngIf="!existingApplication || existingApplication.status === 'NONE' || showReapply">
          <form (ngSubmit)="onSubmit()" #applyForm="ngForm" class="cyber-form">
            
            <div class="form-section-title">
              <span class="section-icon">📋</span>
              <h3>OPERATOR_CREDENTIALS</h3>
            </div>

            <div class="form-grid">
              <div class="form-group full-width">
                <label>FULL_NAME <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input type="text" name="applicantName" [(ngModel)]="application.applicantName" required 
                         placeholder="Enter your full name" class="cyber-input" />
                  <div class="input-glow"></div>
                </div>
              </div>

              <div class="form-group full-width">
                <label>CONTACT_EMAIL <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input type="email" name="applicantEmail" [(ngModel)]="application.applicantEmail" required
                         placeholder="your.email@example.com" class="cyber-input" />
                  <div class="input-glow"></div>
                </div>
              </div>
            </div>

            <div class="form-section-title mt-4">
              <span class="section-icon">📎</span>
              <h3>UPLOAD_CV_DOCUMENT</h3>
              <p class="section-sub">Attach your CV as a PDF file. Include your experience, certifications, skills, and educational background.</p>
            </div>

            <div class="form-group full-width">
              <div class="file-upload-zone" (click)="triggerFileInput()" (dragover)="onDragOver($event)" (drop)="onDrop($event)" [class.has-file]="application.cvFileName">
                <input type="file" id="pdfUpload" #fileInput (change)="onFileSelected($event)" accept="application/pdf" class="file-input-hidden" />
                <div class="upload-icon" *ngIf="!application.cvFileName && !fileLoading">📄</div>
                <div class="upload-icon spinning" *ngIf="fileLoading">⏳</div>
                <div class="upload-icon success" *ngIf="application.cvFileName && !fileLoading">✅</div>
                <p class="upload-main-text" *ngIf="!application.cvFileName && !fileLoading">DROP_YOUR_PDF_HERE</p>
                <p class="upload-main-text" *ngIf="fileLoading">READING_FILE...</p>
                <p class="upload-main-text file-name" *ngIf="application.cvFileName && !fileLoading">{{ application.cvFileName }}</p>
                <p class="upload-sub-text" *ngIf="!application.cvFileName">or click to browse</p>
                <p class="upload-sub-text" *ngIf="application.cvFileName && !fileLoading">FILE_READY • Click to change</p>
              </div>
            </div>


            <div class="form-actions">
              <button type="submit" class="action-neon-btn" [disabled]="!applyForm.form.valid || submitting || fileLoading || !application.cvFileBase64">
                <span *ngIf="submitting" class="spinner-small"></span>
                <span *ngIf="!submitting">SUBMIT_APPLICATION</span>
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

    .dashboard-container { max-width: 900px; margin: 0 auto; padding: 4rem 2rem; position: relative; z-index: 1; }
    
    .lc-hero { text-align: center; margin-bottom: 4rem; }
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 3rem; font-weight: 900; color: var(--text); letter-spacing: 4px; margin-bottom: 0.5rem; }
    .glitch-title span { color: var(--neon3); text-shadow: 0 0 15px var(--neon3); }
    .hero-desc { font-size: 1rem; color: var(--muted); max-width: 600px; margin: 0 auto; line-height: 1.6; letter-spacing: 0.5px; }

    /* STATUS PANEL */
    .status-panel { margin-bottom: 4rem; }
    .status-card { background: var(--card); border: 1px solid var(--border); padding: 3rem;
      clip-path: polygon(30px 0, 100% 0, 100% calc(100% - 30px), calc(100% - 30px) 100%, 0 100%, 0 30px);
    }
    .status-card.pending { border-left: 3px solid #ecc94b; }
    .status-card.approved { border-left: 3px solid var(--neon3); }
    .status-card.rejected { border-left: 3px solid var(--neon-red); }

    .status-header { display: flex; align-items: center; gap: 2rem; margin-bottom: 2rem; }
    .status-icon-box { width: 60px; height: 60px; background: rgba(255,255,255,0.03); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 2rem; }
    .status-header h2 { font-family: 'Orbitron', sans-serif; font-size: 1.3rem; color: var(--text); margin: 0 0 0.5rem; letter-spacing: 2px; }
    .status-text { color: var(--neon2); }
    .status-date { color: var(--muted); font-size: 0.85rem; margin: 0; }
    .status-body p { color: var(--muted); line-height: 1.6; margin-bottom: 1.5rem; }
    .admin-note { color: var(--neon-red) !important; font-style: italic; }

    /* FORM */
    .form-card-wrapper {
      background: var(--card); border: 1px solid var(--border); padding: 4rem;
      clip-path: polygon(40px 0, 100% 0, 100% calc(100% - 40px), calc(100% - 40px) 100%, 0 100%, 0 40px);
      position: relative; overflow: hidden;
    }
    .form-card-wrapper::after { content: ''; position: absolute; top: 0; right: 0; width: 300px; height: 300px; background: var(--neon3); filter: blur(150px); opacity: 0.04; }

    .form-section-title { display: flex; align-items: center; gap: 1rem; margin-bottom: 2rem; flex-wrap: wrap; }
    .form-section-title h3 { font-family: 'Orbitron', sans-serif; color: var(--text); font-size: 1.2rem; margin: 0; letter-spacing: 2px; }
    .section-icon { font-size: 1.5rem; }
    .section-sub { color: var(--muted); font-size: 0.9rem; width: 100%; margin: 0.5rem 0 0 2.5rem; }
    .mt-4 { margin-top: 3rem; }

    .form-grid { display: grid; grid-template-columns: 1fr; gap: 2rem; margin-bottom: 2rem; }
    .full-width { grid-column: 1 / -1; }
    
    .form-group { display: flex; flex-direction: column; gap: 0.8rem; }
    .form-group label { color: var(--muted); font-size: 10px; font-family: 'Orbitron', monospace; letter-spacing: 2px; font-weight: 700; }
    .req { color: var(--neon-red); }
    
    .input-wrapper { position: relative; }
    .cyber-input {
      background: rgba(0,0,0,0.4); border: 1px solid var(--border); color: #fff; padding: 1.2rem;
      font-family: 'Fira Code', monospace; font-size: 0.9rem;
      transition: all 0.3s; width: 100%; box-sizing: border-box; outline: none;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }
    .cv-textarea { min-height: 300px; resize: vertical; line-height: 1.6; clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); }
    .cyber-input:focus { border-color: var(--neon3); }
    .input-glow { position: absolute; bottom: 0; left: 10px; right: 10px; height: 1px; background: var(--neon3); transform: scaleX(0); transition: transform 0.3s; box-shadow: 0 0 10px var(--neon3); }
    .cyber-input:focus ~ .input-glow { transform: scaleX(1); }
    
    .form-actions { display: flex; justify-content: flex-end; gap: 2rem; border-top: 1px solid var(--border); padding-top: 3rem; margin-top: 3rem; }
    
    .action-neon-btn { 
      background: var(--neon3); color: #000; border: none; padding: 15px 40px; font-family: 'Orbitron', monospace; font-size: 11px; font-weight: 900; 
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%); cursor: pointer; transition: all 0.3s; letter-spacing: 2px;
    }
    .action-neon-btn:hover:not(:disabled) { background: #fff; color: #000; box-shadow: 0 0 25px rgba(255,255,255,0.3); transform: translateY(-2px); }
    .action-neon-btn:disabled { opacity: 0.3; cursor: not-allowed; }
    .action-neon-btn.green { background: var(--neon3); }

    .eval-strip { margin-top: 1.5rem; padding: 10px; background: rgba(6, 182, 212, 0.05); border: 1px solid rgba(6, 182, 212, 0.1); color: var(--neon2); font-family: 'Orbitron', monospace; font-size: 9px; text-align: center; letter-spacing: 2px; font-weight: 900; }

    .file-upload-zone {
      border: 2px dashed var(--border); background: rgba(0,0,0,0.3); padding: 3rem 2rem;
      text-align: center; cursor: pointer; transition: all 0.3s;
      clip-path: polygon(15px 0%, 100% 0%, calc(100% - 15px) 100%, 0% 100%);
    }
    .file-upload-zone:hover, .file-upload-zone.has-file { border-color: var(--neon2); background: rgba(6, 182, 212, 0.05); }
    .file-upload-zone.has-file { border-color: var(--neon3); background: rgba(16, 185, 129, 0.05); }
    .file-input-hidden { display: none; }
    .upload-icon { font-size: 3rem; margin-bottom: 1rem; }
    .upload-icon.spinning { animation: spin 1s linear infinite; }
    .upload-icon.success { color: var(--neon3); }
    .upload-main-text { font-family: 'Orbitron', sans-serif; font-size: 1.1rem; color: var(--text); letter-spacing: 2px; margin: 0 0 0.5rem; }
    .upload-main-text.file-name { color: var(--neon3); font-size: 0.95rem; word-break: break-all; }
    .upload-sub-text { color: var(--muted); font-size: 0.85rem; margin: 0; }

    .spinner-small { display: inline-block; width: 1.2rem; height: 1.2rem; border: 3px solid rgba(0,0,0,0.3); border-radius: 50%; border-top-color: #000; animation: spin 1s ease-in-out infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class CoachApplyComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef;
  application: Partial<CoachApplication> = {
    applicantName: '',
    applicantEmail: '',
    cvContent: 'See attached PDF document'
  };

  existingApplication: CoachApplication | null = null;
  submitting = false;
  submitted = false;
  showReapply = false;

  constructor(
    private coachingService: CoachingService,
    private alertService: AlertService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.auth.user$.subscribe((user: any) => {
      if (user) {
        this.application.applicantName = user.name || '';
        this.application.applicantEmail = user.email || '';
        this.checkExistingApplication();
      }
    });
  }

  checkExistingApplication() {
    this.coachingService.getMyApplicationStatus().subscribe({
      next: (data) => {
        if (data && data.status && data.status !== 'NONE') {
          this.existingApplication = data;
        }
      },
      error: () => {}
    });
  }

  onSubmit() {
    if (this.fileLoading) {
      this.alertService.info('Please wait, the PDF file is still loading...', 'FILE_LOADING');
      return;
    }
    this.submitting = true;
    console.log('Submitting application:', {
      name: this.application.applicantName,
      email: this.application.applicantEmail,
      cvContent: this.application.cvContent?.substring(0, 50),
      hasFile: !!this.application.cvFileBase64,
      fileName: this.application.cvFileName
    });
    this.coachingService.submitCoachApplication(this.application).subscribe({
      next: (res: any) => {
        this.submitting = false;
        this.showReapply = false;
        if(res && res.data) {
          this.existingApplication = res.data;
        } else {
          this.checkExistingApplication();
        }
      },
      error: (err) => {
        this.submitting = false;
        console.error('Submit error:', err);
        this.alertService.error(err.error?.message || 'Error submitting application.', 'SUBMISSION_FAILURE');
      }
    });
  }

  fileLoading = false;

  triggerFileInput() {
    this.fileInput.nativeElement.click();
  }

  onDragOver(event: any) {
    event.preventDefault();
    event.stopPropagation();
  }

  onDrop(event: any) {
    event.preventDefault();
    event.stopPropagation();
    const files = event.dataTransfer.files;
    if (files.length > 0) {
      this.processFile(files[0]);
    }
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.processFile(file);
    }
  }

  processFile(file: File) {
    if (file.type !== 'application/pdf') {
      this.alertService.warning('Please upload a valid PDF file.', 'INVALID_FILE_TYPE');
      return;
    }
    
    this.application.cvFileName = file.name;
    this.fileLoading = true;
    const reader = new FileReader();
    reader.onload = () => {
      const base64String = reader.result as string;
      this.application.cvFileBase64 = base64String;
      this.fileLoading = false;
      console.log('PDF loaded successfully. Size:', base64String.length, 'chars');
    };
    reader.onerror = () => {
      this.fileLoading = false;
      this.alertService.error('Error reading the PDF file.', 'FILE_ERROR');
    };
    reader.readAsDataURL(file);
  }

  resetForm() {
    this.showReapply = true;
    this.existingApplication = null;
  }

  goToDashboard() {
    this.router.navigate(['/coaching-quiz/coach-dashboard']);
  }
}
