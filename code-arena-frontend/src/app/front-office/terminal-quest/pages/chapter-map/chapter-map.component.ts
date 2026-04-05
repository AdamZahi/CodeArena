import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { StoryChapter, StoryMission, LevelProgress } from '../../models/terminal-quest.model';

@Component({
  selector: 'app-chapter-map',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './chapter-map.component.html',
  styleUrls: ['./chapter-map.component.css']
})
export class ChapterMapComponent implements OnInit {
  chapters: StoryChapter[] = [];
  progressMap = new Map<string, LevelProgress>();
  isLoading = true;

  selectedMission: StoryMission | null = null;
  selectedMissionProgress: LevelProgress | null = null;
  showMissionPanel = false;

  readonly userId   = 'test-user-001';
  readonly starRange = [1, 2, 3];

  constructor(
    private tqService: TerminalQuestService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.tqService.getChapters().subscribe({
      next: (chapters) => { this.chapters = chapters; this.loadProgress(); },
      error: () => { this.isLoading = false; }
    });
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
