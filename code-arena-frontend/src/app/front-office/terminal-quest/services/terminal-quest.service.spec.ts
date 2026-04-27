import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TerminalQuestService } from './terminal-quest.service';
import { environment } from '../../../../environments/environment';

describe('TerminalQuestService', () => {
  let service: TerminalQuestService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/terminal-quest`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TerminalQuestService]
    });
    service = TestBed.inject(TerminalQuestService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET chapters', () => {
    const mockChapters = [{ id: 'ch1', title: 'Chapter 1', orderIndex: 1 }];
    service.getChapters().subscribe(chapters => {
      expect(chapters.length).toBe(1);
      expect(chapters[0].title).toBe('Chapter 1');
    });
    const req = httpMock.expectOne(`${base}/chapters`);
    expect(req.request.method).toBe('GET');
    req.flush(mockChapters);
  });

  it('should GET chapter by id', () => {
    const mockChapter = { id: 'ch1', title: 'Chapter 1', orderIndex: 1 };
    service.getChapterById('ch1').subscribe(ch => {
      expect(ch.id).toBe('ch1');
    });
    const req = httpMock.expectOne(`${base}/chapters/ch1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockChapter);
  });

  it('should GET levels by chapter', () => {
    const mockLevels = [{ id: 'lv1', title: 'Level 1', chapterId: 'ch1' }];
    service.getLevelsByChapter('ch1').subscribe(levels => {
      expect(levels.length).toBe(1);
    });
    const req = httpMock.expectOne(`${base}/levels/chapter/ch1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockLevels);
  });

  it('should GET level by id', () => {
    const mockLevel = { id: 'lv1', title: 'Level 1' };
    service.getLevelById('lv1').subscribe(lv => {
      expect(lv.id).toBe('lv1');
    });
    const req = httpMock.expectOne(`${base}/levels/lv1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockLevel);
  });

  it('should POST submit answer for level', () => {
    const mockResponse = { correct: true, starsEarned: 3, xpEarned: 50, attempts: 1 };
    service.submitAnswer('lv1', 'user-001', 'ls -la').subscribe(res => {
      expect(res.correct).toBeTrue();
      expect(res.starsEarned).toBe(3);
    });
    const req = httpMock.expectOne(`${base}/levels/lv1/submit`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'user-001', answer: 'ls -la' });
    req.flush(mockResponse);
  });

  it('should GET missions by chapter', () => {
    const mockMissions = [{ id: 'm1', title: 'Mission 1', chapterId: 'ch1' }];
    service.getMissionsByChapter('ch1').subscribe(missions => {
      expect(missions.length).toBe(1);
    });
    const req = httpMock.expectOne(`${base}/missions/chapter/ch1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockMissions);
  });

  it('should GET mission by id', () => {
    const mockMission = { id: 'm1', title: 'Mission 1' };
    service.getMissionById('m1').subscribe(m => {
      expect(m.id).toBe('m1');
    });
    const req = httpMock.expectOne(`${base}/missions/m1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockMission);
  });

  it('should POST submit mission answer', () => {
    const mockResponse = { correct: false, starsEarned: 0, xpEarned: 0, attempts: 2 };
    service.submitMissionAnswer('m1', 'user-001', 'bad cmd').subscribe(res => {
      expect(res.correct).toBeFalse();
    });
    const req = httpMock.expectOne(`${base}/missions/m1/submit`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'user-001', answer: 'bad cmd' });
    req.flush(mockResponse);
  });

  it('should GET progress by user', () => {
    const mockProgress = [{ id: 'p1', userId: 'user-001', completed: true }];
    service.getProgress('user-001').subscribe(progress => {
      expect(progress.length).toBe(1);
    });
    const req = httpMock.expectOne(`${base}/progress/user-001`);
    expect(req.request.method).toBe('GET');
    req.flush(mockProgress);
  });

  it('should POST start survival session', () => {
    const mockSession = { id: 'sess1', userId: 'user-001', waveReached: 1, score: 0, livesRemaining: 3 };
    service.startSurvivalSession('user-001').subscribe(sess => {
      expect(sess.id).toBe('sess1');
      expect(sess.livesRemaining).toBe(3);
    });
    const req = httpMock.expectOne(`${base}/survival/sessions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'user-001' });
    req.flush(mockSession);
  });

  it('should POST submit survival answer', () => {
    const mockResponse = { correct: true, livesRemaining: 3, waveReached: 1, score: 10, gameOver: false };
    service.submitSurvivalAnswer('sess1', 'user-001', 'lv1', 'ls').subscribe(res => {
      expect(res.correct).toBeTrue();
      expect(res.score).toBe(10);
    });
    const req = httpMock.expectOne(`${base}/survival/sessions/sess1/submit`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'user-001', levelId: 'lv1', answer: 'ls' });
    req.flush(mockResponse);
  });

  it('should POST end survival session', () => {
    const mockSession = { id: 'sess1', waveReached: 3, score: 60 };
    service.endSurvivalSession('sess1').subscribe(sess => {
      expect(sess.waveReached).toBe(3);
    });
    const req = httpMock.expectOne(`${base}/survival/sessions/sess1/end`);
    expect(req.request.method).toBe('POST');
    req.flush(mockSession);
  });

  it('should GET leaderboard', () => {
    const mockLb = [{ id: 'lb1', userId: 'user-001', bestWave: 5, bestScore: 200 }];
    service.getLeaderboard().subscribe(lb => {
      expect(lb.length).toBe(1);
      expect(lb[0].bestScore).toBe(200);
    });
    const req = httpMock.expectOne(`${base}/survival/leaderboard`);
    expect(req.request.method).toBe('GET');
    req.flush(mockLb);
  });

  it('should GET player stats', () => {
    const mockStats = { userId: 'user-001', totalLevelsCompleted: 5, totalStarsEarned: 12 };
    service.getPlayerStats('user-001').subscribe(stats => {
      expect(stats.totalLevelsCompleted).toBe(5);
    });
    const req = httpMock.expectOne(`${base}/stats/user-001`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStats);
  });
});
