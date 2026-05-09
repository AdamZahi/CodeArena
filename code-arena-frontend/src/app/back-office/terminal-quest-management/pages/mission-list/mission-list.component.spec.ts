import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MissionListComponent } from './mission-list.component';
import { AdminTqService } from '../../services/admin-tq.service';

describe('MissionListComponent', () => {
  let component: MissionListComponent;
  let adminTqMock: any;

  const mockMissions = [
    { id: 'm1', title: 'Mission 1', difficulty: 'EASY', context: 'ctx', task: 'do it', orderIndex: 1, isBoss: false, xpReward: 100 },
    { id: 'm2', title: 'Mission 2', difficulty: 'HARD', context: 'ctx2', task: 'do it 2', orderIndex: 2, isBoss: true, xpReward: 200 }
  ];

  beforeEach(async () => {
    adminTqMock = {
      getChapterById: jasmine.createSpy('getChapterById').and.returnValue(of({ id: 'c1', title: 'Ch1' })),
      getMissionsByChapter: jasmine.createSpy('getMissionsByChapter').and.returnValue(of(mockMissions)),
      deleteMission: jasmine.createSpy('deleteMission').and.returnValue(of(undefined))
    };

    await TestBed.configureTestingModule({
      imports: [MissionListComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'c1' } } } },
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(MissionListComponent, { set: { imports: [CommonModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(MissionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should load missions on init', () => {
    expect(adminTqMock.getMissionsByChapter).toHaveBeenCalledWith('c1');
    expect(component.missions.length).toBe(2);
  });

  it('should set isLoading = false after load', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('diffClass should return lowercase difficulty class', () => {
    expect(component.diffClass('HARD')).toBe('diff-hard');
    expect(component.diffClass('EASY')).toBe('diff-easy');
  });

  it('truncate should return original string when short', () => {
    expect(component.truncate('short')).toBe('short');
  });

  it('truncate should ellipsize long strings', () => {
    const long = 'a'.repeat(80);
    const result = component.truncate(long, 60);
    expect(result.length).toBe(61);
    expect(result.endsWith('…')).toBeTrue();
  });

  it('should set errorMsg on load error', () => {
    adminTqMock.getMissionsByChapter.and.returnValue(throwError(() => new Error('fail')));
    component.ngOnInit();
    expect(component.errorMsg).toBeTruthy();
  });
});
