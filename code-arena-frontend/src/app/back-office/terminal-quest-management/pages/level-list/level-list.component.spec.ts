import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LevelListComponent } from './level-list.component';
import { AdminTqService } from '../../services/admin-tq.service';

describe('LevelListComponent', () => {
  let component: LevelListComponent;
  let adminTqMock: any;

  const mockLevels = [
    { id: 'l1', title: 'Level 1', difficulty: 'EASY', scenario: 'Do this', orderIndex: 1, isBoss: false, xpReward: 50 },
    { id: 'l2', title: 'Level 2', difficulty: 'MEDIUM', scenario: 'Do that', orderIndex: 2, isBoss: false, xpReward: 75 }
  ];

  beforeEach(async () => {
    adminTqMock = {
      getChapterById: jasmine.createSpy('getChapterById').and.returnValue(of({ id: 'c1', title: 'Ch1' })),
      getLevelsByChapter: jasmine.createSpy('getLevelsByChapter').and.returnValue(of(mockLevels)),
      deleteLevel: jasmine.createSpy('deleteLevel').and.returnValue(of(undefined))
    };

    await TestBed.configureTestingModule({
      imports: [LevelListComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'c1' } } } },
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(LevelListComponent, { set: { imports: [CommonModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(LevelListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should load levels on init', () => {
    expect(adminTqMock.getLevelsByChapter).toHaveBeenCalledWith('c1');
    expect(component.levels.length).toBe(2);
  });

  it('should set isLoading = false after load', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('diffClass should return lowercase css class', () => {
    expect(component.diffClass('MEDIUM')).toBe('diff-medium');
  });

  it('truncate should not change short strings', () => {
    expect(component.truncate('hello')).toBe('hello');
  });

  it('truncate should truncate long strings', () => {
    const s = 'x'.repeat(80);
    expect(component.truncate(s).endsWith('…')).toBeTrue();
  });

  it('should set errorMsg on load error', () => {
    adminTqMock.getLevelsByChapter.and.returnValue(throwError(() => new Error('err')));
    component.ngOnInit();
    expect(component.errorMsg).toBeTruthy();
  });
});
