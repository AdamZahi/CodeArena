import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { TqDashboardComponent } from './tq-dashboard.component';
import { AdminTqService } from '../../services/admin-tq.service';

describe('TqDashboardComponent', () => {
  let component: TqDashboardComponent;
  let adminTqMock: any;

  const mockStats = {
    totalActivePlayers: 100,
    totalSurvivalSessions: 40,
    totalStoryAttempts: 300,
    totalStoryCompletions: 195,
    overallCompletionRate: 0.65,
    chapterStats: []
  };

  beforeEach(async () => {
    adminTqMock = {
      getGlobalStats: jasmine.createSpy('getGlobalStats').and.returnValue(of(mockStats)),
      getChapters: jasmine.createSpy('getChapters').and.returnValue(of([])),
      getOverview: jasmine.createSpy('getOverview').and.returnValue(of({ difficultyBreakdown: [] })),
      getLeaderboard: jasmine.createSpy('getLeaderboard').and.returnValue(of({ content: [] })),
      searchPlayers: jasmine.createSpy('searchPlayers').and.returnValue(of([])),
      exportGlobalPdf: jasmine.createSpy('exportGlobalPdf').and.returnValue(of(new Blob()))
    };

    await TestBed.configureTestingModule({
      imports: [TqDashboardComponent],
      providers: [
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(TqDashboardComponent, { set: { imports: [CommonModule, FormsModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(TqDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should load global stats on init', () => {
    expect(adminTqMock.getGlobalStats).toHaveBeenCalled();
    expect(component.globalStats).toEqual(mockStats);
  });

  it('should load chapters on init', () => {
    expect(adminTqMock.getChapters).toHaveBeenCalled();
  });

  it('should set isLoading = false after chapters load', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('completionRatePct should format fraction as percentage string', () => {
    component.globalStats = { ...mockStats, overallCompletionRate: 0.65 };
    expect(component.completionRatePct).toBe('65.0');
  });

  it('completionRatePct should return 0.0 when no stats', () => {
    component.globalStats = null;
    expect(component.completionRatePct).toBe('0.0');
  });

  it('should search players', () => {
    component.searchQuery = 'alice';
    component.searchPlayers();
    expect(adminTqMock.searchPlayers).toHaveBeenCalledWith('alice');
    expect(component.searchResults).toEqual([]);
  });

  it('should not search when query is empty', () => {
    component.searchQuery = '  ';
    component.searchPlayers();
    expect(component.searchResults).toBeNull();
  });

  it('should set isLoading = false on chapters error', () => {
    adminTqMock.getChapters.and.returnValue(throwError(() => new Error('fail')));
    component.isLoading = true;
    component.ngOnInit();
    expect(component.isLoading).toBeFalse();
  });
});
