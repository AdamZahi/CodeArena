import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { TimerAudioService } from '../../services/timer-audio.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';
import { VoiceNavWidgetComponent } from '../../components/voice-nav-widget/voice-nav-widget.component';
import { StoryChapter, StoryMission, LevelProgress } from '../../models/terminal-quest.model';

@Component({
  selector: 'app-chapter-map',
  standalone: true,
  imports: [CommonModule, RouterLink, VoiceNavWidgetComponent],
  templateUrl: './chapter-map.component.html',
  styleUrls: ['./chapter-map.component.css']
})
export class ChapterMapComponent implements OnInit, OnDestroy {
  chapters: StoryChapter[] = [];
  progressMap = new Map<string, LevelProgress>();
  isLoading = true;

  selectedMission: StoryMission | null = null;
  selectedMissionProgress: LevelProgress | null = null;
  showMissionPanel = false;

  readonly userId   = 'test-user-001';
  readonly starRange = [1, 2, 3];

  private voiceChapterIndex = 0;
  private bgMusic = new Audio('assets/dex.mp3');

  constructor(
    private tqService: TerminalQuestService,
    private router: Router,
    public audio: TimerAudioService,
    private readonly voiceNav: VoiceNavigationService
  ) {}

  ngOnInit(): void {
    this.bgMusic.loop = true;
    this.bgMusic.volume = 0.4;
    this.bgMusic.play().catch(() => {});

    this.tqService.getChapters().subscribe({
      next: (chapters) => { this.chapters = chapters; this.loadProgress(); },
      error: () => { this.isLoading = false; }
    });

    this.voiceNav.registerPageCommands('chapter-map', (cmd: string) => {
      // Chapter selection
      const chapterMatch = cmd.match(/chapter\s*(\d+)|chapitre\s*(\d+)/);
      if (chapterMatch) {
        const num = parseInt(chapterMatch[1] ?? chapterMatch[2], 10);
        this.selectChapterByVoice(num);
        return true;
      }
      // Mission selection
      const missionMatch = cmd.match(/mission\s*(\d+)|mission\s*(one|two|three|four)/);
      if (missionMatch) {
        const wordMap: Record<string, number> = { one: 1, two: 2, three: 3, four: 4 };
        const num = missionMatch[1]
          ? parseInt(missionMatch[1], 10)
          : wordMap[missionMatch[2]] ?? 1;
        this.selectMissionByVoice(num);
        return true;
      }
      // Play / launch selected mission
      if (cmd === 'play' || cmd === 'start' || cmd === 'jouer') {
        this.playMission();
        return true;
      }
      return false;
    });
    this.voiceNav.autoStart();
  }

  ngOnDestroy(): void {
    this.bgMusic.pause();
    this.bgMusic.currentTime = 0;
    this.voiceNav.unregisterPageCommands('chapter-map');
  }

  private loadProgress(): void {
    this.tqService.getProgress(this.userId).subscribe({
      next: (progress) => {
        progress.forEach(p => {
          if (p.missionId) this.progressMap.set(p.missionId, p);
          if (p.levelId)   this.progressMap.set(p.levelId, p);
        });
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  // ── Voice helpers ─────────────────────────────────────

  private selectChapterByVoice(num: number): void {
    const idx = num - 1;
    if (idx < 0 || idx >= this.chapters.length) {
      this.voiceNav.feedback$.next(`Chapter ${num} not found`);
      return;
    }
    this.voiceChapterIndex = idx;
    this.voiceNav.feedback$.next(`Chapter ${num} selected — say "mission 1" to pick a mission`);
  }

  private selectMissionByVoice(num: number): void {
    const chapter = this.chapters[this.voiceChapterIndex];
    if (!chapter) {
      this.voiceNav.feedback$.next('Say "chapter 1" first to select a chapter');
      return;
    }
    const missions = chapter.missions ?? [];
    const mission  = missions[num - 1];
    if (!mission) {
      this.voiceNav.feedback$.next(`Mission ${num} not found in this chapter`);
      return;
    }
    this.selectMission(mission, chapter, this.voiceChapterIndex);
    this.voiceNav.feedback$.next(`Mission ${num} selected — say "play" to start`);
  }

  // ── Core state helpers ────────────────────────────────

  isCompleted(mission: StoryMission): boolean {
    return this.progressMap.get(mission.id)?.completed ?? false;
  }

  getStars(mission: StoryMission): number {
    const p = this.progressMap.get(mission.id);
    return p?.completed ? p.starsEarned : 0;
  }

  isChapterUnlocked(_chapter: StoryChapter, index: number): boolean {
    if (index === 0) return true;
    const prev = this.chapters[index - 1];
    const missions = prev.missions ?? [];
    if (!missions.length) return false;
    return missions.every(m => this.progressMap.get(m.id)?.completed === true);
  }

  isCurrentMission(mission: StoryMission, chapter: StoryChapter, chapterIndex: number): boolean {
    if (!this.isChapterUnlocked(chapter, chapterIndex)) return false;
    if (this.isCompleted(mission)) return false;
    const missions = chapter.missions ?? [];
    const idx = missions.findIndex(m => m.id === mission.id);
    return missions.slice(0, idx).every(m => this.progressMap.get(m.id)?.completed);
  }

  isMissionLocked(mission: StoryMission, chapter: StoryChapter, chapterIndex: number): boolean {
    if (!this.isChapterUnlocked(chapter, chapterIndex)) return true;
    const missions = chapter.missions ?? [];
    const idx = missions.findIndex(m => m.id === mission.id);
    if (idx === 0) return false;
    return !missions.slice(0, idx).every(m => this.progressMap.get(m.id)?.completed);
  }

  // ── Mission panel ─────────────────────────────────────

  selectMission(mission: StoryMission, chapter: StoryChapter, chapterIndex: number): void {
    if (this.isMissionLocked(mission, chapter, chapterIndex)) return;
    this.audio.playClick();
    this.selectedMission = mission;
    this.selectedMissionProgress = this.progressMap.get(mission.id) ?? null;
    this.showMissionPanel = true;
  }

  closeMissionPanel(): void {
    this.showMissionPanel = false;
    this.selectedMission = null;
    this.selectedMissionProgress = null;
  }

  playMission(): void {
    if (this.selectedMission) {
      this.router.navigate(['/terminal-quest/story/play', this.selectedMission.id]);
    }
  }

  getMissionStars(): number {
    return this.selectedMissionProgress?.completed ? this.selectedMissionProgress.starsEarned : 0;
  }
}
