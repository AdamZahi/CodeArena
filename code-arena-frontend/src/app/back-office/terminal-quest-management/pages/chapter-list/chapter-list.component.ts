import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';
import { StoryChapter } from '../../../../front-office/terminal-quest/models/terminal-quest.model';

@Component({
  selector: 'app-chapter-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './chapter-list.component.html',
  styleUrls: ['./chapter-list.component.css']
})
export class ChapterListComponent implements OnInit {
  chapters: StoryChapter[] = [];
  isLoading = true;
  errorMsg = '';
  deletingId: string | null = null;

  constructor(private adminTqService: AdminTqService) {}

  ngOnInit(): void {
    this.loadChapters();
  }

  private loadChapters(): void {
    this.isLoading = true;
    this.adminTqService.getChapters().subscribe({
      next: (chapters) => {
        this.chapters = chapters;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load chapters:', err);
        this.errorMsg = 'Failed to load chapters.';
        this.isLoading = false;
      }
    });
  }

  confirmDelete(chapter: StoryChapter): void {
    if (!confirm(`Delete chapter "${chapter.title}"? All levels will be deleted too.`)) return;
    this.deletingId = chapter.id;
    this.adminTqService.deleteChapter(chapter.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.loadChapters();
      },
      error: (err) => {
        console.error('Delete failed:', err);
        this.errorMsg = 'Failed to delete chapter.';
        this.deletingId = null;
      }
    });
  }

  levelCount(chapter: StoryChapter): number {
    return chapter.levels?.length ?? 0;
  }
}
