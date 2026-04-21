import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { EcoScoreService, EcoScore } from './eco-score.service';

describe('EcoScoreService', () => {
  let service: EcoScoreService;
  let httpMock: HttpTestingController;

  const AI_URL = 'http://localhost:5000';

  const mockEcoScore: EcoScore = {
    score: 7,
    label: 'Good',
    reason: 'Durable product with moderate environmental footprint.',
    color: '#22c55e',
    emoji: '🌿'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EcoScoreService]
    });
    service = TestBed.inject(EcoScoreService);
    httpMock = TestBed.inject(HttpTestingController);
    // Clear internal cache between tests
    (service as any)['cache'].clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── getScore ──────────────────────────────────

  it('getScore — calls Flask API and returns eco score', () => {
    service.getScore('prod-123', 'CodeArena Hoodie', 'HOODIE').subscribe(score => {
      expect(score.score).toBe(7);
      expect(score.label).toBe('Good');
      expect(score.emoji).toBe('🌿');
    });

    const req = httpMock.expectOne(`${AI_URL}/api/eco-score`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      productId: 'prod-123',
      productName: 'CodeArena Hoodie',
      category: 'HOODIE'
    });
    req.flush(mockEcoScore);
  });

  it('getScore — returns cached result on second call (no second HTTP request)', () => {
    // First call — hits API
    service.getScore('prod-cached', 'CodeArena Mousepad', 'MOUSEPAD').subscribe();
    const req = httpMock.expectOne(`${AI_URL}/api/eco-score`);
    req.flush(mockEcoScore);

    // Second call — uses cache, no HTTP
    service.getScore('prod-cached', 'CodeArena Mousepad', 'MOUSEPAD').subscribe(score => {
      expect(score).toEqual(mockEcoScore);
    });
    httpMock.expectNone(`${AI_URL}/api/eco-score`);
  });

  it('getScore — returns fallback score when Flask is down', () => {
    service.getScore('prod-err', 'Test Product', 'OTHER').subscribe(score => {
      expect(score.score).toBe(5);
      expect(score.label).toBe('Average');
      expect(score.emoji).toBe('♻️');
    });

    const req = httpMock.expectOne(`${AI_URL}/api/eco-score`);
    req.error(new ErrorEvent('Network error'));
  });

  // ── loadAllScores ─────────────────────────────

  it('loadAllScores — calls batch endpoint with all products', () => {
    const products = [
      { id: 'p1', name: 'CodeArena Hoodie', category: 'HOODIE' },
      { id: 'p2', name: 'CodeArena Mousepad', category: 'MOUSEPAD' }
    ];

    const mockBatchResponse = {
      success: true,
      count: 2,
      scores: {
        'p1': { score: 6, label: 'Average', reason: 'Standard.', color: '#f59e0b', emoji: '♻️' },
        'p2': { score: 8, label: 'Good', reason: 'Durable.', color: '#22c55e', emoji: '🌿' }
      }
    };

    service.loadAllScores(products).subscribe(scores => {
      expect(Object.keys(scores).length).toBe(2);
      expect(scores['p1'].score).toBe(6);
      expect(scores['p2'].score).toBe(8);
      expect(scores['p2'].emoji).toBe('🌿');
    });

    const req = httpMock.expectOne(`${AI_URL}/api/eco-score/batch`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.products.length).toBe(2);
    req.flush(mockBatchResponse);

    // ── Handle saveEcoScore calls to Spring Boot ──
    // After Flask scores, Angular saves each score to DB
    // We must flush these requests so httpMock.verify() doesn't fail
    httpMock.match('http://localhost:8080/api/shop/products/p1/eco-score')
            .forEach(r => r.flush({}));
    httpMock.match('http://localhost:8080/api/shop/products/p2/eco-score')
            .forEach(r => r.flush({}));
  });

  it('loadAllScores — returns fallback scores when Flask is down', () => {
    const products = [{ id: 'p1', name: 'CodeArena Hoodie', category: 'HOODIE' }];

    service.loadAllScores(products).subscribe(scores => {
      expect(scores['p1'].score).toBe(5);
      expect(scores['p1'].label).toBe('Average');
    });

    const req = httpMock.expectOne(`${AI_URL}/api/eco-score/batch`);
    req.error(new ErrorEvent('Network error'));
    // No saveEcoScore calls when Flask is down — catchError returns fallback
  });

  it('loadAllScores — skips HTTP call if all products already cached', () => {
    const products = [{ id: 'cached-p1', name: 'Hoodie', category: 'HOODIE' }];

    // First load — hits API
    service.loadAllScores(products).subscribe();
    const req = httpMock.expectOne(`${AI_URL}/api/eco-score/batch`);
    req.flush({ success: true, count: 1, scores: { 'cached-p1': mockEcoScore } });

    // ── Handle saveEcoScore call on first load ──
    httpMock.match('http://localhost:8080/api/shop/products/cached-p1/eco-score')
            .forEach(r => r.flush({}));

    // Second load — uses cache, no HTTP to Flask or backend
    service.loadAllScores(products).subscribe(scores => {
      expect(scores['cached-p1']).toEqual(mockEcoScore);
    });
    httpMock.expectNone(`${AI_URL}/api/eco-score/batch`);
  });
});