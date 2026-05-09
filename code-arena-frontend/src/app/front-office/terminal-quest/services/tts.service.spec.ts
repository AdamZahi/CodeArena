import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TtsService } from './tts.service';
import { environment } from '../../../../environments/environment';

describe('TtsService', () => {
  let service: TtsService;
  let httpMock: HttpTestingController;

  const mockAudioContext = {
    decodeAudioData: jasmine.createSpy('decodeAudioData').and.returnValue(Promise.resolve({})),
    createBufferSource: () => ({
      connect: jasmine.createSpy('connect'),
      start: jasmine.createSpy('start'),
      stop: jasmine.createSpy('stop'),
      buffer: null,
      onended: null
    }),
    destination: {},
    state: 'running'
  };

  beforeEach(() => {
    (window as any).AudioContext = jasmine.createSpy('AudioContext').and.returnValue(mockAudioContext);
    (window as any).webkitAudioContext = jasmine.createSpy().and.returnValue(mockAudioContext);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TtsService]
    });
    service = TestBed.inject(TtsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have isMuted = false by default', () => {
    expect(service.isMuted).toBeFalse();
  });

  it('should have isPlaying = false by default', () => {
    expect(service.isPlaying).toBeFalse();
  });

  it('should toggle mute on/off', () => {
    expect(service.isMuted).toBeFalse();
    service.toggleMute();
    expect(service.isMuted).toBeTrue();
    service.toggleMute();
    expect(service.isMuted).toBeFalse();
  });

  it('should POST to TTS endpoint when speak is called', () => {
    service.speak('Hello operator', 'Charon', 'calm');
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/terminal-quest/tts`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ text: 'Hello operator', voiceName: 'Charon', style: 'calm' });
    req.flush(new ArrayBuffer(0));
  });

  it('should not send HTTP request when muted', () => {
    service.isMuted = true;
    service.speak('Test text', 'Charon');
    httpMock.expectNone(`${environment.apiBaseUrl}/api/terminal-quest/tts`);
  });

  it('should use default voice and style when not provided', () => {
    service.speak('Hello');
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/terminal-quest/tts`);
    expect(req.request.body.voiceName).toBe('Charon');
    expect(req.request.body.style).toBe('');
    req.flush(new ArrayBuffer(0));
  });

  it('should set isPlaying = false after stop()', () => {
    service.isPlaying = true;
    service.stop();
    expect(service.isPlaying).toBeFalse();
  });
});
