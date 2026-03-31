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
  readonly userId = 'test-user-001';

  constructor(
    private tqService: TerminalQuestService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.tqService.getChapters().subscribe({
      next: (chapters) => {
        this.chapters = chapters;
        this.loadProgress();
      },
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
    if (missions.length === 0) return false;
    return missions.every(m => this.progressMap.get(m.id)?.completed === true);
  }

  readonly starRange = [1, 2, 3];

  navigateToMission(mission: StoryMission, chapter: StoryChapter, index: number): void {
    if (!this.isChapterUnlocked(chapter, index)) return;
    this.router.navigate(['/terminal-quest/story/play', mission.id]);
  }
}
