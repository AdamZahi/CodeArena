import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TerminalQuestService } from '../../services/terminal-quest.service';
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
export class LevelPlayComponent implements OnInit {
  @ViewChild('terminalOutput') terminalOutput!: ElementRef<HTMLDivElement>;

  mission: StoryMission | null = null;
  commandHistory: TerminalLine[] = [];
  currentCommand = '';
  isCorrect: boolean | null = null;
  result: SubmitAnswerResponse | null = null;
  showHint = false;
  isLoading = true;
  isSubmitting = false;

  readonly userId = 'test-user-001';
  private missionId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tqService: TerminalQuestService
  ) {}

  ngOnInit(): void {
    this.missionId = this.route.snapshot.paramMap.get('levelId') ?? '';
    if (!this.missionId) { this.isLoading = false; return; }

    this.tqService.getMissionById(this.missionId).subscribe({
      next: (mission) => {
        this.mission = mission;
        this.isLoading = false;
        this.addLine('System ready. Type your command below.', 'info');
      },
      error: () => {
        this.isLoading = false;
        this.addLine('Failed to load mission.', 'error');
      }
    });
  }

  executeCommand(): void {
    const cmd = this.currentCommand.trim();
    if (!cmd || this.isSubmitting || !this.mission || this.isCorrect === true) return;

    this.addLine(`$ ${cmd}`, 'input');
    this.currentCommand = '';
    this.isSubmitting = true;
    this.scrollTerminal();

    this.tqService.submitMissionAnswer(this.missionId, this.userId, cmd).subscribe({
      next: (res) => {
        this.result = res;
        this.isCorrect = res.correct;
        this.isSubmitting = false;
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
