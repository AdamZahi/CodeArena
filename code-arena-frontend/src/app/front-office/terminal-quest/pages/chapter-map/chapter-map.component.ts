import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { StoryChapter, StoryLevel, LevelProgress } from '../../models/terminal-quest.model';

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
        progress.forEach(p => this.progressMap.set(p.levelId, p));
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  isCompleted(level: StoryLevel): boolean {
    return this.progressMap.get(level.id)?.completed ?? false;
  }

  getStars(level: StoryLevel): number {
    const p = this.progressMap.get(level.id);
    return p?.completed ? p.starsEarned : 0;
  }

  readonly starRange = [1, 2, 3];

  navigateToLevel(level: StoryLevel, chapter: StoryChapter): void {
    if (chapter.isLocked) return;
    this.router.navigate(['/terminal-quest/story/play', level.id]);
  }
}
