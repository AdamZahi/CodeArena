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
      testCases: []
    };
    this.errors = {};
  }

  editChallenge(challenge: any): void {
    this.isEditMode = true;
    this.showForm = true;
    this.formData = { ...challenge };
    if (!this.formData.testCases) {
        this.formData.testCases = [];
    }
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

    if (this.isEditMode) {
       // Not implemented yet
       this.isSaving = false;
       this.showForm = false;
       this.loadChallenges();
    } else {
       this.challengeService.createChallenge(this.formData).subscribe({
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
