import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { SurvivalPlayComponent } from './survival-play.component';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { TimerAudioService } from '../../services/timer-audio.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';

describe('SurvivalPlayComponent', () => {
  let component: SurvivalPlayComponent;
  let tqServiceMock: any;

  beforeEach(async () => {
    const voiceNavMock = {
      registerPageCommands: jasmine.createSpy(),
      unregisterPageCommands: jasmine.createSpy(),
      autoStart: jasmine.createSpy(),
      feedback$: new BehaviorSubject<string>(''),
      isListening$: new BehaviorSubject<boolean>(false),
      lastCommand$: new BehaviorSubject<string>('')
    };

    tqServiceMock = {
      startSurvivalSession: jasmine.createSpy('startSurvivalSession').and.returnValue(of({
        id: 'sess-1', userId: 'test-user-001',
        livesRemaining: 3, waveReached: 1, score: 0, active: true
      })),
      getChapters: jasmine.createSpy('getChapters').and.returnValue(of([]))
    };

    const audioMock = jasmine.createSpyObj('TimerAudioService', [
      'getTimeForDifficulty', 'playTick', 'playUrgentTick',
      'playSuccessSound', 'playErrorSound', 'playBaseTick', 'playGameOverSound'
    ]);
    audioMock.getTimeForDifficulty.and.returnValue(30);

    await TestBed.configureTestingModule({
      imports: [SurvivalPlayComponent],
      providers: [
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: TerminalQuestService, useValue: tqServiceMock },
        { provide: TimerAudioService, useValue: audioMock },
        { provide: VoiceNavigationService, useValue: voiceNavMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    const fixture = TestBed.createComponent(SurvivalPlayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should have lives = 3 after session starts', () => {
    expect(component.lives).toBe(3);
  });

  it('should have wave = 1 initially', () => {
    expect(component.wave).toBe(1);
  });

  it('should have score = 0 initially', () => {
    expect(component.score).toBe(0);
  });

  it('should have gameOver = false initially', () => {
    expect(component.gameOver).toBeFalse();
  });

  it('should have userId = "test-user-001"', () => {
    expect(component.userId).toBe('test-user-001');
  });

  it('getMinutes should return padded minutes', () => {
    component.timeRemaining = 70;
    expect(component.getMinutes()).toBe('01');
  });

  it('getSeconds should return padded seconds', () => {
    component.timeRemaining = 65;
    expect(component.getSeconds()).toBe('05');
  });

  it('should not throw when abandon is called without session', () => {
    component.session = null;
    expect(() => component.abandon()).not.toThrow();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });
});
