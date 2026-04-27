import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { ChapterMapComponent } from './chapter-map.component';
import { TerminalQuestService } from '../../services/terminal-quest.service';
import { TimerAudioService } from '../../services/timer-audio.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';

describe('ChapterMapComponent', () => {
  let component: ChapterMapComponent;
  let tqServiceMock: any;
  let routerSpy: jasmine.SpyObj<Router>;
  let voiceNavMock: any;

  beforeEach(async () => {
    (window as any).Audio = jasmine.createSpy('Audio').and.returnValue({
      loop: false,
      volume: 0,
      play: () => Promise.resolve(),
      pause: jasmine.createSpy('pause'),
      currentTime: 0
    });

    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    voiceNavMock = {
      registerPageCommands: jasmine.createSpy('registerPageCommands'),
      unregisterPageCommands: jasmine.createSpy('unregisterPageCommands'),
      autoStart: jasmine.createSpy('autoStart'),
      feedback$: new BehaviorSubject<string>(''),
      isListening$: new BehaviorSubject<boolean>(false),
      lastCommand$: new BehaviorSubject<string>('')
    };

    tqServiceMock = {
      getChapters: jasmine.createSpy('getChapters').and.returnValue(of([])),
      getProgress: jasmine.createSpy('getProgress').and.returnValue(of([]))
    };

    const audioMock = jasmine.createSpyObj('TimerAudioService', ['playClick', 'playHover']);

    await TestBed.configureTestingModule({
      imports: [ChapterMapComponent],
      providers: [
        { provide: TerminalQuestService, useValue: tqServiceMock },
        { provide: Router, useValue: routerSpy },
        { provide: TimerAudioService, useValue: audioMock },
        { provide: VoiceNavigationService, useValue: voiceNavMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(ChapterMapComponent, { set: { imports: [CommonModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(ChapterMapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should call getChapters on init', () => {
    expect(tqServiceMock.getChapters).toHaveBeenCalled();
  });

  it('should set isLoading = false after chapters and progress load', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('should set chapters from service response', () => {
    const mockChapters = [
      { id: 'c1', title: 'Chapter 1', missions: [], levels: [], orderIndex: 1, isLocked: false, description: '', createdAt: '2024-01-01' }
    ];
    tqServiceMock.getChapters.and.returnValue(of(mockChapters));
    component.ngOnInit();
    expect(component.chapters.length).toBe(1);
  });

  it('should set isLoading = false on error', () => {
    tqServiceMock.getChapters.and.returnValue(throwError(() => new Error('fail')));
    component.isLoading = true;
    component.ngOnInit();
    expect(component.isLoading).toBeFalse();
  });

  it('chapter 0 should always be unlocked', () => {
    component.chapters = [
      { id: 'c1', title: 'Ch1', missions: [], levels: [], orderIndex: 1, isLocked: false, description: '', createdAt: '2024-01-01' }
    ];
    expect(component.isChapterUnlocked(component.chapters[0], 0)).toBeTrue();
  });

  it('should register voice commands on init', () => {
    expect(voiceNavMock.registerPageCommands).toHaveBeenCalledWith('chapter-map', jasmine.any(Function));
  });

  it('should unregister voice commands on destroy', () => {
    component.ngOnDestroy();
    expect(voiceNavMock.unregisterPageCommands).toHaveBeenCalledWith('chapter-map');
  });
});
