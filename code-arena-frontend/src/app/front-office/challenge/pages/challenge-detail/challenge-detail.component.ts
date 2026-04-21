import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ChallengeService } from '../../services/challenge.service';
import { SubmissionService } from '../../services/submission.service';
import { Subscription, interval } from 'rxjs';
import { switchMap, takeWhile, take } from 'rxjs/operators';
import { AiService, ChallengeDifficultyDto } from '../../services/ai.service';
import { AuthService } from '@auth0/auth0-angular';
import { AuthUserSyncService } from '../../../../core/auth/auth-user-sync.service';

@Component({
  selector: 'app-challenge-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './challenge-detail.component.html',
  styleUrls: ['./challenge-detail.component.css']
})
export class ChallengeDetailComponent implements OnInit, OnDestroy {
  public challengeId!: string;
  public challenge: any;
  public aiDifficulty: ChallengeDifficultyDto | null = null;
  public aiHint: string | null = null;
  public isHintLoading = false;
  public isLoading = true;
  public activeTab: 'description' | 'submissions' = 'description';

  public code = '';
  public language = '62'; // Java Default
  public languages = [
    { id: '62', name: 'Java (OpenJDK 13)' },
    { id: '71', name: 'Python (3.8)' },
    { id: '50', name: 'C (GCC 9.2)' },
    { id: '54', name: 'C++ (GCC 9.2)' },
    { id: '63', name: 'JavaScript (Node 12)' }
  ];

  public isSubmitting = false;
  public submissionResult: any = null;
  public mySubmissions: any[] = [];
  public lineCount = 1;
  public languageMismatchError: string | null = null;
  public isLanguageLocked = false;
  private pollSub?: Subscription;

  // Accordion toggles
  public topicsOpen = false;
  public hintsOpen = false;
  public langInfoOpen = false;
  public companiesOpen = false;
  public similarOpen = false;
  public discussionOpen = false;

  // Gamification properties
  public maxTrials = 4;
  public trialsLeft = 4;
  public healthPercent = 100;
  public isGameOver = false;
  public isMuted = false;
  public autoReconnectTimer = 20;
  private reconnectInterval: any;

  // Discussion & Voting
  public comments: any[] = [];
  public newCommentContent = '';
  public isAddingComment = false;
  public upvotes = 0;
  public downvotes = 0;
  public userVote: string | null = null;
  public currentUserSub: string | null = null;
  public isAdmin = false;
  public antiCheatEnabled = true; // Anti-cheat ON by default

  constructor(
    private route: ActivatedRoute,
    private challengeService: ChallengeService,
    private submissionService: SubmissionService,
    private aiService: AiService,
    public auth: AuthService,
    private authUserSync: AuthUserSyncService
  ) {}

