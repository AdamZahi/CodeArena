import { TestBed } from '@angular/core/testing';
import { TimerAudioService } from './timer-audio.service';

describe('TimerAudioService', () => {
  let service: TimerAudioService;

  const mockAudioContext = {
    createOscillator: () => ({
      connect: jasmine.createSpy('connect'),
      start: jasmine.createSpy('start'),
      stop: jasmine.createSpy('stop'),
      type: 'sine',
      frequency: {
        value: 0,
        setValueAtTime: jasmine.createSpy('setValueAtTime'),
        linearRampToValueAtTime: jasmine.createSpy('linearRampToValueAtTime'),
        exponentialRampToValueAtTime: jasmine.createSpy('exponentialRampToValueAtTime')
      }
    }),
    createGain: () => ({
      connect: jasmine.createSpy('connect'),
      gain: {
        setValueAtTime: jasmine.createSpy('setValueAtTime'),
        exponentialRampToValueAtTime: jasmine.createSpy('exponentialRampToValueAtTime'),
        linearRampToValueAtTime: jasmine.createSpy('linearRampToValueAtTime')
      }
    }),
    createBiquadFilter: () => ({
      connect: jasmine.createSpy('connect'),
      type: 'bandpass',
      frequency: { setValueAtTime: jasmine.createSpy(), linearRampToValueAtTime: jasmine.createSpy() },
      Q: { value: 0 }
    }),
    createBuffer: (_c: number, size: number, rate: number) => ({
      getChannelData: () => new Float32Array(size)
    }),
    createBufferSource: () => ({
      connect: jasmine.createSpy('connect'),
      start: jasmine.createSpy('start'),
      stop: jasmine.createSpy('stop'),
      buffer: null
    }),
    destination: {},
    currentTime: 0,
    sampleRate: 44100,
    state: 'running'
  };

  beforeEach(() => {
    (window as any).AudioContext = jasmine.createSpy('AudioContext').and.returnValue(mockAudioContext);
    (window as any).webkitAudioContext = jasmine.createSpy('webkitAudioContext').and.returnValue(mockAudioContext);

    TestBed.configureTestingModule({
      providers: [TimerAudioService]
    });
    service = TestBed.inject(TimerAudioService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getTimeForDifficulty', () => {
    it('should return 60 for EASY', () => {
      expect(service.getTimeForDifficulty('EASY', false)).toBe(60);
    });

    it('should return 45 for MEDIUM', () => {
      expect(service.getTimeForDifficulty('MEDIUM', false)).toBe(45);
    });

    it('should return 30 for HARD', () => {
      expect(service.getTimeForDifficulty('HARD', false)).toBe(30);
    });

    it('should return 90 for BOSS missions regardless of difficulty', () => {
      expect(service.getTimeForDifficulty('EASY', true)).toBe(90);
      expect(service.getTimeForDifficulty('HARD', true)).toBe(90);
    });

    it('should return 60 for unknown difficulty', () => {
      expect(service.getTimeForDifficulty('UNKNOWN', false)).toBe(60);
    });
  });

  it('should not throw when playTick is called', () => {
    expect(() => service.playTick()).not.toThrow();
  });

  it('should not throw when playUrgentTick is called', () => {
    expect(() => service.playUrgentTick()).not.toThrow();
  });

  it('should not throw when playSuccessSound is called', () => {
    expect(() => service.playSuccessSound()).not.toThrow();
  });

  it('should not throw when playErrorSound is called', () => {
    expect(() => service.playErrorSound()).not.toThrow();
  });

  it('should not throw when playGameOverSound is called', () => {
    expect(() => service.playGameOverSound()).not.toThrow();
  });

  it('should not throw when playBaseTick is called', () => {
    expect(() => service.playBaseTick()).not.toThrow();
  });

  it('should not throw when playClick is called', () => {
    expect(() => service.playClick()).not.toThrow();
  });

  it('should not throw when playHover is called', () => {
    expect(() => service.playHover()).not.toThrow();
  });

  it('should not throw when playMissionEntry is called', () => {
    expect(() => service.playMissionEntry()).not.toThrow();
  });
});
