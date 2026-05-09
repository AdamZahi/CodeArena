import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiSemanticSearchService, SemanticSearchResult } from '../../services/ai/ai-semantic-search.service';

@Component({
  selector: 'app-semantic-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './semantic-search.component.html',
  styleUrl: './semantic-search.component.css'
})
export class SemanticSearchComponent {
  @Input() channelId!: number;

  query = '';
  results: SemanticSearchResult[] = [];
  isLoading = false;
  searched = false;

  constructor(private semanticSearch: AiSemanticSearchService) {}

  search(): void {
    if (!this.query.trim() || !this.channelId) return;
    this.isLoading = true;
    this.searched = false;

    this.semanticSearch.search(this.query.trim(), this.channelId).subscribe({
      next: (res) => {
        this.results = res.results;
        this.isLoading = false;
        this.searched = true;
      },
      error: () => {
        this.isLoading = false;
        this.searched = true;
      }
    });
  }

  clear(): void {
    this.query = '';
    this.results = [];
    this.searched = false;
  }

  getScoreColor(score: number): string {
    if (score >= 0.7) return '#4dff88';
    if (score >= 0.5) return '#22c7ff';
    return '#ffd96b';
  }
}