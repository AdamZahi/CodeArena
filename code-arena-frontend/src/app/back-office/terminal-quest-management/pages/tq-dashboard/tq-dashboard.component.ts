import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';
import { GlobalStats, StoryChapter } from '../../../../front-office/terminal-quest/models/terminal-quest.model';

@Component({
  selector: 'app-tq-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './tq-dashboard.component.html',
  styleUrls: ['./tq-dashboard.component.css']
})
export class TqDashboardComponent implements OnInit {
  globalStats: GlobalStats | null = null;
  chapters: StoryChapter[] = [];
  overview: any = null;
  difficultyStats: any[] = [];
  leaderboard: any[] = [];
  searchQuery = '';
  searchResults: any[] | null = null;
  isLoading = true;
  isSearching = false;
  error = '';

  constructor(private readonly adminTqService: AdminTqService) {}

  ngOnInit(): void {
    this.adminTqService.getGlobalStats().subscribe({
      next: (stats) => { this.globalStats = stats; },
      error: () => { this.error = 'Could not load stats.'; }
    });

    this.adminTqService.getChapters().subscribe({
      next: (chapters) => { this.chapters = chapters; this.isLoading = false; },
      error: () => { this.isLoading = false; }
    });

    this.adminTqService.getOverview().subscribe({
      next: (data) => { this.overview = data; this.difficultyStats = data.difficultyBreakdown ?? []; },
      error: () => {}
    });

    this.adminTqService.getLeaderboard(0, 5).subscribe({
      next: (page) => { this.leaderboard = page.content ?? []; },
      error: () => {}
    });
  }

  searchPlayers(): void {
    if (!this.searchQuery.trim()) { this.searchResults = null; return; }
    this.isSearching = true;
    this.adminTqService.searchPlayers(this.searchQuery).subscribe({
      next: (results) => { this.searchResults = results; this.isSearching = false; },
      error: () => { this.isSearching = false; }
    });
  }

  exportGlobalPdf(): void {
    this.adminTqService.exportGlobalPdf().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'global-report.pdf';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {}
    });
  }

  get completionRatePct(): string {
    if (!this.globalStats) return '0.0';
    const r = this.globalStats.overallCompletionRate;
    return (r > 1 ? r : r * 100).toFixed(1);
  }
}
