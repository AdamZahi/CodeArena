import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { LevelPlayComponent } from './level-play.component';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { TimerAudioService } from '../../services/timer-audio.service';
import { TtsService } from '../../services/tts.service';
import { MissionVoiceService } from '../../services/mission-voice.service';
import { CommandExplainerService } from '../../services/command-explainer.service';
import { AdaptiveLearningService } from '../../services/adaptive-learning.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';

const mockAudioContext = {
  decodeAudioData: jasmine.createSpy('decodeAudioData').and.returnValue(Promise.resolve({})),
  createBufferSource: () => ({
    connect: jasmine.createSpy(), start: jasmine.createSpy(),
    stop: jasmine.createSpy(), buffer: null, onended: null
  }),
  destination: {},
  state: 'running'
};

describe('LevelPlayComponent', () => {
  let component: LevelPlayComponent;

  beforeEach(async () => {
    (window as any).AudioContext = jasmine.createSpy('AudioContext').and.returnValue(mockAudioContext);
    (window as any).webkitAudioContext = jasmine.createSpy().and.returnValue(mockAudioContext);

    const voiceNavMock = {
      registerPageCommands: jasmine.createSpy(),
      unregisterPageCommands: jasmine.createSpy(),
      autoStart: jasmine.createSpy(),
      pause: jasmine.createSpy(),
      resume: jasmine.createSpy(),
      feedback$: new BehaviorSubject<string>(''),
      isListening$: new BehaviorSubject<boolean>(false),
      lastCommand$: new BehaviorSubject<string>('')
    };

    const adaptiveMock = {
      predictAdaptation: jasmine.createSpy('predictAdaptation').and.returnValue(of({
        successProbability: 0.8, recommendedAction: 'CHALLENGE',
        timerAdjustment: 0, showHint: false,
        difficultyLabel: 'EASY', playerLevel: 'PROFICIENT'
      }))
    };

    const tqMock = {
      getMissionById: jasmine.createSpy('getMissionById').and.returnValue(of(null))
    };

    await TestBed.configureTestingModule({
      imports: [LevelPlayComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'mock-id' } } } },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: TerminalQuestService, useValue: tqMock },
        { provide: TimerAudioService, useValue: jasmine.createSpyObj('TimerAudioService', ['getTimeForDifficulty', 'playTick', 'playUrgentTick', 'playSuccessSound', 'playErrorSound', 'playBaseTick', 'playGameOverSound']) },
        { provide: TtsService, useValue: jasmine.createSpyObj('TtsService', ['speak', 'stop']) },
        { provide: MissionVoiceService, useValue: jasmine.createSpyObj('MissionVoiceService', ['playMissionIntro', 'playCorrectAnswer', 'playWrongAnswer']) },
        { provide: CommandExplainerService, useValue: jasmine.createSpyObj('CommandExplainerService', ['explain']) },
        { provide: AdaptiveLearningService, useValue: adaptiveMock },
        { provide: VoiceNavigationService, useValue: voiceNavMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    const fixture = TestBed.createComponent(LevelPlayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should have isCorrect = null by default', () => {
    expect(component.isCorrect).toBeNull();
  });

  it('should have showHint = false by default', () => {
    expect(component.showHint).toBeFalse();
  });

  it('should toggle hint', () => {
    component.showHint = false;
    component.toggleHint();
    expect(component.showHint).toBeTrue();
    component.toggleHint();
    expect(component.showHint).toBeFalse();
  });

  it('should have userId = "test-user-001"', () => {
    expect(component.userId).toBe('test-user-001');
  });

  it('getMinutes should return padded minutes string', () => {
    component.timeRemaining = 90;
    expect(component.getMinutes()).toBe('01');
  });

  it('getSeconds should return padded seconds string', () => {
    component.timeRemaining = 65;
    expect(component.getSeconds()).toBe('05');
  });

  it('getStarDisplay should return stars array', () => {
    expect(component.getStarDisplay(2)).toEqual(['★', '★', '☆']);
  });

  afterEach(() => {
    component.ngOnDestroy();
  });
});
