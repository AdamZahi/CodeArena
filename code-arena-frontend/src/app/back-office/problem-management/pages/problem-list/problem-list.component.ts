import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ChallengeService } from '../../../../front-office/challenge/services/challenge.service';

@Component({
  selector: 'app-problem-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './problem-list.component.html',
  styleUrl: './problem-list.component.css'
})
export class ProblemListComponent implements OnInit {

  challenges: any[] = [];
  filteredChallenges: any[] = [];
  isLoading = true;
  deleteConfirmId: number | null = null;

  // Filter & Search
  searchTerm = '';
  filterDifficulty = '';

  // Pagination
  currentPage = 0;
  pageSize = 8;
  totalPages = 0;
  pages: number[] = [];

  // Stats
  totalEasy = 0;
  totalMedium = 0;
  totalHard = 0;

  constructor(
    private challengeService: ChallengeService,
    public router: Router
  ) {}

  ngOnInit(): void {
    this.loadChallenges();
  }

  loadChallenges(): void {
    this.isLoading = true;
    this.challengeService.getAll().subscribe({
      next: (res: any[]) => {
        this.challenges = res;
        this.computeStats();
        this.applyFilters();
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  computeStats(): void {
    this.totalEasy = this.challenges.filter(c => c.difficulty === 'EASY').length;
    this.totalMedium = this.challenges.filter(c => c.difficulty === 'MEDIUM').length;
    this.totalHard = this.challenges.filter(c => c.difficulty === 'HARD').length;
  }

  applyFilters(): void {
    let result = [...this.challenges];

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(c =>
        c.title?.toLowerCase().includes(term) ||
        c.tags?.toLowerCase().includes(term) ||
        c.language?.toLowerCase().includes(term)
      );
    }

    if (this.filterDifficulty) {
      result = result.filter(c => c.difficulty === this.filterDifficulty);
    }

    this.filteredChallenges = result;
    this.totalPages = Math.ceil(this.filteredChallenges.length / this.pageSize);
    this.pages = Array.from({ length: this.totalPages }, (_, i) => i);
    if (this.currentPage >= this.totalPages) this.currentPage = 0;
  }

  get paginatedChallenges(): any[] {
    const start = this.currentPage * this.pageSize;
    return this.filteredChallenges.slice(start, start + this.pageSize);
  }

  onSearch(event: Event): void {
    this.searchTerm = (event.target as HTMLInputElement).value;
    this.currentPage = 0;
    this.applyFilters();
  }

  onFilterDifficulty(difficulty: string): void {
    this.filterDifficulty = this.filterDifficulty === difficulty ? '' : difficulty;
    this.currentPage = 0;
    this.applyFilters();
  }

  createChallenge(): void {
    this.router.navigate(['/admin/problems/new']);
  }

  editChallenge(id: number): void {
    this.router.navigate(['/admin/problems/edit', id]);
  }

  confirmDelete(id: number): void {
    this.deleteConfirmId = id;
  }

  cancelDelete(): void {
    this.deleteConfirmId = null;
  }

  deleteChallenge(id: number): void {
    this.challengeService.deleteChallenge(id.toString()).subscribe({
      next: () => {
        this.challenges = this.challenges.filter(c => c.id !== id);
        this.computeStats();
        this.applyFilters();
        this.deleteConfirmId = null;
      },
      error: (e) => {
        console.error('Delete failed', e);
        this.deleteConfirmId = null;
      }
    });
  }

  getDifficultyClass(difficulty: string): string {
    switch (difficulty) {
      case 'EASY': return 'diff-easy';
      case 'MEDIUM': return 'diff-medium';
      case 'HARD': return 'diff-hard';
      default: return '';
    }
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
  }

  nextPage(): void { this.goToPage(this.currentPage + 1); }
  prevPage(): void { this.goToPage(this.currentPage - 1); }

  goToDashboard(): void {
    this.router.navigate(['/admin/problems/dashboard']);
  }
}
