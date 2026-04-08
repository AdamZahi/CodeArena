import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { CurrentUser, AuthUserSyncService } from '../../../../core/auth/auth-user-sync.service';
import { CustomizeIdentityComponent } from './components/customize-identity/customize-identity.component';
import { SubmissionService, SubmissionDto } from '../../../../core/services/submission.service';
import { BattleService } from '../../../battle/services/battle.service';
import { MatchHistorySummary } from '../../models/match-history.model';
import { Observable, interval, of } from 'rxjs';
import { switchMap, map, startWith, shareReplay } from 'rxjs/operators';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, CustomizeIdentityComponent],
  templateUrl: './my-profile.component.html',
  styleUrls: ['./my-profile.component.css']
})
export class MyProfileComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly authUserSync = inject(AuthUserSyncService);
  private readonly submissionService = inject(SubmissionService);
  private readonly battleService = inject(BattleService);

  readonly user$ = this.auth.user$;
  readonly currentUser$ = this.authUserSync.currentUser$;

  // XP constants matching backend (ExecutionService: every 500 XP = 1 level)
  readonly XP_PER_LEVEL = 500;

  readonly submissions$: Observable<SubmissionDto[]> = this.currentUser$.pipe(
    switchMap(user => {
      if (user?.auth0Id) {
        return this.submissionService.getUserSubmissions(user.auth0Id).pipe(
          map(submissions => submissions.slice(0, 10))
        );
      }
      return [];
    })
  );

  readonly battleHistory$: Observable<MatchHistorySummary[]> = this.currentUser$.pipe(
    switchMap(user => {
      if (user?.auth0Id) {
        return this.battleService.getMyMatchHistory(0, 10).pipe(
          map(response => response.matches)
        );
      }
      return [];
    })
  );
  
  readonly topElo$ = this.battleService.getSeasonLeaderboard(0, 3).pipe(
    map(res => res.entries),
    startWith([])
  );

  readonly topXp$ = this.battleService.getXpLeaderboard(0, 3).pipe(
    map(res => res.entries),
    startWith([])
  );

  readonly battleProfile$ = this.currentUser$.pipe(
    switchMap(user => {
      if (user?.auth0Id) {
        return this.battleService.getBattleProfile(user.auth0Id);
      }
      return of(null);
    }),
    shareReplay(1)
  );

  isCustomizeModalOpen = false;
  activeLeaderboardTab: 'elo' | 'xp' = 'elo';

  ngOnInit() {
    // Force re-fetch user data whenever profile loads (ensures fresh XP/level)
    this.authUserSync.forceSync();
  }

  setLeaderboardTab(tab: 'elo' | 'xp') {
    this.activeLeaderboardTab = tab;
  }

  getXpProgress(totalXp: number | undefined): number {
    const xp = totalXp || 0;
    return (xp % this.XP_PER_LEVEL) / this.XP_PER_LEVEL * 100;
  }

  getXpCurrent(totalXp: number | undefined): number {
    return (totalXp || 0) % this.XP_PER_LEVEL;
  }

  getTierIcon(tier: string | undefined): string {
    const t = (tier || 'BRONZE').toUpperCase();
    switch (t) {
      case 'LEGEND': return 'https://opgg-static.akamaized.net/images/medals_new/challenger.png';
      case 'DIAMOND': return 'https://opgg-static.akamaized.net/images/medals_new/diamond.png';
      case 'GOLD': return 'https://opgg-static.akamaized.net/images/medals_new/gold.png';
      case 'SILVER': return 'https://opgg-static.akamaized.net/images/medals_new/silver.png';
      default: return 'https://opgg-static.akamaized.net/images/medals_new/bronze.png';
    }
  }

  openCustomizeModal() {
    this.isCustomizeModalOpen = true;
  }

  closeCustomizeModal() {
    this.isCustomizeModalOpen = false;
    // Re-fetch after closing the customize modal (badges/icons may have changed)
    this.authUserSync.forceSync();
  }
}
