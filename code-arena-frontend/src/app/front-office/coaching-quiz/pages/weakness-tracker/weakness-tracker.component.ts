import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import {
  AiMemoryService,
  WeaknessProfile,
  AnalysisResult,
  MistakeRecord,
  CodeSubmission
} from '../../services/ai-memory.service';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-weakness-tracker',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent],
  templateUrl: './weakness-tracker.component.html',
  styleUrls: ['./weakness-tracker.component.css']
})
export class WeaknessTrackerComponent implements OnInit {

  // Student config
  studentId = 'student_001';

  // Code submission
  codeInput = '';
  selectedLanguage = 'JAVA';
  isAnalyzing = false;
  analysisResult: AnalysisResult | null = null;

  // Profile data
  profile: WeaknessProfile | null = null;
  mistakes: MistakeRecord[] = [];
  isLoadingProfile = false;

  // UI state
  activeTab: 'analyze' | 'dashboard' | 'history' = 'analyze';
  particles = Array.from({ length: 15 }, (_, i) => ({
    x: Math.random() * 100,
    y: Math.random() * 100,
    delay: Math.random() * 8,
    size: Math.random() * 4 + 2,
  }));

  constructor(
    private aiMemory: AiMemoryService,
    private alert: AlertService
  ) {}

  ngOnInit() {
    this.loadProfile();
  }

  setTab(tab: 'analyze' | 'dashboard' | 'history') {
    this.activeTab = tab;
    if (tab === 'dashboard') this.loadProfile();
    if (tab === 'history') this.loadMistakes();
  }

  analyzeCode() {
    if (!this.codeInput.trim()) return;
    this.isAnalyzing = true;
    this.analysisResult = null;

    const submission: CodeSubmission = {
      studentId: this.studentId,
      language: this.selectedLanguage,
      code: this.codeInput
    };

    this.aiMemory.analyzeCode(submission).subscribe({
      next: (result) => {
        this.analysisResult = result;
        this.isAnalyzing = false;
        if (result.errors_detected) {
          this.alert.warning('Analysis Complete', `Found ${result.errors.length} issue(s) in your code.`);
        } else {
          this.alert.success('Clean Code!', 'No common issues detected.');
        }
        // Refresh profile after analysis
        this.loadProfile();
      },
      error: (err) => {
        this.isAnalyzing = false;
        this.alert.error('Analysis Failed', 'Could not reach the AI analysis service.');
      }
    });
  }

  loadProfile() {
    this.isLoadingProfile = true;
    this.aiMemory.getProfile(this.studentId).subscribe({
      next: (p) => { this.profile = p; this.isLoadingProfile = false; },
      error: () => { this.isLoadingProfile = false; }
    });
  }

  loadMistakes() {
    this.aiMemory.getMistakes(this.studentId).subscribe({
      next: (m) => this.mistakes = m,
      error: () => {}
    });
  }

  getScoreColor(): string {
    if (!this.profile) return '#64748b';
    const s = this.profile.overall_score;
    if (s >= 80) return '#10b981';
    if (s >= 50) return '#f59e0b';
    return '#f43f5e';
  }

  getScoreLabel(): string {
    if (!this.profile) return 'N/A';
    const s = this.profile.overall_score;
    if (s >= 80) return 'EXCELLENT';
    if (s >= 60) return 'GOOD';
    if (s >= 40) return 'NEEDS WORK';
    return 'CRITICAL';
  }

  getSeverityColor(severity: string): string {
    switch (severity) {
      case 'high': return '#f43f5e';
      case 'medium': return '#f59e0b';
      case 'low': return '#10b981';
      default: return '#64748b';
    }
  }

  getCategoryIcon(category: string): string {
    const icons: {[key: string]: string} = {
      'Syntax Error': '⌨️',
      'Logic Error': '🧠',
      'OOP Error': '🏗️',
      'SQL Error': '🗄️',
      'Code Style': '✨',
      'Error Handling': '🛡️',
      'Performance': '⚡',
      'Security': '🔒',
    };
    return icons[category] || '📋';
  }

  getLanguageKeys(): string[] {
    return this.profile ? Object.keys(this.profile.language_weaknesses) : [];
  }

  getErrorTypeKeys(): string[] {
    return this.profile ? Object.keys(this.profile.error_type_frequency) : [];
  }

  getMaxTrendErrors(): number {
    if (!this.profile || !this.profile.improvement_trend.length) return 1;
    return Math.max(...this.profile.improvement_trend.map(t => t.errors), 1);
  }

  getTrendBarHeight(errors: number): number {
    return (errors / this.getMaxTrendErrors()) * 100;
  }

  getMaxErrorFreq(): number {
    if (!this.profile) return 1;
    const vals = Object.values(this.profile.error_type_frequency);
    return Math.max(...vals, 1);
  }
}
