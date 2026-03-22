import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChallengeService } from '../../services/challenge.service';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-challenge-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './challenge-list.component.html',
  styleUrls: ['./challenge-list.component.css']
})
export class ChallengeListComponent implements OnInit {
  challenges: any[] = [];
  filteredChallenges: any[] = [];
  isLoading = true;
  searchTerm = '';
  selectedDifficulty = '';

  constructor(
    private challengeService: ChallengeService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.challengeService.getAll().subscribe({
      next: (data) => {
        this.challenges = data;
        this.filteredChallenges = data;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  goToChallenge(id: string): void {
    this.router.navigate(['/challenge', id]);
  }

  filterByDifficulty(diff: string): void {
    this.selectedDifficulty = diff;
    this.applyFilters();
  }

  onSearch(): void {
    this.applyFilters();
  }

  applyFilters(): void {
    this.filteredChallenges = this.challenges.filter(c => {
      const matchesDifficulty = this.selectedDifficulty === '' || c.difficulty === this.selectedDifficulty;
      const matchesSearch = this.searchTerm === '' || c.title.toLowerCase().includes(this.searchTerm.toLowerCase());
      return matchesDifficulty && matchesSearch;
    });
  }

  getTags(tags: string): string[] {
    if (!tags) return [];
    return tags.split(',').map((t: string) => t.trim()).filter((t: string) => t.length > 0);
  }
}
