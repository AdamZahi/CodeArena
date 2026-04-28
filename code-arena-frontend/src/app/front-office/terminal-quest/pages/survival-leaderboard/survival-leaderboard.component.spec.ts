import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { of, throwError } from 'rxjs';
import { AuthService } from '@auth0/auth0-angular';
import { SurvivalLeaderboardComponent } from './survival-leaderboard.component';
import { TerminalQuestService } from '../../services/terminal-quest.service';

const MOCK_USER_SUB = 'auth0|test-user';

describe('SurvivalLeaderboardComponent', () => {
  let component: SurvivalLeaderboardComponent;
  let tqServiceMock: any;

  const mockLeaderboard = [
    { id: 'lb1', userId: 'u1', bestWave: 10, bestScore: 500 },
    { id: 'lb2', userId: 'u2', bestWave: 8, bestScore: 400 },
    { id: 'lb3', userId: MOCK_USER_SUB, bestWave: 5, bestScore: 200 }
  ];

  const mockUserRanking = {
    id: 'lb3', userId: MOCK_USER_SUB, bestWave: 5, bestScore: 200
  };

  beforeEach(async () => {
    tqServiceMock = {
      getLeaderboard: jasmine.createSpy('getLeaderboard').and.returnValue(of(mockLeaderboard)),
      getMyRanking: jasmine.createSpy('getMyRanking').and.returnValue(of(mockUserRanking))
    };

    const authMock = { user$: of({ sub: MOCK_USER_SUB }) };

    await TestBed.configureTestingModule({
      imports: [SurvivalLeaderboardComponent],
      providers: [
        { provide: AuthService, useValue: authMock },
        { provide: TerminalQuestService, useValue: tqServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(SurvivalLeaderboardComponent, { set: { imports: [CommonModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(SurvivalLeaderboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should set userId from auth user', () => {
    expect(component.userId).toBe(MOCK_USER_SUB);
  });

  it('should load leaderboard on init', () => {
    expect(tqServiceMock.getLeaderboard).toHaveBeenCalled();
    expect(component.leaderboard.length).toBe(3);
  });

  it('should load user ranking on init via getMyRanking', () => {
    expect(tqServiceMock.getMyRanking).toHaveBeenCalled();
    expect(component.userRanking).toEqual(mockUserRanking);
  });

  it('should set isLoading = false after data loads', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('should return correct rank for user in leaderboard', () => {
    expect(component.getUserRank()).toBe(3);
  });

  it('should return -1 when userRanking is null', () => {
    component.userRanking = null;
    expect(component.getUserRank()).toBe(-1);
  });

  it('should return rank-gold style for index 0', () => {
    expect(component.getRankStyle(0)).toBe('rank-gold');
  });

  it('should return rank-silver style for index 1', () => {
    expect(component.getRankStyle(1)).toBe('rank-silver');
  });

  it('should return rank-bronze style for index 2', () => {
    expect(component.getRankStyle(2)).toBe('rank-bronze');
  });

  it('should return empty string for index > 2', () => {
    expect(component.getRankStyle(5)).toBe('');
  });

  it('should set isLoading = false on leaderboard error', () => {
    tqServiceMock.getLeaderboard.and.returnValue(throwError(() => new Error('fail')));
    component.isLoading = true;
    component.ngOnInit();
    expect(component.isLoading).toBeFalse();
  });
});
