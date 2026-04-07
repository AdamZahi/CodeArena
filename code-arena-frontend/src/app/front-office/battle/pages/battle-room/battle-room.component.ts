import { Component, OnInit, OnDestroy, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, Subscription, interval } from 'rxjs';
import { takeUntil, debounceTime } from 'rxjs/operators';
import { AuthService } from '@auth0/auth0-angular';
import { BattleWebsocketService } from '../../services/battle-websocket.service';
import { BattleService } from '../../services/battle.service';
import {
  ArenaStateResponse,
  ArenaChallengeResponse,
  OpponentProgressEvent,
  MatchFinishedEvent,
  SubmissionResultResponse,
  ProgressPulse,
  OpponentActivityEvent,
  ActivityType,
  TestCaseProgressEvent,
  TestCaseStatus,
  PlayerDisconnectedEvent,
  PlayerReconnectedEvent,
} from '../../models/battle-room.model';

@Component({
  selector: 'app-battle-room',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './battle-room.component.html',
  styleUrls: ['./battle-room.component.css'],
})
export class BattleRoomComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ws = inject(BattleWebsocketService);
  private readonly battleService = inject(BattleService);
  private readonly auth = inject(AuthService);
  private readonly destroy$ = new Subject<void>();

  roomId = '';
  currentUserId = '';
  arenaState: ArenaStateResponse | null = null;
  lastSubmissionResult: SubmissionResultResponse | null = null;

  activeChallengeIndex = 0;
  selectedLanguage = 'javascript';
  code = '';
  submitting = false;
  lineCount = 20;
  leftPanelWidth = 40;
  private resizing = false;

  opponentPulses: Record<string, ProgressPulse | null> = {};
  private solvedChallenges = new Set<string>();
  private countdownStarted = false;

  // ── Feature 1: Opponent Activity ─────────────────────────
  opponentActivities: Record<string, ActivityType> = {};
  challengeSwitchToast: { displayName: string; challengeId: number } | null = null;
  private toastTimeout: any = null;
  private typingSubject = new Subject<void>();
  private idleTimer: any = null;
  private readonly IDLE_TIMEOUT = 5000;

  // ── Feature 2: Test Case Progress ────────────────────────
  testCaseChips: { status: TestCaseStatus }[] = [];

  // ── Auto-Save Draft ───────────────────────────────────────
  private draftSaveSubject = new Subject<void>();
  private draftPerChallenge: Record<string, { code: string; language: string }> = {};

  // ── Feature 3: Disconnect / Reconnect ────────────────────
  disconnected = false;
  reconnectCountdown = 0;
  private reconnectInterval: any = null;
  opponentDisconnects: Record<string, { displayName: string; deadline: number }> = {};
  private heartbeatSub: Subscription | null = null;

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('roomId') ?? '';
    if (!this.roomId) return;

    this.auth.user$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      if (user?.sub) this.currentUserId = user.sub;
    });

    this.ws.connect(this.roomId);

    this.battleService.getArenaState(this.roomId).subscribe({
      next: (state) => {
        this.arenaState = state;
        this.startCountdown();
        this.restoreAllDrafts();
      },
      error: () => {},
    });

    this.ws.arenaState$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => {
        this.arenaState = e.payload;
        this.startCountdown();
      });

    this.ws.opponentProgress$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onOpponentProgress(e.payload));

    this.ws.matchFinished$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onMatchFinished(e.payload));

    this.ws.matchCancelled$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.clearAllDrafts();
        this.router.navigate(['/battle']);
      });

    this.ws.submissionResult$
      .pipe(takeUntil(this.destroy$))
      .subscribe((result) => this.onSubmissionResult(result));

    // ── Feature 1: Opponent activity events ──────────────────
    this.ws.opponentActivity$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onOpponentActivity(e.payload));

    // Debounce typing reports — send at most every 2s
    this.typingSubject
      .pipe(debounceTime(2000), takeUntil(this.destroy$))
      .subscribe(() => {
        this.battleService.reportActivity(this.roomId, {
          type: 'TYPING',
          challengeId: this.activeChallenge?.position ?? null,
        }).subscribe();
      });

    // ── Feature 2: Test case progress ────────────────────────
    this.ws.testCaseProgress$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onTestCaseProgress(e));

    // ── Feature 3: Disconnect/Reconnect ──────────────────────
    this.ws.playerDisconnected$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onPlayerDisconnected(e.payload));

    this.ws.playerReconnected$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onPlayerReconnected(e.payload));

    // Start heartbeat every 15s
    this.heartbeatSub = interval(15000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.battleService.sendHeartbeat(this.roomId).subscribe();
      });

    // ── Auto-Save Draft: debounce 1s ─────────────────────────
    this.draftSaveSubject
      .pipe(debounceTime(1000), takeUntil(this.destroy$))
      .subscribe(() => this.saveDraft());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.ws.disconnect();
    if (this.idleTimer) clearTimeout(this.idleTimer);
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    if (this.reconnectInterval) clearInterval(this.reconnectInterval);
  }

  get activeChallenge(): ArenaChallengeResponse | null {
    return this.arenaState?.challenges[this.activeChallengeIndex] ?? null;
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  private startCountdown(): void {
    if (this.countdownStarted || !this.arenaState || this.arenaState.remainingSeconds <= 0) return;
    this.countdownStarted = true;

    interval(1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.arenaState && this.arenaState.remainingSeconds > 0) {
          this.arenaState = {
            ...this.arenaState,
            remainingSeconds: this.arenaState.remainingSeconds - 1,
          };
        }
      });
  }

  isChallengeSolved(roomChallengeId: string): boolean {
    return this.solvedChallenges.has(roomChallengeId);
  }

  getLineNumbers(): number[] {
    return Array.from({ length: this.lineCount }, (_, i) => i + 1);
  }

  onCodeInput(): void {
    const lines = this.code.split('\n').length;
    this.lineCount = Math.max(20, lines + 5);

    // Feature 1: emit typing activity
    this.typingSubject.next();
    this.resetIdleTimer();

    // Auto-save draft
    this.draftSaveSubject.next();
  }

  onEditorKeydown(event: KeyboardEvent): void {
    if (event.key === 'Tab') {
      event.preventDefault();
      const textarea = event.target as HTMLTextAreaElement;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      this.code = this.code.substring(0, start) + '  ' + this.code.substring(end);
      setTimeout(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 2;
      });
    }
  }

  selectChallenge(index: number): void {
    if (index === this.activeChallengeIndex) return;

    // Save current draft before switching
    this.saveDraftForChallenge(this.activeChallengeIndex);

    this.activeChallengeIndex = index;

    // Restore draft for new challenge
    this.restoreDraftForChallenge(index);

    // Feature 1: report challenge switch
    this.battleService.reportActivity(this.roomId, {
      type: 'SWITCHED_CHALLENGE',
      challengeId: this.arenaState?.challenges[index]?.position ?? null,
    }).subscribe();
  }

  submitCode(): void {
    if (!this.activeChallenge || this.submitting) return;
    this.submitting = true;
    this.lastSubmissionResult = null;
    this.testCaseChips = [];

    this.battleService.submitSolution(this.roomId, {
      roomId: this.roomId,
      roomChallengeId: this.activeChallenge.roomChallengeId,
      language: this.selectedLanguage,
      code: this.code,
    }).subscribe({
      next: () => {
        // HTTP response only confirms receipt (PENDING).
        // The actual judging result arrives via WebSocket (submissionResult$).
      },
      error: () => {
        this.submitting = false;
      },
    });
  }

  private onSubmissionResult(result: SubmissionResultResponse): void {
    this.lastSubmissionResult = result;
    this.submitting = false;

    if (result.isAccepted) {
      this.solvedChallenges.add(result.roomChallengeId);
      this.clearDraft(result.roomChallengeId);
      // Auto-advance to next unsolved challenge
      if (this.arenaState) {
        const nextIndex = this.arenaState.challenges.findIndex(
          (c, i) => i > this.activeChallengeIndex && !this.solvedChallenges.has(c.roomChallengeId)
        );
        if (nextIndex >= 0) {
          setTimeout(() => (this.activeChallengeIndex = nextIndex), 1000);
        }
      }
    }
  }

  private onOpponentProgress(progress: OpponentProgressEvent): void {
    if (!this.arenaState) return;

    this.arenaState = {
      ...this.arenaState,
      participants: this.arenaState.participants.map((p) =>
        p.participantId === progress.participantId
          ? {
              ...p,
              challengesCompleted: progress.challengesCompleted,
              currentChallengePosition: progress.currentChallengePosition,
              totalAttempts: progress.totalAttempts,
              isFinished: progress.isFinished,
            }
          : p
      ),
    };

    // Show pulse animation
    this.opponentPulses[progress.participantId] = progress.pulse;
    setTimeout(() => {
      this.opponentPulses[progress.participantId] = null;
    }, 800);
  }

  private onMatchFinished(_event: MatchFinishedEvent): void {
    this.clearAllDrafts();
    this.router.navigate(['/battle/result', this.roomId]);
  }

  // ── Feature 1: Opponent Activity ─────────────────────────

  private onOpponentActivity(event: OpponentActivityEvent): void {
    this.opponentActivities[event.participantId] = event.type;

    if (event.type === 'SWITCHED_CHALLENGE' && event.challengeId != null) {
      this.challengeSwitchToast = {
        displayName: event.displayName,
        challengeId: event.challengeId,
      };
      if (this.toastTimeout) clearTimeout(this.toastTimeout);
      this.toastTimeout = setTimeout(() => {
        this.challengeSwitchToast = null;
      }, 3000);
    }

    // Clear typing/idle after 4s
    if (event.type === 'TYPING') {
      setTimeout(() => {
        if (this.opponentActivities[event.participantId] === 'TYPING') {
          this.opponentActivities[event.participantId] = 'IDLE';
        }
      }, 4000);
    }
  }

  private resetIdleTimer(): void {
    if (this.idleTimer) clearTimeout(this.idleTimer);
    this.idleTimer = setTimeout(() => {
      this.battleService.reportActivity(this.roomId, {
        type: 'IDLE',
        challengeId: this.activeChallenge?.position ?? null,
      }).subscribe();
    }, this.IDLE_TIMEOUT);
  }

  // ── Feature 2: Test Case Progress ────────────────────────

  private onTestCaseProgress(event: TestCaseProgressEvent): void {
    // Initialize chips array if needed
    if (this.testCaseChips.length === 0 && event.totalTestCases > 0) {
      this.testCaseChips = Array.from({ length: event.totalTestCases }, () => ({
        status: 'PENDING' as TestCaseStatus,
      }));
    }

    if (event.testCaseIndex < this.testCaseChips.length) {
      this.testCaseChips[event.testCaseIndex] = { status: event.status };
    }
  }

  // ── Feature 3: Disconnect / Reconnect ────────────────────

  private onPlayerDisconnected(event: PlayerDisconnectedEvent): void {
    this.opponentDisconnects[event.participantId] = {
      displayName: event.displayName,
      deadline: event.reconnectDeadlineSeconds,
    };

    // Countdown the deadline
    const countdownInterval = setInterval(() => {
      const entry = this.opponentDisconnects[event.participantId];
      if (entry && entry.deadline > 0) {
        this.opponentDisconnects[event.participantId] = {
          ...entry,
          deadline: entry.deadline - 1,
        };
      } else {
        clearInterval(countdownInterval);
      }
    }, 1000);
  }

  private onPlayerReconnected(event: PlayerReconnectedEvent): void {
    delete this.opponentDisconnects[event.participantId];
  }

  @HostListener('window:offline')
  onOffline(): void {
    this.disconnected = true;
    this.reconnectCountdown = 30;
    this.reconnectInterval = setInterval(() => {
      if (this.reconnectCountdown > 0) {
        this.reconnectCountdown--;
      } else {
        clearInterval(this.reconnectInterval);
      }
    }, 1000);
  }

  @HostListener('window:online')
  onOnline(): void {
    this.attemptReconnect();
  }

  attemptReconnect(): void {
    this.battleService.reconnect(this.roomId).subscribe({
      next: (state) => {
        this.arenaState = state;
        this.disconnected = false;
        if (this.reconnectInterval) clearInterval(this.reconnectInterval);
        // Re-establish WebSocket
        this.ws.disconnect();
        this.ws.connect(this.roomId);
      },
      error: () => {
        // Retry in 3s
        setTimeout(() => this.attemptReconnect(), 3000);
      },
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.arenaState && this.arenaState.status === 'IN_PROGRESS') {
      event.preventDefault();
    }
  }

  get disconnectedOpponents(): { participantId: string; displayName: string; deadline: number }[] {
    return Object.entries(this.opponentDisconnects).map(([id, info]) => ({
      participantId: id,
      displayName: info.displayName,
      deadline: info.deadline,
    }));
  }

  // ── Auto-Save Draft Methods ────────────────────────────────

  private draftKey(challengeId: string): string {
    return `battle_draft:${this.roomId}:${challengeId}:${this.currentUserId}`;
  }

  private saveDraft(): void {
    const ch = this.activeChallenge;
    if (!ch || !this.currentUserId) return;
    this.saveDraftForChallenge(this.activeChallengeIndex);
  }

  private saveDraftForChallenge(index: number): void {
    const ch = this.arenaState?.challenges[index];
    if (!ch || !this.currentUserId) return;
    // Save in memory
    this.draftPerChallenge[ch.roomChallengeId] = { code: this.code, language: this.selectedLanguage };
    // Save to localStorage
    try {
      const value = JSON.stringify({
        code: this.code,
        language: this.selectedLanguage,
        savedAt: Date.now(),
      });
      if (value.length > 50000) return; // 50KB guard
      localStorage.setItem(this.draftKey(ch.roomChallengeId), value);
    } catch { /* QuotaExceededError guard */ }
  }

  private restoreDraftForChallenge(index: number): void {
    const ch = this.arenaState?.challenges[index];
    if (!ch) { this.code = ''; return; }
    // Check in-memory first
    const mem = this.draftPerChallenge[ch.roomChallengeId];
    if (mem) {
      this.code = mem.code;
      this.selectedLanguage = mem.language;
      this.lineCount = Math.max(20, this.code.split('\n').length + 5);
      return;
    }
    // Fallback to localStorage
    try {
      const raw = localStorage.getItem(this.draftKey(ch.roomChallengeId));
      if (raw) {
        const draft = JSON.parse(raw);
        this.code = draft.code || '';
        this.selectedLanguage = draft.language || 'javascript';
        this.lineCount = Math.max(20, this.code.split('\n').length + 5);
        return;
      }
    } catch { /* ignore */ }
    this.code = '';
  }

  private restoreAllDrafts(): void {
    if (!this.arenaState || this.arenaState.status === 'FINISHED') return;
    // Restore draft for active challenge
    this.restoreDraftForChallenge(this.activeChallengeIndex);
    // Pre-load other drafts into memory
    for (let i = 0; i < this.arenaState.challenges.length; i++) {
      if (i === this.activeChallengeIndex) continue;
      const ch = this.arenaState.challenges[i];
      try {
        const raw = localStorage.getItem(this.draftKey(ch.roomChallengeId));
        if (raw) {
          const draft = JSON.parse(raw);
          this.draftPerChallenge[ch.roomChallengeId] = { code: draft.code, language: draft.language };
        }
      } catch { /* ignore */ }
    }
  }

  private clearDraft(roomChallengeId: string): void {
    delete this.draftPerChallenge[roomChallengeId];
    try {
      localStorage.removeItem(this.draftKey(roomChallengeId));
    } catch { /* ignore */ }
  }

  private clearAllDrafts(): void {
    if (!this.arenaState) return;
    for (const ch of this.arenaState.challenges) {
      this.clearDraft(ch.roomChallengeId);
    }
    this.draftPerChallenge = {};
  }

  // ── Panel Resizing ─────────────────────────────────────────

  startResize(event: MouseEvent): void {
    this.resizing = true;
    event.preventDefault();
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    if (!this.resizing) return;
    const percentage = (event.clientX / window.innerWidth) * 100;
    this.leftPanelWidth = Math.max(25, Math.min(70, percentage));
  }

  @HostListener('document:mouseup')
  onMouseUp(): void {
    this.resizing = false;
  }
}
