import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BehaviorSubject } from 'rxjs';
import { QuestHomeComponent } from './quest-home.component';
import { TimerAudioService } from '../../services/timer-audio.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';

describe('QuestHomeComponent', () => {
  let component: QuestHomeComponent;
  let routerSpy: jasmine.SpyObj<Router>;
  let voiceNavMock: any;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    voiceNavMock = {
      registerPageCommands: jasmine.createSpy('registerPageCommands'),
      unregisterPageCommands: jasmine.createSpy('unregisterPageCommands'),
      autoStart: jasmine.createSpy('autoStart'),
      feedback$: new BehaviorSubject<string>(''),
      isListening$: new BehaviorSubject<boolean>(false),
      lastCommand$: new BehaviorSubject<string>('')
    };

    const audioMock = jasmine.createSpyObj('TimerAudioService', ['playClick', 'playHover']);

    await TestBed.configureTestingModule({
      imports: [QuestHomeComponent],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: TimerAudioService, useValue: audioMock },
        { provide: VoiceNavigationService, useValue: voiceNavMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(QuestHomeComponent, { set: { imports: [CommonModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(QuestHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should register voice commands on init', () => {
    expect(voiceNavMock.registerPageCommands).toHaveBeenCalledWith('quest-home', jasmine.any(Function));
  });

  it('should call autoStart on init', () => {
    expect(voiceNavMock.autoStart).toHaveBeenCalled();
  });

  it('should navigate to story on voice command "story"', () => {
    const handler = voiceNavMock.registerPageCommands.calls.mostRecent().args[1];
    const result = handler('story mode');
    expect(result).toBeTrue();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/terminal-quest/story']);
  });

  it('should navigate to survival on voice command "survival"', () => {
    const handler = voiceNavMock.registerPageCommands.calls.mostRecent().args[1];
    const result = handler('survival');
    expect(result).toBeTrue();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/terminal-quest/survival']);
  });

  it('should navigate to skill-report on voice command "skill"', () => {
    const handler = voiceNavMock.registerPageCommands.calls.mostRecent().args[1];
    const result = handler('skill report');
    expect(result).toBeTrue();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/terminal-quest/skill-report']);
  });

  it('should navigate to leaderboard on voice command "leaderboard"', () => {
    const handler = voiceNavMock.registerPageCommands.calls.mostRecent().args[1];
    const result = handler('leaderboard');
    expect(result).toBeTrue();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/terminal-quest/survival/leaderboard']);
  });

  it('should return false for unknown voice command', () => {
    const handler = voiceNavMock.registerPageCommands.calls.mostRecent().args[1];
    const result = handler('unknown command');
    expect(result).toBeFalse();
  });

  it('should unregister page commands on destroy', () => {
    component.ngOnDestroy();
    expect(voiceNavMock.unregisterPageCommands).toHaveBeenCalledWith('quest-home');
  });

  it('should navigate to skill-report via goToSkillReport()', () => {
    component.goToSkillReport();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/terminal-quest/skill-report']);
  });
});
