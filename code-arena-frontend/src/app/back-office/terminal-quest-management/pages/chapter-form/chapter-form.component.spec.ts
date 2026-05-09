import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { ChapterFormComponent } from './chapter-form.component';
import { AdminTqService } from '../../services/admin-tq.service';

describe('ChapterFormComponent', () => {
  let component: ChapterFormComponent;
  let adminTqMock: any;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    adminTqMock = {
      createChapter: jasmine.createSpy('createChapter').and.returnValue(of({})),
      updateChapter: jasmine.createSpy('updateChapter').and.returnValue(of({})),
      getChapterById: jasmine.createSpy('getChapterById').and.returnValue(of({
        title: 'Ch1', description: 'desc', orderIndex: 1, isLocked: false
      }))
    };

    await TestBed.configureTestingModule({
      imports: [ChapterFormComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } },
        { provide: Router, useValue: routerSpy },
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(ChapterFormComponent, { set: { imports: [CommonModule, ReactiveFormsModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(ChapterFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('form should be invalid when empty', () => {
    component.form.reset();
    expect(component.form.invalid).toBeTrue();
  });

  it('form should be valid when required fields are filled', () => {
    component.form.setValue({
      title: 'Linux Basics',
      description: 'A great chapter',
      orderIndex: 1,
      isLocked: false
    });
    expect(component.form.valid).toBeTrue();
  });

  it('should be in create mode when no route id', () => {
    expect(component.isEditMode).toBeFalse();
  });

  it('isInvalid should return true for touched invalid field', () => {
    component.form.get('title')!.markAsTouched();
    component.form.get('title')!.setValue('');
    expect(component.isInvalid('title')).toBeTrue();
  });

  it('isInvalid should return false for valid field', () => {
    component.form.get('title')!.setValue('My Chapter');
    component.form.get('title')!.markAsTouched();
    expect(component.isInvalid('title')).toBeFalse();
  });

  it('should call createChapter when form is valid and not in edit mode', () => {
    component.form.setValue({ title: 'Ch', description: 'desc', orderIndex: 1, isLocked: false });
    component.save();
    expect(adminTqMock.createChapter).toHaveBeenCalled();
  });

  it('should not submit when form is invalid', () => {
    component.form.reset();
    component.save();
    expect(adminTqMock.createChapter).not.toHaveBeenCalled();
  });
});

describe('ChapterFormComponent (edit mode)', () => {
  let component: ChapterFormComponent;

  beforeEach(async () => {
    const adminTqMock = {
      getChapterById: jasmine.createSpy('getChapterById').and.returnValue(of({
        title: 'Existing', description: 'desc', orderIndex: 2, isLocked: true
      })),
      updateChapter: jasmine.createSpy('updateChapter').and.returnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [ChapterFormComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'c1' } } } },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(ChapterFormComponent, { set: { imports: [CommonModule, ReactiveFormsModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(ChapterFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be in edit mode when route has id', () => {
    expect(component.isEditMode).toBeTrue();
  });

  it('should patch form values from loaded chapter', () => {
    expect(component.form.get('title')!.value).toBe('Existing');
  });
});
