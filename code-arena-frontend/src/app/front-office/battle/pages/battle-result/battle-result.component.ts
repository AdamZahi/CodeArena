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

  // Feature 3 — Share
  shareUrl = '';
  shareLoading = false;
  toastMessage = '';
  private toastTimer: any = null;

  // Feature 4 — ELO animation
  animatedEloDelta = 0;
  showTierUpBanner = false;
  private readonly UNRANKED_MODES = ['PRACTICE', 'BLITZ', 'DAILY'];

  get isRankedMode(): boolean {
    return !!this.summary && !this.UNRANKED_MODES.includes(this.summary.mode);
  }

  tierProgress(elo: number | null | undefined): number {
    if (elo == null) return 0;
    // BRONZE 0-1199, SILVER 1200-1499, GOLD 1500-1799, DIAMOND 1800-2099, LEGEND 2100+
    const bands: [number, number][] = [[0, 1200], [1200, 1500], [1500, 1800], [1800, 2100], [2100, 2400]];
    for (const [lo, hi] of bands) {
      if (elo < hi) return Math.max(0, Math.min(100, ((elo - lo) / (hi - lo)) * 100));
    }
    return 100;
  }

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
        this.startEloAnimation();
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

  shareResult(): void {
    if (this.shareLoading) return;
    if (this.shareUrl) {
      this.copyShareUrl();
      return;
    }
    this.shareLoading = true;
    this.battleService.createShareToken(this.roomId).subscribe({
      next: (res) => {
        this.shareUrl = res.shareUrl;
        this.shareLoading = false;
        this.copyShareUrl();
      },
      error: () => {
        this.shareLoading = false;
        this.showToast('Failed to create share link');
      },
    });
  }

  private copyShareUrl(): void {
    if (!this.shareUrl) return;
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(this.shareUrl).then(
        () => this.showToast('Share link copied to clipboard'),
        () => this.showToast(this.shareUrl),
      );
    } else {
      this.showToast(this.shareUrl);
    }
  }

  private showToast(message: string): void {
    this.toastMessage = message;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => (this.toastMessage = ''), 3000);
  }

  private startEloAnimation(): void {
    if (!this.isRankedMode) return;
    const me = this.myStanding;
    if (!me) return;
    const target = me.eloChange ?? 0;
    const duration = 1200;
    const start = performance.now() + 800; // delay after podium entrance
    const tick = (now: number) => {
      const t = Math.max(0, Math.min(1, (now - start) / duration));
      this.animatedEloDelta = Math.round(target * t);
      if (t < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);

    if (me.tierChanged) {
      setTimeout(() => {
        this.showTierUpBanner = true;
        setTimeout(() => (this.showTierUpBanner = false), 5000);
      }, 1500);
    }
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
