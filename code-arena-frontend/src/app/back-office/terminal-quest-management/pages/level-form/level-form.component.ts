import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';

@Component({
  selector: 'app-level-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './level-form.component.html',
  styleUrls: ['./level-form.component.css']
})
export class LevelFormComponent implements OnInit {
  form!: FormGroup;
  chapterId = '';
  levelId: string | null = null;
  isEditMode = false;
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
    this.chapterId = this.route.snapshot.paramMap.get('chapterId') || '';
    this.levelId   = this.route.snapshot.paramMap.get('id');

    this.form = this.fb.group({
      title:           ['', Validators.required],
      scenario:        ['', Validators.required],
      acceptedAnswers: ['', Validators.required],
      hint:            [''],
      orderIndex:      [1, [Validators.required, Validators.min(1)]],
      difficulty:      ['EASY', Validators.required],
      isBoss:          [false],
      xpReward:        [100, [Validators.required, Validators.min(1)]]
    });

    if (this.levelId) {
      this.isEditMode = true;
      this.isLoading = true;
      // acceptedAnswers is not returned by the API — admin must re-enter it when editing
      this.form.get('acceptedAnswers')!.clearValidators();
      this.form.get('acceptedAnswers')!.updateValueAndValidity();

      this.adminTqService.getLevelById(this.levelId).subscribe({
        next: (level) => {
          this.form.patchValue({
            title:      level.title,
            scenario:   level.scenario,
            hint:       level.hint || '',
            orderIndex: level.orderIndex,
            difficulty: level.difficulty,
            isBoss:     level.isBoss,
            xpReward:   level.xpReward
          });
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Failed to load level:', err);
          this.errorMsg = 'Failed to load level.';
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
    const v = this.form.value;

    const payload: Record<string, unknown> = {
      chapterId:  this.chapterId,
      title:      v.title,
      scenario:   v.scenario,
      hint:       v.hint,
      orderIndex: v.orderIndex,
      difficulty: v.difficulty,
      isBoss:     v.isBoss,
      xpReward:   v.xpReward
    };
    if (v.acceptedAnswers?.trim()) {
      payload['acceptedAnswers'] = v.acceptedAnswers.trim();
    }

    const req = this.isEditMode
      ? this.adminTqService.updateLevel(this.levelId!, payload as never)
      : this.adminTqService.createLevel(payload as never);

    req.subscribe({
      next: () => {
        this.router.navigate(['/admin/terminal-quest/levels', this.chapterId]);
      },
      error: (err) => {
        console.error('Save failed:', err);
        this.errorMsg = 'Failed to save level. Check that acceptedAnswers is valid JSON.';
        this.isSaving = false;
      }
    });
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && c.touched);
  }
}
