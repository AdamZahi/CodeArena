import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { filter, take } from 'rxjs/operators';
import { SkillEngineService } from '../../services/skill-engine.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';
import { VoiceNavWidgetComponent } from '../../components/voice-nav-widget/voice-nav-widget.component';
import { SkillAnalysis } from '../../models/terminal-quest.model';

@Component({
  selector: 'app-skill-report',
  standalone: true,
  imports: [CommonModule, VoiceNavWidgetComponent],
  templateUrl: './skill-report.component.html',
  styleUrls: ['./skill-report.component.css']
})
export class SkillReportComponent implements OnInit, OnDestroy {
  analysis: SkillAnalysis | null = null;
  isLoading = true;
  userId = '';

  constructor(
    private readonly auth: AuthService,
    private readonly skillEngineService: SkillEngineService,
    private readonly router: Router,
    private readonly voiceNav: VoiceNavigationService
  ) {}

  ngOnInit(): void {
    this.auth.user$.pipe(
      filter(u => !!u),
      take(1)
    ).subscribe(user => {
      this.userId = user?.sub ?? '';
      this.skillEngineService.analyzePlayer().subscribe({
        next: (data) => {
          this.analysis  = data;
          this.isLoading = false;
          console.log('[skill-report] full analysis:', this.analysis);
          console.log('[skill-report] certificationReadiness:', this.analysis?.certificationReadiness);
        },
        error: () => { this.isLoading = false; }
      });
    });

    this.voiceNav.registerPageCommands('skill-report', (_cmd: string) => false);
    this.voiceNav.autoStart();
  }

  ngOnDestroy(): void {
    this.voiceNav.unregisterPageCommands('skill-report');
  }

  getSkillEntries(): [string, number][] {
    if (!this.analysis) return [];
    return Object.entries(this.analysis.skillProfile).sort((a, b) => b[1] - a[1]);
  }

  getCertEntries(): [string, any][] {
    if (!this.analysis?.certificationReadiness) return [];
    const cert = this.analysis.certificationReadiness;
    console.log('[skill-report] cert type:', typeof cert, cert);
    console.log('[skill-report] cert keys:', Object.keys(cert));
    const entries = Object.keys(cert).map(key => [key, cert[key]] as [string, any]);
    console.log('[skill-report] mapped entries:', entries);
    return entries;
  }

  getBarColor(score: number): string {
    if (score < 40) return '#ef4444';
    if (score < 70) return '#f59e0b';
    return '#4ade80';
  }

  goBack(): void {
    this.router.navigate(['/terminal-quest']);
  }
}
