import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CommandExplainerService } from './command-explainer.service';
import { environment } from '../../../../environments/environment';

describe('CommandExplainerService', () => {
  let service: CommandExplainerService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/terminal-quest/explain`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CommandExplainerService]
    });
    service = TestBed.inject(CommandExplainerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST explain for correct command', () => {
    const mockResponse = { explanation: 'The ls -la command lists all files with details.' };

    service.explain('ls -la', 'List files', 'Linux basics', 'EASY', true).subscribe(res => {
      expect(res.explanation).toContain('ls -la');
    });

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      command: 'ls -la',
      missionTask: 'List files',
      missionContext: 'Linux basics',
      difficulty: 'EASY',
      isCorrect: true
    });
    req.flush(mockResponse);
  });

  it('should POST explain for incorrect command', () => {
    const mockResponse = { explanation: 'That command is incorrect. Try ls to list files.' };

    service.explain('dir', 'List files', 'Linux basics', 'EASY', false).subscribe(res => {
      expect(res.explanation).toBeDefined();
    });

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.isCorrect).toBeFalse();
    req.flush(mockResponse);
  });

  it('should POST explain for HARD difficulty', () => {
    service.explain('systemctl restart nginx', 'Restart service', 'Service management', 'HARD', true).subscribe();

    const req = httpMock.expectOne(base);
    expect(req.request.body.difficulty).toBe('HARD');
    expect(req.request.body.command).toBe('systemctl restart nginx');
    req.flush({ explanation: 'systemctl restarts a systemd service.' });
  });
});
