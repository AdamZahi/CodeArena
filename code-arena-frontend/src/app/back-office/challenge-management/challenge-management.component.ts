import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChallengeService } from '../../front-office/challenge/services/challenge.service';
import { HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-challenge-management',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './challenge-management.component.html',
  styleUrls: ['./challenge-management.component.css']
})
export class ChallengeManagementComponent implements OnInit {

  challenges: any[] = [];
  isLoading = true;
  deleteConfirmId: string | null = null;

  isEditMode = false;
  isSaving = false;
  showForm = false;

  formData: any = {
    title: '',
    description: '',
    difficulty: '',
    tags: '',
    language: '',
    testCases: []
  };

  errors: any = {};

  // Pagination
  currentPage = 0;
  pageSize = 8;
  totalPages = 0;
  totalItems = 0;
  pages: number[] = [];

  constructor(private challengeService: ChallengeService) {}

  ngOnInit(): void {
    this.loadChallenges();
  }

  loadChallenges(): void {
    this.isLoading = true;
    this.challengeService.getAll().subscribe({
      next: (res: any) => {
        this.challenges = res;
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  createChallenge(): void {
    this.isEditMode = false;
    this.showForm = true;
    this.formData = {
      title: '',
      description: '',
      difficulty: '',
      tags: '',
      language: '',
      testCases: []
    };
    this.errors = {};
  }

  editChallenge(challenge: any): void {
    this.isEditMode = true;
    this.showForm = true;
    // Deep copy to prevent mutating the original list data
    this.formData = {
      id: challenge.id,
      title: challenge.title || '',
      description: challenge.description || '',
      difficulty: challenge.difficulty || '',
      tags: challenge.tags || '',
      language: challenge.language || '',
      testCases: (challenge.testCases || []).map((tc: any) => ({
        input: tc.input || '',
        expectedOutput: tc.expectedOutput || '',
        isHidden: tc.isHidden || false
      }))
    };
    this.errors = {};
  }

  confirmDelete(id: string): void {
    this.deleteConfirmId = id;
  }

  cancelDelete(): void {
    this.deleteConfirmId = null;
  }

  deleteChallenge(id: string): void {
    // API Call to delete
    this.challengeService.deleteChallenge(id).subscribe({
      next: () => {
         this.loadChallenges();
         this.deleteConfirmId = null;
      },
      error: (e) => console.error('Delete failed', e)
    });
  }

  goBack(): void {
    this.showForm = false;
  }

  addTestCase(): void {
    this.formData.testCases.push({ input: '', expectedOutput: '', isHidden: false });
  }

  removeTestCase(index: number): void {
    this.formData.testCases.splice(index, 1);
  }

  validateForm(): boolean {
    this.errors = {};
    let isValid = true;
    if (!this.formData.title) { this.errors['title'] = 'Title is required'; isValid = false; }
    if (!this.formData.description) { this.errors['description'] = 'Description is required'; isValid = false; }
    if (!this.formData.difficulty) { this.errors['difficulty'] = 'Difficulty is required'; isValid = false; }
    return isValid;
  }

  clearError(field: string): void {
    delete this.errors[field];
  }

  onSubmit(): void {
    if (!this.validateForm()) return;
    this.isSaving = true;

    const payload = {
      title: this.formData.title,
      description: this.formData.description,
      difficulty: this.formData.difficulty,
      tags: this.formData.tags,
      language: this.formData.language,
      testCases: this.formData.testCases
    };

    if (this.isEditMode) {
       this.challengeService.updateChallenge(this.formData.id, payload).subscribe({
          next: () => {
             this.isSaving = false;
             this.showForm = false;
             this.loadChallenges();
          },
          error: (e) => { console.error('Update failed', e); this.isSaving = false; }
       });
    } else {
       this.challengeService.createChallenge(payload).subscribe({
          next: () => {
             this.isSaving = false;
             this.showForm = false;
             this.loadChallenges();
          },
          error: (e) => { console.error('Create failed', e); this.isSaving = false; }
       });
    }
  }
}
