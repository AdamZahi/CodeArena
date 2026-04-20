import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChallengeService } from '../../services/challenge.service';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-challenge-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './challenge-list.component.html',
  styleUrls: ['./challenge-list.component.css']
})
export class ChallengeListComponent implements OnInit {
  public challenges: any[] = [];
  public filteredChallenges: any[] = [];
  public isLoading = true;
  public searchTerm = '';
  public selectedDifficulty = '';

  constructor(
    private challengeService: ChallengeService,
    private router: Router,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadChallenges();
  }

  public loadChallenges(): void {
    this.isLoading = true;
    this.challengeService.getAll().subscribe({
      next: (data) => {
        this.challenges = data || [];
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading challenges:', err);
        this.isLoading = false;
      }
    });
  }

  public goToChallenge(id: string): void {
    this.router.navigate(['/challenge', id]);
  }

  public filterByDifficulty(diff: string): void {
    this.selectedDifficulty = diff;
    this.applyFilters();
  }

  public onSearch(): void {
    this.applyFilters();
  }

  public applyFilters(): void {
    if (!this.challenges) return;
    this.filteredChallenges = this.challenges.filter(c => {
      const matchesDifficulty = !this.selectedDifficulty || c.difficulty === this.selectedDifficulty;
      const term = this.searchTerm.toLowerCase();
      const matchesSearch = !this.searchTerm || 
        (c.title && c.title.toLowerCase().includes(term)) ||
        (c.tags && c.tags.toLowerCase().includes(term));
      return matchesDifficulty && matchesSearch;
    });
  }

  public getTags(tags: string): string[] {
    if (!tags) return [];
    return tags.split(',').map((t: string) => t.trim()).filter(t => t.length > 0);
  }

  public getLanguageName(id: string): string {
    switch ((id || '').toLowerCase()) {
      case 'python':     return 'PYTHON 3.12';
      case 'javascript': return 'JS (NODE 20)';
      case 'java':       return 'JAVA 15';
      case 'go':         return 'GO 1.16';
      case 'rust':       return 'RUST 1.50';
      case 'csharp':     return 'C# (.NET 5)';
      case 'php':        return 'PHP 8.2';
      case 'bash':       return 'BASH 5.2';
      default:           return (id || 'UNKNOWN').toUpperCase();
    }
  }
}
