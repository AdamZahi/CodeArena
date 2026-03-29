import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { SurvivalSession, StoryLevel } from '../../models/terminal-quest.model';

interface TerminalLine {
  text: string;
  type: 'input' | 'success' | 'error' | 'info';
}

@Component({
  selector: 'app-survival-play',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './survival-play.component.html',
  styleUrls: ['./survival-play.component.css']
})
export class SurvivalPlayComponent implements OnInit {
  @ViewChild('terminalOutput') terminalOutput!: ElementRef<HTMLDivElement>;

  session: SurvivalSession | null = null;
  currentChallenge: StoryLevel | null = null;
  commandHistory: TerminalLine[] = [];
  currentCommand = '';
  lives = 3;
  wave = 1;
  score = 0;
  gameOver = false;
  isSubmitting = false;
  isLoading = true;
  heartsShake = false;
  gameOverStats: { wave: number; score: number; message: string } | null = null;

  readonly userId = 'test-user-001';
  readonly heartRange = [1, 2, 3];

  constructor(
    private tqService: TerminalQuestService,
    private router: Router
  ) {}

  ngOnInit(): void {
    console.log('ngOnInit called');
    this.startGame();
  }

  private startGame(): void {
    this.isLoading = true;
    console.log('starting session...');
    this.tqService.startSurvivalSession(this.userId).subscribe({
      next: (session) => {
        console.log('session started:', session);
        this.session = session;
        this.lives = session.livesRemaining;
        this.wave = session.waveReached;
        this.score = session.score;
        this.loadFirstChallenge();
      },
      error: (err) => {
        console.log('error:', err);
        this.isLoading = false;
        this.addLine('Failed to start session. Try again.', 'error');
      }
    });
  }

  private loadFirstChallenge(): void {
    this.tqService.getChapters().subscribe({
      next: (chapters) => {
        console.log('chapters loaded:', chapters);
        const allLevels = chapters.flatMap(c => c.levels || []);
        console.log('allLevels:', allLevels);
        if (allLevels.length > 0) {
          this.currentChallenge = allLevels[Math.floor(Math.random() * allLevels.length)];
        }
        this.isLoading = false;
        this.addLine('Session started — 3 lives remaining. Good luck.', 'info');
      },
      error: (err) => {
        console.log('chapters error:', err);
        this.isLoading = false;
        this.addLine('Session started. Type your command.', 'info');
      }
    });
  }

  executeCommand(): void {
    const cmd = this.currentCommand.trim();
    if (!cmd || this.isSubmitting || !this.session || !this.currentChallenge || this.gameOver) return;

    this.addLine(`$ ${cmd}`, 'input');
    this.currentCommand = '';
    this.isSubmitting = true;
    this.scrollTerminal();

    this.tqService.submitSurvivalAnswer(this.session.id, this.userId, this.currentChallenge.id, cmd).subscribe({
      next: (res) => {
        this.lives = res.livesRemaining;
        this.wave = res.waveReached;
        this.score = res.score;
        this.isSubmitting = false;

        if (res.correct) {
          const pts = res.waveReached * 10;
          this.addLine(`✓ Correct! +${pts} pts — Wave ${res.waveReached}`, 'success');
          this.scrollTerminal();
          setTimeout(() => {
            this.commandHistory = [];
            this.currentChallenge = res.nextChallenge;
            if (this.currentChallenge) {
              this.addLine('New challenge. Type your command.', 'info');
            }
          }, 900);
        } else {
          this.addLine('✗ Wrong answer! −1 life', 'error');
          this.triggerHeartShake();
          this.scrollTerminal();
          if (res.gameOver) {
            setTimeout(() => {
              this.gameOverStats = {
                wave: res.waveReached,
                score: res.score,
                message: `You reached wave ${res.waveReached}!`
              };
              this.gameOver = true;
            }, 600);
          }
        }
      },
      error: () => {
        this.isSubmitting = false;
        this.addLine('Connection error. Try again.', 'error');
        this.scrollTerminal();
      }
    });
  }

  abandon(): void {
    if (!this.session) return;
    this.tqService.endSurvivalSession(this.session.id).subscribe({
      next: (ended) => {
        this.gameOverStats = { wave: ended.waveReached, score: ended.score, message: 'Session abandoned.' };
        this.gameOver = true;
      },
      error: () => {
        this.gameOverStats = { wave: this.wave, score: this.score, message: 'Session abandoned.' };
        this.gameOver = true;
      }
    });
  }

  playAgain(): void {
    this.session = null;
    this.currentChallenge = null;
    this.commandHistory = [];
    this.currentCommand = '';
    this.lives = 3;
    this.wave = 1;
    this.score = 0;
    this.gameOver = false;
    this.gameOverStats = null;
    this.heartsShake = false;
    this.startGame();
  }

  goToLeaderboard(): void {
    this.router.navigate(['/terminal-quest/survival/leaderboard']);
  }

  private addLine(text: string, type: TerminalLine['type']): void {
    this.commandHistory.push({ text, type });
  }

  private triggerHeartShake(): void {
    this.heartsShake = true;
    setTimeout(() => { this.heartsShake = false; }, 500);
  }

  private scrollTerminal(): void {
    setTimeout(() => {
      if (this.terminalOutput?.nativeElement) {
        this.terminalOutput.nativeElement.scrollTop = this.terminalOutput.nativeElement.scrollHeight;
      }
    }, 50);
  }
}
