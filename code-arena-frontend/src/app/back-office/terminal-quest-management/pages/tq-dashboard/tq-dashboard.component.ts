import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';
import { GlobalStats, StoryChapter } from '../../../../front-office/terminal-quest/models/terminal-quest.model';

@Component({
  selector: 'app-tq-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tq-dashboard.component.html',
  styleUrls: ['./tq-dashboard.component.css']
})
export class TqDashboardComponent implements OnInit {
  globalStats: GlobalStats | null = null;
  chapters: StoryChapter[] = [];
  isLoading = true;
  error = '';

  constructor(private adminTqService: AdminTqService) {}

  ngOnInit(): void {
    this.adminTqService.getGlobalStats().subscribe({
      next: (stats) => { this.globalStats = stats; },
      error: (err) => {
        console.error('Failed to load global stats:', err);
        this.error = 'Could not load stats.';
      }
    });

    this.adminTqService.getChapters().subscribe({
      next: (chapters) => {
        this.chapters = chapters;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load chapters:', err);
        this.isLoading = false;
      }
    });
  }

  get completionRatePct(): string {
    if (!this.globalStats) return '0.0';
    const r = this.globalStats.overallCompletionRate;
    return (r > 1 ? r : r * 100).toFixed(1);
  }
}