  ngOnInit(): void {
    const savedMute = localStorage.getItem('codearena_muted');
    if (savedMute) this.isMuted = savedMute === 'true';

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.challengeId = id;
      this.loadHealthState();
      this.loadChallenge();
      this.loadMySubmissions();
      this.loadVotes();
      this.loadComments();
      this.loadAiDifficulty();

      // Get Auth0 user info (for sub claim)
      this.auth.user$.subscribe(user => {
        if (user) {
          this.currentUserSub = user.sub || null;
        }
      });

      // Check admin role from backend (the real source of truth)
      this.authUserSync.currentUser$.subscribe(backendUser => {
        if (backendUser) {
          this.isAdmin = backendUser.role === 'ADMIN';
        }
      });
    }
  }

  public toggleSound(): void {
    this.isMuted = !this.isMuted;
    localStorage.setItem('codearena_muted', this.isMuted.toString());
  }

  // Per-challenge health persistence
  private getHealthKey(): string {
    return `codearena_health_${this.challengeId}`;
  }

  private getReconnectTimeKey(): string {
    return `codearena_reconnect_time_${this.challengeId}`;
  }

  private loadHealthState(): void {
    const saved = localStorage.getItem(this.getHealthKey());
    if (saved !== null) {
      this.trialsLeft = parseInt(saved, 10);
      this.healthPercent = (this.trialsLeft / this.maxTrials) * 100;
      this.isGameOver = this.trialsLeft <= 0;
      if (this.isGameOver) {
        this.startReconnectTimer();
      }
    } else {
      this.trialsLeft = this.maxTrials;
      this.healthPercent = 100;
      this.isGameOver = false;
    }
  }

  private startReconnectTimer(): void {
    const reconnectKey = this.getReconnectTimeKey();
    const reconnectTimeStr = localStorage.getItem(reconnectKey);
    let reconnectTime = reconnectTimeStr ? parseInt(reconnectTimeStr, 10) : Date.now() + 20000;
    
    if (!reconnectTimeStr) {
        localStorage.setItem(reconnectKey, reconnectTime.toString());
    }

    if (this.reconnectInterval) {
      clearInterval(this.reconnectInterval);
    }

    const initialDiff = reconnectTime - Date.now();
    this.autoReconnectTimer = initialDiff > 0 ? Math.ceil(initialDiff / 1000) : 0;

    this.reconnectInterval = setInterval(() => {
      const now = Date.now();
      const difference = reconnectTime - now;

      if (difference <= 0) {
        clearInterval(this.reconnectInterval);
        this.reestablishConnection();
      } else {
        this.autoReconnectTimer = Math.ceil(difference / 1000);
      }
    }, 1000);
  }

  private saveHealthState(): void {
    localStorage.setItem(this.getHealthKey(), this.trialsLeft.toString());
  }

  // Web Audio API synthesized Mario-style sounds (no external files needed)
  private audioCtx: AudioContext | null = null;

  private getAudioCtx(): AudioContext {
    if (!this.audioCtx) {
      this.audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
    return this.audioCtx;
  }

  private playTone(freq: number, duration: number, type: OscillatorType = 'square', vol: number = 0.15, startDelay: number = 0): void {
    if (this.isMuted) return;
    try {
      const ctx = this.getAudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = type;
      osc.frequency.value = freq;
      gain.gain.setValueAtTime(vol, ctx.currentTime + startDelay);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + startDelay + duration);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(ctx.currentTime + startDelay);
      osc.stop(ctx.currentTime + startDelay + duration);
    } catch (e) {}
  }

  private playSound(type: 'type' | 'success' | 'fail' | 'gameover'): void {
    switch (type) {
      case 'type':
        // Quick blip (like Tetris piece drop)
        this.playTone(800, 0.05, 'square', 0.08);
        break;
      case 'success':
        // Mario coin sound — ascending notes
        this.playTone(988, 0.1, 'square', 0.15, 0);
        this.playTone(1319, 0.4, 'square', 0.15, 0.1);
        break;
      case 'fail':
        // Sad descending tone
        this.playTone(494, 0.15, 'square', 0.15, 0);
        this.playTone(370, 0.15, 'square', 0.15, 0.15);
        this.playTone(294, 0.15, 'square', 0.15, 0.3);
        this.playTone(220, 0.4, 'sawtooth', 0.12, 0.45);
        break;
      case 'gameover':
        // Mario game over — dramatic descending sequence
        this.playTone(784, 0.2, 'square', 0.18, 0);
        this.playTone(622, 0.2, 'square', 0.18, 0.2);
        this.playTone(523, 0.2, 'square', 0.18, 0.4);
        this.playTone(392, 0.3, 'sawtooth', 0.15, 0.6);
        this.playTone(330, 0.3, 'sawtooth', 0.15, 0.9);
        this.playTone(262, 0.6, 'sawtooth', 0.18, 1.2);
        break;
    }
  }

  public onCodeInput(): void {
    this.updateLineNumbers();
    this.playSound('type');
    this.applyTetrisEffect();
  }

  private applyTetrisEffect(): void {
    // We'll add a CSS class to the editor box briefly
    const editor = document.querySelector('.editor-body');
    if (editor) {
      editor.classList.add('tetris-pulse');
      setTimeout(() => editor.classList.remove('tetris-pulse'), 100);
    }
  }

  ngOnDestroy(): void {
    if (this.pollSub) this.pollSub.unsubscribe();
    if (this.reconnectInterval) clearInterval(this.reconnectInterval);
  }

  public loadChallenge(): void {
    this.isLoading = true;
    this.challengeService.getById(this.challengeId).subscribe({
      next: (data) => {
        this.challenge = data;
        this.isLoading = false;
        
        // If the challenge enforces a specific programming language, map it
        if (this.challenge && this.challenge.language) {
            this.language = this.challenge.language.trim();
        }
        
        this.code = this.getBoilerplate(this.language);
        this.updateLineNumbers();

        // Lock the language dropdown if the challenge enforces a specific language
        if (this.challenge?.language) {
          this.isLanguageLocked = true;
        }
      },
      error: (e) => {
        console.error('Error loading challenge:', e);
        this.isLoading = false;
      }
    });
  }

  public loadAiDifficulty(): void {
    this.aiService.getChallengeDifficulty(+this.challengeId).subscribe({
      next: (data) => {
        this.aiDifficulty = data;
      },
      error: (e) => console.log('AI difficulty not available yet')
    });
  }

  public generateAiHint(): void {
    if (this.isHintLoading || this.aiHint) return;
    this.isHintLoading = true;
    this.aiService.getChallengeHint(+this.challengeId).subscribe({
      next: (res) => {
        this.isHintLoading = false;
        // Simulate a typing effect
        const fullHint = res.hint || 'No hint generated.';
        this.aiHint = '';
        let i = 0;
        const interval = setInterval(() => {
          this.aiHint! += fullHint.charAt(i);
          i++;
          if (i >= fullHint.length) clearInterval(interval);
        }, 30); // Typewriter speed
        this.playSound('type');
      },
      error: (e) => {
        this.isHintLoading = false;
        this.aiHint = 'Neural network offline. Hint generation failed.';
      }
    });
  }

  public onLanguageChange(): void {
    this.languageMismatchError = null;
    if (this.challenge?.language) {
      const challengeLang = this.challenge.language.toString().trim();
      if (this.language !== challengeLang) {
        const requiredName = this.getLanguageName(challengeLang);
        this.languageMismatchError = `⚠ PROTOCOL VIOLATION: This challenge requires ${requiredName}. Language has been reset.`;
        this.language = challengeLang;
        this.code = this.getBoilerplate(this.language);
        return;
      }
    }
    this.code = this.getBoilerplate(this.language);
    this.updateLineNumbers();
  }

  public updateLineNumbers(): void {
    this.lineCount = (this.code.match(/\n/g) || []).length + 1;
  }

  public getLineNumbers(): number[] {
    return Array.from({ length: Math.max(this.lineCount, 15) }, (_, i) => i + 1);
  }

  public handleTab(event: KeyboardEvent): void {
    if (event.key === 'Tab') {
      event.preventDefault();
      const textarea = event.target as HTMLTextAreaElement;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      this.code = this.code.substring(0, start) + '    ' + this.code.substring(end);
      setTimeout(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 4;
        this.updateLineNumbers();
      });
    }
  }

  public submitCode(): void {
    if (!this.code.trim() || this.isSubmitting || this.isGameOver) return;

    // Language enforcement check
    if (this.challenge?.language) {
      const requiredLang = this.challenge.language.toString().trim();
      if (this.language !== requiredLang) {
        const requiredName = this.getLanguageName(requiredLang);
        const selectedName = this.getLanguageName(this.language);
        this.submissionResult = {
          status: 'LANGUAGE_MISMATCH',
          errorOutput: `⛔ SUBMISSION BLOCKED: This challenge requires ${requiredName}, but you selected ${selectedName}. Please use the correct language.`
        };
        this.activeTab = 'submissions';
        this.playSound('fail');
        return;
      }
    }

    this.languageMismatchError = null;
    this.isSubmitting = true;
    this.submissionResult = null;
    this.activeTab = 'submissions';

    const req = {
      code: this.code,
      language: this.language,
      challengeId: this.challengeId
    };

    this.submissionService.submitCode(req).subscribe({
      next: (res: any) => {
        this.submissionResult = res;
        this.startPolling(res.id);
      },
      error: (e: any) => {
        this.isSubmitting = false;
        this.submissionResult = { status: 'ERROR', errorOutput: 'Submission link failure.' };
        this.decrementHealth();
      }
    });
  }

  private startPolling(subId: string): void {
    if (this.pollSub) this.pollSub.unsubscribe();

    this.pollSub = interval(2000).pipe(
      switchMap(() => this.submissionService.getSubmissionStatus(subId)),
      takeWhile(res => res.status === 'PENDING' || res.status === 'IN_PROGRESS', true)
    ).subscribe({
      next: (res) => {
        this.submissionResult = res;
        if (res.status !== 'PENDING' && res.status !== 'IN_PROGRESS') {
          this.isSubmitting = false;
          this.loadMySubmissions();
          
          if (res.status === 'ACCEPTED') {
            this.playSound('success');
          } else {
            this.playSound('fail');
            this.decrementHealth();
          }
        }
      },
      error: () => {
        this.isSubmitting = false;
        this.decrementHealth();
      }
    });
  }

  private decrementHealth(): void {
    if (this.isGameOver) return;
    this.trialsLeft--;
    this.healthPercent = (this.trialsLeft / this.maxTrials) * 100;
    this.saveHealthState();
    
    if (this.trialsLeft <= 0) {
      this.isGameOver = true;
      this.healthPercent = 0;
      this.saveHealthState();
      
      const reconnectKey = this.getReconnectTimeKey();
      if (!localStorage.getItem(reconnectKey)) {
        localStorage.setItem(reconnectKey, (Date.now() + 20000).toString());
      }
      
      this.playSound('gameover');
      this.startReconnectTimer();
    }
  }

  public loadMySubmissions(): void {
    if (!this.currentUserSub) return;
    this.submissionService.getUserSubmissions(this.currentUserSub).subscribe({
      next: (res: any) => {
        const currentChallengeSubs = res.filter((s: any) => s.challengeId == this.challengeId);
        this.mySubmissions = currentChallengeSubs.sort((a: any, b: any) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime());
      }
    });
  }

  public getTags(tags: string): string[] {
    if (!tags) return [];
    return tags.split(',').map(t => t.trim()).filter(t => t.length > 0);
  }

  public formatDescription(desc: string): string {
    if (!desc) return '';
    return desc.replace(/\n/g, '<br/>');
  }

  public getVisibleTestCases(): any[] {
    if (!this.challenge?.testCases) return [];
    return this.challenge.testCases.filter((tc: any) => !tc.isHidden);
  }

  public getLanguageName(id: string): string {
    return this.languages.find(l => l.id === id)?.name || id;
  }

  public viewSubmissions(): void {
    this.activeTab = 'submissions';
    this.loadMySubmissions();
  }

  private getBoilerplate(langId: string): string {
    switch (langId) {
      case '62': return 'import java.util.*;\n\nclass Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String line = sc.nextLine().trim();\n        \n        // TODO: Solve the problem here\n        \n        System.out.println(line);\n    }\n}';
      case '71': return 'import sys\n\n# Read input from stdin\nline = input().strip()\n\n# TODO: Solve the problem\n\n# Print result to stdout\nprint(line)';
      case '50': return '#include <stdio.h>\n#include <stdlib.h>\n#include <string.h>\n\nint main() {\n    char line[1024];\n    // Read input from stdin\n    fgets(line, sizeof(line), stdin);\n    \n    // TODO: Solve the problem\n    \n    // Print result to stdout\n    printf("%s", line);\n    return 0;\n}';
      case '54': return '#include <iostream>\n#include <vector>\n#include <string>\nusing namespace std;\n\nint main() {\n    string line;\n    // Read input from stdin\n    getline(cin, line);\n    \n    // TODO: Solve the problem\n    \n    // Print result to stdout\n    cout << line << endl;\n    return 0;\n}';
      case '63': return '// Read input from stdin\nconst input = require("fs").readFileSync("/dev/stdin", "utf8").trim();\nconst lines = input.split("\\n");\n\n// TODO: Solve the problem\n\n// Print result to stdout\nconsole.log(lines[0]);';
      default: return '// Write your solution here\n// Read from stdin, print to stdout\n';
    }
  }

  // --- Voting Methods ---
  public loadVotes(): void {
    this.challengeService.getVotes(+this.challengeId).subscribe({
      next: (res) => {
        this.upvotes = res.upvotes;
        this.downvotes = res.downvotes;
        this.userVote = res.userVote;
      }
    });
  }

  public upvote(): void {
    this.challengeService.upvote(+this.challengeId).subscribe({
      next: (res) => {
        this.upvotes = res.upvotes;
        this.downvotes = res.downvotes;
        this.userVote = res.userVote;
      }
    });
  }

  public downvote(): void {
    this.challengeService.downvote(+this.challengeId).subscribe({
      next: (res) => {
        this.upvotes = res.upvotes;
        this.downvotes = res.downvotes;
        this.userVote = res.userVote;
      }
    });
  }

  // --- Discussion Methods ---
  public loadComments(): void {
    this.challengeService.getComments(+this.challengeId).subscribe({
      next: (res) => this.comments = res
    });
  }

  public postComment(): void {
    if (!this.newCommentContent.trim() || this.isAddingComment) return;
    this.isAddingComment = true;

    // Get the display name from Auth0 (same as in header profile)
    this.auth.user$.pipe(take(1)).subscribe((user: any) => {
      const displayName = user?.nickname || user?.name || 'User';
      
      this.challengeService.addComment(+this.challengeId, this.newCommentContent, displayName).subscribe({
        next: (res) => {
          this.comments.unshift(res);
          this.newCommentContent = '';
          this.isAddingComment = false;
        },
        error: () => this.isAddingComment = false
      });
    });
  }

  public deleteComment(commentId: number): void {
    if (!confirm('Are you sure you want to delete this comment?')) return;
    this.challengeService.deleteComment(commentId).subscribe({
      next: () => {
        this.comments = this.comments.filter(c => c.id !== commentId);
      }
    });
  }

  public reestablishConnection(): void {
    this.trialsLeft = this.maxTrials;
    this.healthPercent = 100;
    this.isGameOver = false;
    this.saveHealthState();
    localStorage.removeItem(this.getReconnectTimeKey());
    window.location.reload();
  }

  public toggleAntiCheat(): void {
    this.antiCheatEnabled = !this.antiCheatEnabled;
  }

  public blockEvent(event: Event): void {
    if (this.antiCheatEnabled) {
      event.preventDefault();
    }
  }
}
