import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BattleWebsocketService } from '../../services/battle-websocket.service';
import { MatchFinishedEvent, SpectatorFeedEvent } from '../../models/battle-room.model';

@Component({
  selector: 'app-battle-result',
  standalone: true,
  template: '<p>battle-result works</p>'
})
export class BattleResultComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly ws    = inject(BattleWebsocketService);
  private readonly destroy$ = new Subject<void>();

  roomId = '';
  matchResult: MatchFinishedEvent | null = null;
  spectatorFeed: SpectatorFeedEvent[] = [];

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('roomId') ?? '';
    if (!this.roomId) return;

    this.ws.connect(this.roomId);

    this.ws.matchFinished$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => (this.matchResult = e.payload));

    this.ws.spectatorFeed$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.spectatorFeed.push(e.payload));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.ws.disconnect();
  }
}
