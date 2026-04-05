import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';
import { StoryLevel, StoryChapter } from '../../../../front-office/terminal-quest/models/terminal-quest.model';

@Component({
  selector: 'app-level-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './level-list.component.html',
  styleUrls: ['./level-list.component.css']
})
export class LevelListComponent implements OnInit {
  chapterId = '';
  chapter: StoryChapter | null = null;
  levels: StoryLevel[] = [];
  isLoading = true;
  errorMsg = '';
  deletingId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private adminTqService: AdminTqService
  ) {}

  ngOnInit(): void {
    this.chapterId = this.route.snapshot.paramMap.get('chapterId') || '';

    this.adminTqService.getChapterById(this.chapterId).subscribe({
      next: (ch) => { this.chapter = ch; },
      error: (err) => console.error('Failed to load chapter:', err)
    });

    this.loadLevels();
  }

  private loadLevels(): void {
    this.isLoading = true;
    this.adminTqService.getLevelsByChapter(this.chapterId).subscribe({
      next: (levels) => {
        this.levels = levels;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load levels:', err);
        this.errorMsg = 'Failed to load levels.';
        this.isLoading = false;
      }
    });
  }

  confirmDelete(level: StoryLevel): void {
    if (!confirm(`Delete level "${level.title}"?`)) return;
    this.deletingId = level.id;
    this.adminTqService.deleteLevel(level.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.loadLevels();
      },
      error: (err) => {
        console.error('Delete failed:', err);
        this.errorMsg = 'Failed to delete level.';
        this.deletingId = null;
      }
    });
  }

  diffClass(difficulty: string): string {
    return 'diff-' + difficulty.toLowerCase();
  }

  truncate(text: string, max = 60): string {
    return text.length > max ? text.slice(0, max) + '…' : text;
  }
}
