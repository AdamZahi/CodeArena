import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '@auth0/auth0-angular';
import { BattleWebsocketService } from '../../services/battle-websocket.service';
import { BattleService } from '../../services/battle.service';
import {
  SpectatorFeedEvent,
  PostMatchSummaryResponse,
  PlayerScoreResponse,
} from '../../models/battle-room.model';

interface ConfettiPiece {
  x: number;
  delay: number;
  color: string;
}

@Component({
  selector: 'app-battle-result',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './battle-result.component.html',
  styleUrls: ['./battle-result.component.css'],
})
export class BattleResultComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ws = inject(BattleWebsocketService);
  private readonly battleService = inject(BattleService);
  private readonly auth = inject(AuthService);
  private readonly destroy$ = new Subject<void>();

  roomId = '';
  currentUserId = '';
  summary: PostMatchSummaryResponse | null = null;
  spectatorFeed: SpectatorFeedEvent[] = [];
  confettiPieces: ConfettiPiece[] = [];

  get isWinner(): boolean {
    return this.myStanding?.isWinner ?? false;
  }

  get myStanding(): PlayerScoreResponse | null {
    return this.summary?.standings.find(s => s.userId === this.currentUserId) ?? null;
  }

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('roomId') ?? '';
    if (!this.roomId) {
      this.router.navigate(['/battle']);
      return;
    }

    this.auth.user$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      if (user?.sub) this.currentUserId = user.sub;
    });

    // Load scoreboard from REST API
    this.battleService.getScoreboard(this.roomId).subscribe({
      next: (summary) => {
        this.summary = summary;
        if (this.isWinner) this.generateConfetti();
      },
      error: () => {},
    });

    // Also listen via WebSocket for spectator feed
    this.ws.connect(this.roomId);

    this.ws.spectatorFeed$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.spectatorFeed.push(e.payload));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.ws.disconnect();
  }

  formatDuration(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    if (m === 0) return `${s}s`;
    return `${m}m ${s}s`;
  }

  getPodiumOrder(): PlayerScoreResponse[] {
    if (!this.summary) return [];
    const sorted = [...this.summary.standings].sort((a, b) => a.finalRank - b.finalRank);
    // Reorder for visual podium: 2nd, 1st, 3rd
    if (sorted.length >= 3) {
      return [sorted[1], sorted[0], sorted[2]];
    }
    return sorted;
  }

  backToArena(): void {
    this.router.navigate(['/battle']);
  }

  viewLeaderboard(): void {
    this.router.navigate(['/battle']);
  }

  private generateConfetti(): void {
    const colors = ['#8b5cf6', '#06b6d4', '#10b981', '#ffd700', '#ef4444', '#f59e0b'];
    this.confettiPieces = Array.from({ length: 40 }, () => ({
      x: Math.random() * 100,
      delay: Math.random() * 3,
      color: colors[Math.floor(Math.random() * colors.length)],
    }));
  }
}
