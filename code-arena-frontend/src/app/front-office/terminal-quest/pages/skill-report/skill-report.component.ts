import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { SkillEngineService } from '../../services/skill-engine.service';
import { SkillAnalysis } from '../../models/terminal-quest.model';

@Component({
  selector: 'app-skill-report',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skill-report.component.html',
  styleUrls: ['./skill-report.component.css']
})
export class SkillReportComponent implements OnInit {
  analysis: SkillAnalysis | null = null;
  isLoading = true;

  constructor(
    private readonly skillEngineService: SkillEngineService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.skillEngineService.analyzePlayer('test-user-001').subscribe({
      next: (data) => {
        this.analysis  = data;
        this.isLoading = false;
        console.log('[skill-report] full analysis:', this.analysis);
        console.log('[skill-report] certificationReadiness:', this.analysis?.certificationReadiness);
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  getSkillEntries(): [string, number][] {
    if (!this.analysis) return [];
    return Object.entries(this.analysis.skillProfile)
      .sort((a, b) => b[1] - a[1]);
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
