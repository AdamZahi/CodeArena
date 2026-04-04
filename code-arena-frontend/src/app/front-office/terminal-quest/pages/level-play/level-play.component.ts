import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { TimerAudioService } from '../../services/timer-audio.service';
import { StoryMission, SubmitAnswerResponse } from '../../models/terminal-quest.model';

interface TerminalLine {
  text: string;
  type: 'input' | 'success' | 'error' | 'info';
}

@Component({
  selector: 'app-level-play',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './level-play.component.html',
  styleUrls: ['./level-play.component.css']
})
export class LevelPlayComponent implements OnInit, OnDestroy {
  @ViewChild('terminalOutput') terminalOutput!: ElementRef<HTMLDivElement>;

  mission: StoryMission | null = null;
  commandHistory: TerminalLine[] = [];
  currentCommand = '';
  isCorrect: boolean | null = null;
  result: SubmitAnswerResponse | null = null;
  showHint = false;
  isLoading = true;
  isSubmitting = false;
  speedBonus = false;
  isTimeOut = false;

  timeRemaining = 0;
  totalTime = 0;
  timerInterval: ReturnType<typeof setInterval> | null = null;
  isTimeCritical = false;
  isDanger = false;
  colonBlink = false;

  readonly ledRange = [1, 2, 3, 4, 5];
  readonly userId = 'test-user-001';
  private missionId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tqService: TerminalQuestService,
    private audio: TimerAudioService
  ) {}

  ngOnInit(): void {
    this.missionId = this.route.snapshot.paramMap.get('levelId') ?? '';
    if (!this.missionId) { this.isLoading = false; return; }

    this.tqService.getMissionById(this.missionId).subscribe({
      next: (mission) => {
        this.mission = mission;
        this.isLoading = false;
        this.addLine('System ready. Type your command below.', 'info');
        this.totalTime = this.audio.getTimeForDifficulty(mission.difficulty, mission.isBoss);
        this.timeRemaining = this.totalTime;
        this.startTimer();
      },
      error: () => {
        this.isLoading = false;
        this.addLine('Failed to load mission.', 'error');
      }
    });
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  private startTimer(): void {
    this.clearTimer();
    this.timerInterval = setInterval(() => {
      if (this.isCorrect === true || this.isTimeOut) {
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
      }

      if (this.timeRemaining <= 0) {
        this.timeRemaining = 0;
        this.isTimeOut = true;
        this.clearTimer();
        this.audio.playGameOverSound();
      }
    }, 1000);
  }

  private clearTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  getMinutes(): string {
    return Math.floor(this.timeRemaining / 60).toString().padStart(2, '0');
  }

  getSeconds(): string {
    return (this.timeRemaining % 60).toString().padStart(2, '0');
  }

  retryMission(): void {
    this.isTimeOut = false;
    this.isTimeCritical = false;
    this.isDanger = false;
    this.colonBlink = false;
    this.currentCommand = '';
    this.commandHistory = [];
    this.addLine('System ready. Type your command below.', 'info');
    this.timeRemaining = this.totalTime;
    this.startTimer();
  }

  executeCommand(): void {
    const cmd = this.currentCommand.trim();
    if (!cmd || this.isSubmitting || !this.mission || this.isCorrect === true || this.isTimeOut) return;

    this.addLine(`$ ${cmd}`, 'input');
    this.currentCommand = '';
    this.isSubmitting = true;
    this.scrollTerminal();

    this.tqService.submitMissionAnswer(this.missionId, this.userId, cmd).subscribe({
      next: (res) => {
        this.result = res;
        this.isCorrect = res.correct;
        this.isSubmitting = false;

        if (res.correct) {
          this.clearTimer();
          this.speedBonus = this.timeRemaining > this.totalTime * 0.8;
          this.audio.playSuccessSound();
        } else {
          this.audio.playErrorSound();
        }

        this.addLine(res.correct ? `✓ ${res.message}` : `✗ ${res.message}`, res.correct ? 'success' : 'error');
        this.scrollTerminal();
      },
      error: () => {
        this.isSubmitting = false;
        this.addLine('✗ Connection error. Try again.', 'error');
        this.scrollTerminal();
      }
    });
  }

  toggleHint(): void {
    this.showHint = !this.showHint;
  }

  backToMap(): void {
    this.router.navigate(['/terminal-quest/story']);
  }

  getStarDisplay(stars: number): string[] {
    return [1, 2, 3].map(i => i <= stars ? '★' : '☆');
  }

  private addLine(text: string, type: TerminalLine['type']): void {
    this.commandHistory.push({ text, type });
  }

  private scrollTerminal(): void {
    setTimeout(() => {
      if (this.terminalOutput?.nativeElement) {
        this.terminalOutput.nativeElement.scrollTop = this.terminalOutput.nativeElement.scrollHeight;
      }
    }, 50);
  }
}
