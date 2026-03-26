import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ChallengeService } from '../../../../front-office/challenge/services/challenge.service';

@Component({
  selector: 'app-problem-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './problem-dashboard.component.html',
  styleUrl: './problem-dashboard.component.css'
})
export class ProblemDashboardComponent implements OnInit {

  challenges: any[] = [];
  isLoading = true;

  // Stats
  total = 0;
  totalEasy = 0;
  totalMedium = 0;
  totalHard = 0;
  totalTestCases = 0;
  avgTestCases = 0;

  // Language breakdown
  languageStats: { name: string; count: number; percent: number }[] = [];

  // Top tags
  tagStats: { name: string; count: number }[] = [];

  // Recent challenges
  recentChallenges: any[] = [];

  // Challenges without test cases
  noTestCaseChallenges: any[] = [];

  constructor(
    private challengeService: ChallengeService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.challengeService.getAll().subscribe({
      next: (res: any[]) => {
        this.challenges = res;
        this.computeStats();
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  computeStats(): void {
    this.total = this.challenges.length;
    this.totalEasy = this.challenges.filter(c => c.difficulty === 'EASY').length;
    this.totalMedium = this.challenges.filter(c => c.difficulty === 'MEDIUM').length;
    this.totalHard = this.challenges.filter(c => c.difficulty === 'HARD').length;

    this.totalTestCases = this.challenges.reduce((sum, c) => sum + (c.testCases?.length || 0), 0);
    this.avgTestCases = this.total > 0 ? Math.round((this.totalTestCases / this.total) * 10) / 10 : 0;

    // Language stats
    const langMap: Record<string, number> = {};
    this.challenges.forEach(c => {
      const lang = c.language || 'Unspecified';
      langMap[lang] = (langMap[lang] || 0) + 1;
    });
    this.languageStats = Object.entries(langMap)
      .map(([name, count]) => ({
        name,
        count,
        percent: Math.round((count / this.total) * 100)
      }))
      .sort((a, b) => b.count - a.count);

    // Tag stats
    const tagMap: Record<string, number> = {};
    this.challenges.forEach(c => {
      if (c.tags) {
        c.tags.split(',').map((t: string) => t.trim()).filter((t: string) => t).forEach((tag: string) => {
          tagMap[tag] = (tagMap[tag] || 0) + 1;
        });
      }
    });
    this.tagStats = Object.entries(tagMap)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10);

    // Recent challenges
    this.recentChallenges = [...this.challenges]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5);

    // Challenges without test cases
    this.noTestCaseChallenges = this.challenges.filter(c => !c.testCases || c.testCases.length === 0);
  }

  goToList(): void {
    this.router.navigate(['/admin/problems']);
  }

  editChallenge(id: number): void {
    this.router.navigate(['/admin/problems/edit', id]);
  }

  getDifficultyPercent(difficulty: string): number {
    if (this.total === 0) return 0;
    const count = this.challenges.filter(c => c.difficulty === difficulty).length;
    return Math.round((count / this.total) * 100);
  }
}
