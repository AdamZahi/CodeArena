import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';

@Component({
  selector: 'app-chapter-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './chapter-form.component.html',
  styleUrls: ['./chapter-form.component.css']
})
export class ChapterFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  chapterId: string | null = null;
  isLoading = false;
  isSaving = false;
  errorMsg = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private adminTqService: AdminTqService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      title:       ['', Validators.required],
      description: ['', Validators.required],
      orderIndex:  [1, [Validators.required, Validators.min(1)]],
      isLocked:    [false]
    });

    this.chapterId = this.route.snapshot.paramMap.get('id');
    if (this.chapterId) {
      this.isEditMode = true;
      this.isLoading = true;
      this.adminTqService.getChapterById(this.chapterId).subscribe({
        next: (chapter) => {
          this.form.patchValue({
            title:       chapter.title,
            description: chapter.description,
            orderIndex:  chapter.orderIndex,
            isLocked:    chapter.isLocked
          });
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Failed to load chapter:', err);
          this.errorMsg = 'Failed to load chapter.';
          this.isLoading = false;
        }
      });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.errorMsg = '';
    const payload = this.form.value;

    const req = this.isEditMode
      ? this.adminTqService.updateChapter(this.chapterId!, payload)
      : this.adminTqService.createChapter(payload);

    req.subscribe({
      next: () => {
        this.router.navigate(['/admin/terminal-quest/chapters']);
      },
      error: (err) => {
        console.error('Save failed:', err);
        this.errorMsg = 'Failed to save chapter. Please try again.';
        this.isSaving = false;
      }
    });
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && c.touched);
  }
}
