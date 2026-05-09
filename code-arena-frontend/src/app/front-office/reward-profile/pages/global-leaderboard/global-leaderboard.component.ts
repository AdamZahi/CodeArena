import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleService } from '../../../battle/services/battle.service';
import { SeasonLeaderboardEntry, XpLeaderboardEntry } from '../../models/leaderboard.model';
import { Observable, BehaviorSubject, switchMap, combineLatest, map } from 'rxjs';

@Component({
  selector: 'app-global-leaderboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './global-leaderboard.component.html',
  styleUrls: ['./global-leaderboard.component.css']
})
export class GlobalLeaderboardComponent implements OnInit {
  private readonly battleService = inject(BattleService);

  activeTab$ = new BehaviorSubject<'elo' | 'xp'>('elo');

  readonly eloLeaderboard$: Observable<SeasonLeaderboardEntry[]> = this.battleService.getSeasonLeaderboard(0, 50).pipe(
    map(resp => resp.entries)
  );

  readonly xpLeaderboard$: Observable<XpLeaderboardEntry[]> = this.battleService.getXpLeaderboard(0, 50).pipe(
    map(resp => resp.entries)
  );

  ngOnInit() {}

  setTab(tab: 'elo' | 'xp') {
    this.activeTab$.next(tab);
  }
}
