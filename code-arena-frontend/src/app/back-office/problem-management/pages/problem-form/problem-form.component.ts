import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ChallengeService } from '../../../../front-office/challenge/services/challenge.service';

@Component({
  selector: 'app-problem-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './problem-form.component.html',
  styleUrl: './problem-form.component.css'
})
export class ProblemFormComponent implements OnInit {

  isEditMode = false;
  isSaving = false;
  isLoading = false;

  formData: any = {
    title: '',
    description: '',
    difficulty: '',
    tags: '',
    language: '',
    testCases: []
  };

  errors: any = {};

  // Success feedback
  showSuccess = false;
  successMessage = '';

  constructor(
    private challengeService: ChallengeService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.isLoading = true;
      this.challengeService.getById(id).subscribe({
        next: (challenge: any) => {
          this.formData = { ...challenge };
          if (!this.formData.testCases) {
            this.formData.testCases = [];
          }
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          this.router.navigate(['/admin/problems']);
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/admin/problems']);
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

    if (!this.formData.title || this.formData.title.trim() === '') {
      this.errors['title'] = 'Title is required';
      isValid = false;
    }
    if (!this.formData.description || this.formData.description.trim() === '') {
      this.errors['description'] = 'Description is required';
      isValid = false;
    }
    if (!this.formData.difficulty) {
      this.errors['difficulty'] = 'Difficulty is required';
      isValid = false;
    }

    // Validate test cases
    if (this.formData.testCases && this.formData.testCases.length > 0) {
      for (let i = 0; i < this.formData.testCases.length; i++) {
        const tc = this.formData.testCases[i];
        if (!tc.input || tc.input.trim() === '') {
          this.errors[`tc_input_${i}`] = 'Input is required';
          isValid = false;
        }
        if (!tc.expectedOutput || tc.expectedOutput.trim() === '') {
          this.errors[`tc_output_${i}`] = 'Expected output is required';
          isValid = false;
        }
      }
    }

    return isValid;
  }

  clearError(field: string): void {
    delete this.errors[field];
  }

  onSubmit(): void {
    if (!this.validateForm()) return;
    this.isSaving = true;

    if (this.isEditMode) {
      this.challengeService.updateChallenge(this.formData.id, this.formData).subscribe({
        next: () => {
          this.isSaving = false;
          this.showSuccessMessage('Problem updated successfully!');
          setTimeout(() => this.router.navigate(['/admin/problems']), 1500);
        },
        error: (e) => {
          console.error('Update failed', e);
          this.errors['submit'] = 'Failed to update problem. Please try again.';
          this.isSaving = false;
        }
      });
    } else {
      this.challengeService.createChallenge(this.formData).subscribe({
        next: () => {
          this.isSaving = false;
          this.showSuccessMessage('Problem created successfully!');
          setTimeout(() => this.router.navigate(['/admin/problems']), 1500);
        },
        error: (e) => {
          console.error('Create failed', e);
          this.errors['submit'] = 'Failed to create problem. Please try again.';
          this.isSaving = false;
        }
      });
    }
  }

  showSuccessMessage(msg: string): void {
    this.successMessage = msg;
    this.showSuccess = true;
    setTimeout(() => this.showSuccess = false, 3000);
  }
}
