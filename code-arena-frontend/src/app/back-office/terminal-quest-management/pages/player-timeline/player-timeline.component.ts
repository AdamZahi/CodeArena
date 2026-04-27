import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';

@Component({
  selector: 'app-player-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './player-timeline.component.html',
  styleUrls: ['./player-timeline.component.css']
})
export class PlayerTimelineComponent implements OnInit {
  userId = '';
  timeline: any[] = [];
  breakdown: Record<string, number> = {};
  isLoading = true;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly adminTqService: AdminTqService
  ) {}

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('userId') ?? '';
    if (!this.userId) { this.router.navigate(['/admin/terminal-quest']); return; }

    this.adminTqService.getPlayerTimeline(this.userId).subscribe({
      next: (data) => { this.timeline = data; this.isLoading = false; },
      error: () => { this.isLoading = false; }
    });

    this.adminTqService.getActivityBreakdown(this.userId).subscribe({
      next: (data) => { this.breakdown = data; },
      error: () => {}
    });
  }

  exportPdf(): void {
    this.adminTqService.exportPlayerPdf(this.userId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `player-${this.userId}-report.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {}
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/terminal-quest']);
  }

  get breakdownEntries(): [string, number][] {
    return Object.entries(this.breakdown);
  }

  formatLabel(type: string): string {
    return type.toLowerCase().replace(/_/g, ' ');
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      MISSION_COMPLETED: '✅',
      MISSION_FAILED:    '❌',
      LEVEL_COMPLETED:   '⭐',
      LEVEL_FAILED:      '💔',
      SURVIVAL_STARTED:  '▶',
      SURVIVAL_ENDED:    '🏁',
      HINT_USED:         '💡',
    };
    return icons[type] ?? '•';
  }
}
