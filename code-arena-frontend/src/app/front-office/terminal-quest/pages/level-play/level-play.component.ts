import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { TimerAudioService } from '../../services/timer-audio.service';
import { TtsService } from '../../services/tts.service';
import { MissionVoiceService } from '../../services/mission-voice.service';
import { CommandExplainerService } from '../../services/command-explainer.service';
import { AdaptiveLearningService } from '../../services/adaptive-learning.service';
import { AdaptivePrediction, StoryMission, SubmitAnswerResponse } from '../../models/terminal-quest.model';

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

  explanation = '';
  isExplaining = false;

  adaptivePrediction: AdaptivePrediction | null = null;
  isAdaptiveLoading = false;
  adaptiveMessage = '';

  readonly ledRange = [1, 2, 3, 4, 5];
  readonly userId = 'test-user-001';
  private missionId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tqService: TerminalQuestService,
    public audio: TimerAudioService,
    public ttsService: TtsService,
    private missionVoice: MissionVoiceService,
    private explainerService: CommandExplainerService,
    private readonly adaptiveService: AdaptiveLearningService
  ) {}

  ngOnInit(): void {
    this.missionId = this.route.snapshot.paramMap.get('levelId') ?? '';
    if (!this.missionId) { this.isLoading = false; return; }

    this.tqService.getMissionById(this.missionId).subscribe({
      next: (mission) => {
        this.mission = mission;
        this.isLoading = false;
        this.addLine('System ready. Type your command below.', 'info');
        this.missionVoice.playMissionIntro(mission);
        this.totalTime = this.audio.getTimeForDifficulty(mission.difficulty, mission.isBoss);
        this.timeRemaining = this.totalTime;
        this.startTimer();

        this.isAdaptiveLoading = true;
        this.adaptiveService.predictAdaptation(this.userId, this.missionId).subscribe({
          next: (prediction) => {
            console.log('[adaptive] response from backend:', prediction);
            this.adaptivePrediction = prediction;
            this.isAdaptiveLoading = false;
            this.totalTime = this.audio.getTimeForDifficulty(mission.difficulty, mission.isBoss) + prediction.timerAdjustment;
            this.timeRemaining = this.totalTime;
            if (prediction.showHint) {
              this.showHint = true;
            }
            if (prediction.playerLevel === 'STRUGGLING') {
              this.adaptiveMessage = '🤖 AI detects you may need help — extra time and hints enabled.';
            } else if (prediction.playerLevel === 'PROFICIENT') {
              this.adaptiveMessage = '🤖 AI challenge mode — reduced timer for experienced players.';
            }
          },
          error: () => {
            this.isAdaptiveLoading = false;
          }
        });
      },
      error: () => {
        this.isLoading = false;
        this.addLine('Failed to load mission.', 'error');
      }
    });
  }

  ngOnDestroy(): void {
    this.clearTimer();
    this.ttsService.stop();
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
      } else if (this.timeRemaining > 15) {
        this.audio.playBaseTick();
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
    this.explanation = '';
    this.isExplaining = false;
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
          this.missionVoice.playCorrectAnswer(res.starsEarned);
        } else {
          this.audio.playErrorSound();
          this.missionVoice.playWrongAnswer(res.attempts);
        }

        this.addLine(res.correct ? `✓ ${res.message}` : `✗ ${res.message}`, res.correct ? 'success' : 'error');
        this.scrollTerminal();

        this.explanation = '';
        this.isExplaining = true;
        this.explainerService.explain(cmd, this.mission!.task, this.mission!.context, this.mission!.difficulty, res.correct).subscribe({
          next: (r) => { this.explanation = r.explanation; this.isExplaining = false; },
          error: ()  => { this.isExplaining = false; }
        });
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

  getSpeakerRole(): string {
    const name = this.mission?.speakerName ?? '';
    if (name.includes('Sarah'))    return '// DevOps Lead — NexaTech';
    if (name.includes('Lina'))     return '// Cloud Architect — NexaTech';
    if (name.includes('Alex'))     return '// SRE Engineer — NexaTech';
    if (name.includes('Nadia'))    return '// Security Analyst — NexaTech';
    if (name.includes('Karim'))    return '// Platform Engineer — NexaTech';
    if (name.includes('Directeur')) return '// CTO — NexaTech';
    return '// NexaTech Engineering';
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
