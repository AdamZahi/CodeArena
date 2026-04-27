import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { LevelFormComponent } from './level-form.component';
import { AdminTqService } from '../../services/admin-tq.service';

describe('LevelFormComponent (create mode)', () => {
  let component: LevelFormComponent;
  let adminTqMock: any;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    adminTqMock = {
      createLevel: jasmine.createSpy('createLevel').and.returnValue(of({})),
      getLevelById: jasmine.createSpy('getLevelById').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [LevelFormComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (key: string) => key === 'chapterId' ? 'c1' : null } } }
        },
        { provide: Router, useValue: routerSpy },
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(LevelFormComponent, { set: { imports: [CommonModule, ReactiveFormsModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(LevelFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should be in create mode', () => {
    expect(component.isEditMode).toBeFalse();
  });

  it('form should be invalid when empty', () => {
    component.form.reset();
    expect(component.form.invalid).toBeTrue();
  });

  it('form should be valid with all required fields', () => {
    component.form.patchValue({
      title: 'Level X',
      scenario: 'Scenario text',
      acceptedAnswers: 'ls -la',
      hint: '',
      orderIndex: 1,
      difficulty: 'EASY',
      isBoss: false,
      xpReward: 100
    });
    expect(component.form.valid).toBeTrue();
  });

  it('isInvalid should return true for touched invalid field', () => {
    component.form.get('title')!.markAsTouched();
    component.form.get('title')!.setValue('');
    expect(component.isInvalid('title')).toBeTrue();
  });

  it('should call createLevel on valid save', () => {
    component.form.patchValue({
      title: 'L', scenario: 'S', acceptedAnswers: 'ls',
      hint: '', orderIndex: 1, difficulty: 'EASY', isBoss: false, xpReward: 50
    });
    component.save();
    expect(adminTqMock.createLevel).toHaveBeenCalled();
  });

  it('should not submit with invalid form', () => {
    component.form.reset();
    component.save();
    expect(adminTqMock.createLevel).not.toHaveBeenCalled();
  });
});

describe('LevelFormComponent (edit mode)', () => {
  let component: LevelFormComponent;

  beforeEach(async () => {
    const adminTqMock = {
      getLevelById: jasmine.createSpy('getLevelById').and.returnValue(of({
        title: 'Existing Level', scenario: 'Do this', hint: '',
        orderIndex: 2, difficulty: 'MEDIUM', isBoss: false, xpReward: 75
      })),
      updateLevel: jasmine.createSpy('updateLevel').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [LevelFormComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (key: string) => key === 'chapterId' ? 'c1' : 'l1' } } }
        },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(LevelFormComponent, { set: { imports: [CommonModule, ReactiveFormsModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(LevelFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be in edit mode', () => {
    expect(component.isEditMode).toBeTrue();
  });

  it('should patch form with loaded level values', () => {
    expect(component.form.get('title')!.value).toBe('Existing Level');
    expect(component.form.get('difficulty')!.value).toBe('MEDIUM');
  });
});
