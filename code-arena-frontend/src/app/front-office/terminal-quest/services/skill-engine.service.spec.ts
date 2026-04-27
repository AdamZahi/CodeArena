import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SkillEngineService } from './skill-engine.service';
import { environment } from '../../../../environments/environment';

describe('SkillEngineService', () => {
  let service: SkillEngineService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/terminal-quest/skill`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SkillEngineService]
    });
    service = TestBed.inject(SkillEngineService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET analyze player endpoint', () => {
    const mockAnalysis = {
      skillProfile: { filesystem: 75, network: 60, process: 50, security: 40, disk: 30, service: 55 },
      overallScore: 51.7,
      predictedWeakness: 'disk',
      weaknessConfidence: 0.82,
      certificationReadiness: {},
      recommendations: [],
      playerTitle: 'Junior Operator',
      nextTitle: 'SysAdmin',
      progressToNextTitle: 35.0
    };

    service.analyzePlayer('user-001').subscribe(analysis => {
      expect(analysis.overallScore).toBe(51.7);
      expect(analysis.playerTitle).toBe('Junior Operator');
      expect(analysis.predictedWeakness).toBe('disk');
      expect(analysis.skillProfile['filesystem']).toBe(75);
    });

    const req = httpMock.expectOne(`${base}/analyze/user-001`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAnalysis);
  });

  it('should GET analyze for different user', () => {
    service.analyzePlayer('player-42').subscribe();
    const req = httpMock.expectOne(`${base}/analyze/player-42`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });
});
