import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { AuthService } from '@auth0/auth0-angular';
import { SkillReportComponent } from './skill-report.component';
import { SkillEngineService } from '../../services/skill-engine.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';

const MOCK_USER_SUB = 'auth0|test-user';

describe('SkillReportComponent', () => {
  let component: SkillReportComponent;
  let skillServiceMock: any;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockAnalysis = {
    skillProfile: { filesystem: 80, network: 55, process: 60, security: 30, disk: 45, service: 70 },
    overallScore: 57,
    predictedWeakness: 'security',
    weaknessConfidence: 0.9,
    certificationReadiness: {
      'RHCSA': { ready: false, overallMatch: 50, gaps: [], strengths: [] },
      'LPIC-1': { ready: false, overallMatch: 40, gaps: [], strengths: [] }
    },
    recommendations: [],
    playerTitle: 'SysAdmin',
    nextTitle: 'DevOps Engineer',
    progressToNextTitle: 40
  };

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    const voiceNavMock = {
      registerPageCommands: jasmine.createSpy(),
      unregisterPageCommands: jasmine.createSpy(),
      autoStart: jasmine.createSpy(),
      feedback$: new BehaviorSubject<string>(''),
      isListening$: new BehaviorSubject<boolean>(false),
      lastCommand$: new BehaviorSubject<string>('')
    };

    skillServiceMock = {
      analyzePlayer: jasmine.createSpy('analyzePlayer').and.returnValue(of(mockAnalysis))
    };

    const authMock = { user$: of({ sub: MOCK_USER_SUB }) };

    await TestBed.configureTestingModule({
      imports: [SkillReportComponent],
      providers: [
        { provide: AuthService, useValue: authMock },
        { provide: SkillEngineService, useValue: skillServiceMock },
        { provide: Router, useValue: routerSpy },
        { provide: VoiceNavigationService, useValue: voiceNavMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    const fixture = TestBed.createComponent(SkillReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should set userId from auth on init', () => {
    expect(component.userId).toBe(MOCK_USER_SUB);
  });

  it('should load analysis on init', () => {
    expect(skillServiceMock.analyzePlayer).toHaveBeenCalled();
    expect(component.analysis).toEqual(mockAnalysis);
  });

  it('should set isLoading = false after data loads', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('getBarColor should return red for score < 40', () => {
    expect(component.getBarColor(30)).toBe('#ef4444');
  });

  it('getBarColor should return amber for score < 70', () => {
    expect(component.getBarColor(55)).toBe('#f59e0b');
  });

  it('getBarColor should return green for score >= 70', () => {
    expect(component.getBarColor(80)).toBe('#4ade80');
  });

  it('getSkillEntries should return sorted entries by score descending', () => {
    const entries = component.getSkillEntries();
    expect(entries.length).toBe(6);
    expect(entries[0][0]).toBe('filesystem');
    expect(entries[0][1]).toBe(80);
    const scores = entries.map(e => e[1]);
    for (let i = 0; i < scores.length - 1; i++) {
      expect(scores[i]).toBeGreaterThanOrEqual(scores[i + 1]);
    }
  });

  it('getSkillEntries should return empty array when no analysis', () => {
    component.analysis = null;
    expect(component.getSkillEntries()).toEqual([]);
  });

  it('should navigate back on goBack()', () => {
    component.goBack();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/terminal-quest']);
  });
});
