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
import { AiService, UserSkillProfileDto } from '../../../challenge/services/ai.service';
import { ViewChild, ElementRef, AfterViewInit } from '@angular/core';

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
  private readonly aiService = inject(AiService);

  @ViewChild('skillRadar', { static: false }) skillRadar!: ElementRef<HTMLCanvasElement>;
  public aiSkillProfile: UserSkillProfileDto | null = null;

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
    if (this.auth.isAuthenticated$) {
      this.loadAiProfile();
    }
  }

  loadAiProfile() {
    this.aiService.getSkillProfile().subscribe({
      next: (profile: any) => {
        this.aiSkillProfile = profile;
        setTimeout(() => this.drawRadarChart(), 300);
      },
      error: (e: any) => console.log('AI Profile not available')
    });
  }

  drawRadarChart() {
    if (!this.aiSkillProfile || !this.skillRadar) return;
    const canvas = this.skillRadar.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = Math.min(centerX, centerY) - 30;

    const skills = this.aiSkillProfile.skillMap;
    // We only want top 6 skills to look good on radar
    let labels = Object.keys(skills);
    if (labels.length === 0) {
      labels = ['LOGIC', 'ARRAYS', 'STRINGS', 'MATH', 'TREES', 'DP'];
      labels.forEach(l => skills[l] = 0.5);
    } else {
      labels = labels.slice(0, 6); // Max 6 
    }
    
    // Ensure at least 3 sides for a polygon
    if (labels.length < 3) {
      labels.push('LOGIC', 'BASICS', 'SYNTAX');
      labels = Array.from(new Set(labels)).slice(0, 6);
    }

    const sides = labels.length;
    const angleStep = (Math.PI * 2) / sides;

    ctx.clearRect(0, 0, width, height);

    // Draw grid rings
    ctx.strokeStyle = 'rgba(139, 92, 246, 0.2)';
    ctx.lineWidth = 1;
    for (let ring = 1; ring <= 4; ring++) {
      ctx.beginPath();
      for (let i = 0; i <= sides; i++) {
        const angle = i * angleStep - Math.PI / 2;
        const r = radius * (ring / 4);
        const x = centerX + Math.cos(angle) * r;
        const y = centerY + Math.sin(angle) * r;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();
    }

    // Draw axes
    for (let i = 0; i < sides; i++) {
      const angle = i * angleStep - Math.PI / 2;
      const x = centerX + Math.cos(angle) * radius;
      const y = centerY + Math.sin(angle) * radius;
      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.lineTo(x, y);
      ctx.stroke();

      // Draw text
      ctx.fillStyle = '#64748b';
      ctx.font = '10px unquote("Orbitron")';
      ctx.textAlign = x > centerX ? 'left' : x < centerX ? 'right' : 'center';
      ctx.textBaseline = y > centerY ? 'top' : y < centerY ? 'bottom' : 'middle';
      const tx = centerX + Math.cos(angle) * (radius + 8);
      const ty = centerY + Math.sin(angle) * (radius + 8);
      
      let label = labels[i];
      if (label.length > 8) label = label.substring(0, 7) + '.';
      ctx.fillText(label, tx, ty);
    }

    // Draw data polygon
    ctx.beginPath();
    for (let i = 0; i < sides; i++) {
      const angle = i * angleStep - Math.PI / 2;
      const value = skills[labels[i]] || 0.1; // 0.1 minimum
      const r = radius * value;
      const x = centerX + Math.cos(angle) * r;
      const y = centerY + Math.sin(angle) * r;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.closePath();
    
    // Fill and Stroke
    ctx.fillStyle = 'rgba(6, 182, 212, 0.3)';
    ctx.fill();
    ctx.strokeStyle = '#06b6d4';
    ctx.lineWidth = 2;
    ctx.stroke();
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
