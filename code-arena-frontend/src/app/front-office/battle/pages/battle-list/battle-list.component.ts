import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BattleService } from '../../services/battle.service';
import {
  BattleRoomResponse,
  CreateRoomRequest,
} from '../../models/battle-room.model';

@Component({
  selector: 'app-battle-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './battle-list.component.html',
  styleUrls: ['./battle-list.component.css'],
})
export class BattleListComponent implements OnInit {
  Math = Math;

  private readonly router = inject(Router);
  private readonly battleService = inject(BattleService);

  publicRooms: BattleRoomResponse[] = [];
  loading = true;
  showCreateModal = false;
  showJoinModal = false;
  creating = false;
  joining = false;
  createError = '';
  searchQuery = '';
  selectedMode = '';
  inviteToken = '';

  modes = ['DUEL', 'BLITZ', 'RANKED_ARENA', 'PRACTICE'];
  difficulties = ['EASY', 'MEDIUM', 'HARD', 'MIXED'];

  allModes = [
    { value: 'DUEL', label: 'DUEL', icon: '\u2694' },
    { value: 'RANKED_ARENA', label: 'RANKED', icon: '\u265B' },
    { value: 'BLITZ', label: 'BLITZ', icon: '\u26A1' },
    { value: 'PRACTICE', label: 'PRACTICE', icon: '\u2696' },
    { value: 'TEAM', label: 'TEAM', icon: '\u2660' },
    { value: 'DAILY', label: 'DAILY', icon: '\u2606' },
  ];

  createForm: CreateRoomRequest = {
    mode: 'DUEL',
    maxPlayers: 2,
    challengeCount: 3,
    isPublic: true,
    difficulty: 'MIXED',
  };

  ngOnInit(): void {
    this.loadRooms();
  }

  get filteredRooms(): BattleRoomResponse[] {
    return this.publicRooms.filter((r) => {
      const matchesMode = !this.selectedMode || r.mode === this.selectedMode;
      const matchesSearch =
        !this.searchQuery ||
        r.participants.some((p) =>
          p.username.toLowerCase().includes(this.searchQuery.toLowerCase())
        );
      return matchesMode && matchesSearch;
    });
  }

  loadRooms(): void {
    this.loading = true;
    this.battleService.getPublicRooms().subscribe({
      next: (rooms) => {
        this.publicRooms = rooms;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  getTotalPlayers(): number {
    return this.publicRooms.reduce((sum, r) => sum + r.participants.length, 0);
  }

  getActiveMatches(): number {
    return this.publicRooms.filter((r) => r.status === 'IN_PROGRESS').length;
  }

  getHostInitial(room: BattleRoomResponse): string {
    return room.participants[0]?.username?.charAt(0).toUpperCase() || '?';
  }

  joinPublicRoom(room: BattleRoomResponse): void {
    if (room.status === 'WAITING') {
      this.battleService.joinRoom({ inviteToken: room.inviteToken }).subscribe({
        next: () => this.router.navigate(['/battle/lobby', room.id]),
        error: () => this.router.navigate(['/battle/lobby', room.id]),
      });
    }
  }

  createRoom(): void {
    this.creating = true;
    this.createError = '';
    this.battleService.createRoom(this.createForm).subscribe({
      next: (res) => {
        this.creating = false;
        this.showCreateModal = false;
        this.router.navigate(['/battle/lobby', res.room.id]);
      },
      error: (err) => {
        this.creating = false;

        const message = err?.error?.message || 'Unable to create room with current settings.';
        this.createError = message;

        // If backend reports limited challenge availability, sync the stepper to valid max.
        const match = /available:\s*(\d+)/i.exec(message);
        if (match) {
          const available = Number(match[1]);
          if (!Number.isNaN(available) && available > 0) {
            this.createForm.challengeCount = Math.min(this.createForm.challengeCount, available);
          }
        }
      },
    });
  }

  joinByInvite(): void {
    this.joining = true;
    this.battleService.joinRoom({ inviteToken: this.inviteToken }).subscribe({
      next: (lobby) => {
        this.joining = false;
        this.showJoinModal = false;
        this.router.navigate(['/battle/lobby', lobby.room.id]);
      },
      error: () => {
        this.joining = false;
      },
    });
  }
}
