import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { AdminTqService } from '../../services/admin-tq.service';

@Component({
  selector: 'app-mission-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './mission-form.component.html',
  styleUrls: ['./mission-form.component.css']
})
export class MissionFormComponent implements OnInit {
  form!: FormGroup;
  chapterId = '';
  missionId: string | null = null;
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
    this.missionId = this.route.snapshot.paramMap.get('id');

    this.form = this.fb.group({
      title:           ['', Validators.required],
      context:         ['', Validators.required],
      task:            ['', Validators.required],
      acceptedAnswers: ['', Validators.required],
      hint:            [''],
      orderIndex:      [1, [Validators.required, Validators.min(1)]],
      difficulty:      ['EASY', Validators.required],
      isBoss:          [false],
      xpReward:        [100, [Validators.required, Validators.min(1)]]
    });

    if (this.missionId) {
      this.isEditMode = true;
      this.isLoading = true;
      this.form.get('acceptedAnswers')!.clearValidators();
      this.form.get('acceptedAnswers')!.updateValueAndValidity();

      this.adminTqService.getMissionById(this.missionId).subscribe({
        next: (mission) => {
          this.form.patchValue({
            title:      mission.title,
            context:    mission.context,
            task:       mission.task,
            hint:       mission.hint || '',
            orderIndex: mission.orderIndex,
            difficulty: mission.difficulty,
            isBoss:     mission.isBoss,
            xpReward:   mission.xpReward
          });
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Failed to load mission:', err);
          this.errorMsg = 'Failed to load mission.';
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
      context:    v.context,
      task:       v.task,
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
      ? this.adminTqService.updateMission(this.missionId!, payload)
      : this.adminTqService.createMission(payload);

    req.subscribe({
      next: () => {
        this.router.navigate(['/admin/terminal-quest/missions', this.chapterId]);
      },
      error: (err) => {
        console.error('Save failed:', err);
        this.errorMsg = 'Failed to save mission. Check that acceptedAnswers is valid JSON.';
        this.isSaving = false;
      }
    });
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && c.touched);
  }
}
