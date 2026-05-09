import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AdaptiveLearningService } from './adaptive-learning.service';
import { environment } from '../../../../environments/environment';

describe('AdaptiveLearningService', () => {
  let service: AdaptiveLearningService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/terminal-quest/adaptive`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AdaptiveLearningService]
    });
    service = TestBed.inject(AdaptiveLearningService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST to predict endpoint with userId and missionId', () => {
    const mockPrediction = {
      successProbability: 0.85,
      recommendedAction: 'CHALLENGE',
      timerAdjustment: -10,
      showHint: false,
      difficultyLabel: 'HARD',
      playerLevel: 'PROFICIENT'
    };

    service.predictAdaptation('user-001', 'mission-123').subscribe(result => {
      expect(result.successProbability).toBe(0.85);
      expect(result.recommendedAction).toBe('CHALLENGE');
      expect(result.showHint).toBeFalse();
    });

    const req = httpMock.expectOne(`${base}/predict`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'user-001', missionId: 'mission-123' });
    req.flush(mockPrediction);
  });

  it('should POST with ASSIST recommendation for struggling player', () => {
    const mockPrediction = {
      successProbability: 0.2,
      recommendedAction: 'ASSIST',
      timerAdjustment: 15,
      showHint: true,
      difficultyLabel: 'EASY',
      playerLevel: 'STRUGGLING'
    };

    service.predictAdaptation('new-user', 'mission-456').subscribe(result => {
      expect(result.recommendedAction).toBe('ASSIST');
      expect(result.showHint).toBeTrue();
      expect(result.timerAdjustment).toBe(15);
    });

    const req = httpMock.expectOne(`${base}/predict`);
    req.flush(mockPrediction);
  });
});
