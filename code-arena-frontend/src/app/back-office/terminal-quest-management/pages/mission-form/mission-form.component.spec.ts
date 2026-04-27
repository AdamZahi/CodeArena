import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { MissionFormComponent } from './mission-form.component';
import { AdminTqService } from '../../services/admin-tq.service';

describe('MissionFormComponent (create mode)', () => {
  let component: MissionFormComponent;
  let adminTqMock: any;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    adminTqMock = {
      createMission: jasmine.createSpy('createMission').and.returnValue(of({})),
      getMissionById: jasmine.createSpy('getMissionById').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [MissionFormComponent],
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
      .overrideComponent(MissionFormComponent, { set: { imports: [CommonModule, ReactiveFormsModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(MissionFormComponent);
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

  it('form should be valid when required fields filled', () => {
    component.form.patchValue({
      title: 'Mission X',
      context: 'Context here',
      task: 'Do the task',
      acceptedAnswers: 'ls -la',
      hint: '',
      orderIndex: 1,
      difficulty: 'EASY',
      isBoss: false,
      xpReward: 100
    });
    expect(component.form.valid).toBeTrue();
  });

  it('isInvalid should detect invalid touched field', () => {
    component.form.get('title')!.markAsTouched();
    component.form.get('title')!.setValue('');
    expect(component.isInvalid('title')).toBeTrue();
  });

  it('should call createMission on valid form save', () => {
    component.form.patchValue({
      title: 'T', context: 'C', task: 'Do', acceptedAnswers: 'ls',
      hint: '', orderIndex: 1, difficulty: 'EASY', isBoss: false, xpReward: 100
    });
    component.save();
    expect(adminTqMock.createMission).toHaveBeenCalled();
  });

  it('should not submit when form is invalid', () => {
    component.form.reset();
    component.save();
    expect(adminTqMock.createMission).not.toHaveBeenCalled();
  });
});

describe('MissionFormComponent (edit mode)', () => {
  let component: MissionFormComponent;

  beforeEach(async () => {
    const adminTqMock = {
      getMissionById: jasmine.createSpy('getMissionById').and.returnValue(of({
        title: 'Existing', context: 'ctx', task: 'task',
        hint: '', orderIndex: 1, difficulty: 'HARD', isBoss: false, xpReward: 150
      })),
      updateMission: jasmine.createSpy('updateMission').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [MissionFormComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (key: string) => key === 'chapterId' ? 'c1' : 'm1' } } }
        },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(MissionFormComponent, { set: { imports: [CommonModule, ReactiveFormsModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(MissionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be in edit mode when mission id present', () => {
    expect(component.isEditMode).toBeTrue();
  });

  it('should patch form with loaded mission values', () => {
    expect(component.form.get('title')!.value).toBe('Existing');
    expect(component.form.get('difficulty')!.value).toBe('HARD');
  });
});
