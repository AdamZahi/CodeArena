import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { filter, take } from 'rxjs/operators';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { SurvivalLeaderboardEntry } from '../../models/terminal-quest.model';

@Component({
  selector: 'app-survival-leaderboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './survival-leaderboard.component.html',
  styleUrls: ['./survival-leaderboard.component.css']
})
export class SurvivalLeaderboardComponent implements OnInit {
  leaderboard: SurvivalLeaderboardEntry[] = [];
  userRanking: SurvivalLeaderboardEntry | null = null;
  isLoading = true;
  hasUserRecord = true;
  userId = '';

  readonly medals = ['&#129351;', '&#129352;', '&#129353;']; // 🥇🥈🥉

  constructor(
    private readonly auth: AuthService,
    private tqService: TerminalQuestService
  ) {}

  ngOnInit(): void {
    this.auth.user$.pipe(
      filter(u => !!u),
      take(1)
    ).subscribe(user => {
      this.userId = user?.sub ?? '';
      this.tqService.getLeaderboard().subscribe({
        next: (lb) => {
          this.leaderboard = lb;
          this.loadUserRanking();
        },
        error: () => {
          this.isLoading = false;
        }
      });
    });
  }

  private loadUserRanking(): void {
    this.tqService.getMyRanking().subscribe({
      next: (ranking) => {
        this.userRanking = ranking;
        if (ranking.bestWave === 0 && ranking.bestScore === 0) {
          this.hasUserRecord = false;
        }
        this.isLoading = false;
      },
      error: () => {
        this.hasUserRecord = false;
        this.isLoading = false;
      }
    });
  }

  getUserRank(): number {
    if (!this.userRanking) return -1;
    const idx = this.leaderboard.findIndex(e => e.userId === this.userRanking!.userId);
    return idx >= 0 ? idx + 1 : -1;
  }

  getRankStyle(index: number): string {
    if (index === 0) return 'rank-gold';
    if (index === 1) return 'rank-silver';
    if (index === 2) return 'rank-bronze';
    return '';
  }
}
