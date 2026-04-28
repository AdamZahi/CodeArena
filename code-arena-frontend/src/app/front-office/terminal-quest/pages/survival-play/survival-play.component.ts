import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { filter, take } from 'rxjs/operators';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { TimerAudioService } from '../../services/timer-audio.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';
import { VoiceNavWidgetComponent } from '../../components/voice-nav-widget/voice-nav-widget.component';
import { SurvivalSession, StoryLevel } from '../../models/terminal-quest.model';

interface TerminalLine {
  text: string;
  type: 'input' | 'success' | 'error' | 'info';
}

@Component({
  selector: 'app-survival-play',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, VoiceNavWidgetComponent],
  templateUrl: './survival-play.component.html',
  styleUrls: ['./survival-play.component.css']
})
export class SurvivalPlayComponent implements OnInit, OnDestroy {
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
  showHint = false;

  timeRemaining = 0;
  totalTime = 0;
  timerInterval: ReturnType<typeof setInterval> | null = null;
  isTimeCritical = false;
  isDanger = false;
  colonBlink = false;

  private pendingTimeouts: ReturnType<typeof setTimeout>[] = [];

  readonly ledRange = [1, 2, 3, 4, 5];
  userId = '';
  readonly heartRange = [1, 2, 3];

  constructor(
    private readonly auth: AuthService,
    private tqService: TerminalQuestService,
    private router: Router,
    public audio: TimerAudioService,
    private readonly voiceNav: VoiceNavigationService
  ) {}

  ngOnInit(): void {
    this.auth.user$.pipe(
      filter(u => !!u),
      take(1)
    ).subscribe(user => {
      this.userId = user?.sub ?? '';
      this.startGame();
    });

    this.voiceNav.registerPageCommands('survival-play', (cmd: string) => {
      if (cmd === 'hint' || cmd === 'indice') {
        this.showHint = true;
        this.voiceNav.feedback$.next('Hint shown');
        return true;
      }
      if (cmd === 'quit' || cmd === 'quitter') {
        this.abandon();
        return true;
      }
      return false;
    });
    this.voiceNav.autoStart();
  }

  ngOnDestroy(): void {
    this.clearTimer();
    this.pendingTimeouts.forEach(id => clearTimeout(id));
    this.voiceNav.unregisterPageCommands('survival-play');
  }

  private startTimer(): void {
    this.clearTimer();
    if (!this.currentChallenge || this.gameOver) return;

    this.totalTime = this.audio.getTimeForDifficulty(this.currentChallenge.difficulty, this.currentChallenge.isBoss);
    this.timeRemaining = this.totalTime;
    this.isTimeCritical = false;
    this.isDanger = false;
    this.colonBlink = false;

    this.timerInterval = setInterval(() => {
      if (this.gameOver) {
        this.clearTimer();
        return;
      }

      this.timeRemaining--;
      this.colonBlink = !this.colonBlink;

      if (this.timeRemaining <= 5 && this.timeRemaining > 0) {
        this.isTimeCritical = true;
        this.isDanger = true;
        this.audio.playUrgentTick();
      } else if (this.timeRemaining <= 15 && this.timeRemaining > 5) {
        this.isTimeCritical = true;
        this.audio.playTick();
      } else if (this.timeRemaining > 15) {
        this.audio.playBaseTick();
      }

      if (this.timeRemaining <= 0) {
        this.timeRemaining = 0;
        this.clearTimer();
        this.onTimeOut();
      }
    }, 1000);
  }

  private clearTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  private scheduleTransition(callback: () => void, delay: number): void {
    const id = setTimeout(() => {
      this.pendingTimeouts = this.pendingTimeouts.filter(t => t !== id);
      callback();
    }, delay);
    this.pendingTimeouts.push(id);
  }

  getMinutes(): string {
    return Math.floor(this.timeRemaining / 60).toString().padStart(2, '0');
  }

  getSeconds(): string {
    return (this.timeRemaining % 60).toString().padStart(2, '0');
  }

  private onTimeOut(): void {
    this.audio.playErrorSound();
    this.lives--;
    this.addLine("⏰ Time's up! -1 ❤️", 'error');
    this.triggerHeartShake();
    this.scrollTerminal();

    if (this.lives <= 0) {
      this.audio.playGameOverSound();
      this.scheduleTransition(() => {
        this.gameOverStats = { wave: this.wave, score: this.score, message: `You reached wave ${this.wave}!` };
        this.gameOver = true;
      }, 800);
      return;
    }

    this.scheduleTransition(() => {
      this.isTimeCritical = false;
      this.isDanger = false;
      this.colonBlink = false;
      this.timeRemaining = this.totalTime;
      this.addLine('Timer reset — try again!', 'info');
      this.scrollTerminal();
      this.startTimer();
    }, 1200);
  }

  private startGame(): void {
    this.isLoading = true;
    this.tqService.startSurvivalSession().subscribe({
      next: (session) => {
        this.session = session;
        this.lives = session.livesRemaining;
        this.wave = session.waveReached;
        this.score = session.score;
        this.loadFirstChallenge();
      },
      error: () => {
        this.isLoading = false;
        this.addLine('Failed to start session. Try again.', 'error');
      }
    });
  }

  private loadFirstChallenge(): void {
    this.tqService.getChapters().subscribe({
      next: (chapters) => {
        const allLevels = chapters.flatMap(c => c.levels || []);
        if (allLevels.length > 0) {
          this.currentChallenge = allLevels[Math.floor(Math.random() * allLevels.length)];
        }
        this.isLoading = false;
        this.addLine('Session started — 3 lives remaining. Good luck.', 'info');
        if (this.currentChallenge) {
          this.startTimer();
        }
      },
      error: () => {
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

    this.tqService.submitSurvivalAnswer(this.session.id, this.currentChallenge.id, cmd).subscribe({
      next: (res) => {
        this.lives = res.livesRemaining;
        this.wave = res.waveReached;
        this.score = res.score;
        this.isSubmitting = false;

        if (res.correct) {
          this.clearTimer();
          this.audio.playSuccessSound();
          const pts = res.waveReached * 10;
          this.addLine(`✓ Correct! +${pts} pts — Wave ${res.waveReached}`, 'success');
          this.scrollTerminal();
          this.scheduleTransition(() => {
            this.commandHistory = [];
            this.showHint = false;
            this.currentChallenge = res.nextChallenge;
            if (this.currentChallenge) {
              this.addLine('New challenge. Type your command.', 'info');
              this.startTimer();
            }
          }, 900);
        } else {
          this.audio.playErrorSound();
          this.addLine('✗ Wrong answer! −1 life', 'error');
          this.triggerHeartShake();
          this.scrollTerminal();
          if (res.gameOver) {
            this.clearTimer();
            this.audio.playGameOverSound();
            this.scheduleTransition(() => {
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
    this.clearTimer();
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
    this.clearTimer();
    this.pendingTimeouts.forEach(id => clearTimeout(id));
    this.pendingTimeouts = [];
    this.session = null;
    this.currentChallenge = null;
    this.commandHistory = [];
    this.currentCommand = '';
    this.lives = 3;
    this.wave = 1;
    this.score = 0;
    this.gameOver = false;
    this.gameOverStats = null;
    this.isTimeCritical = false;
    this.isDanger = false;
    this.colonBlink = false;
    this.showHint = false;
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
