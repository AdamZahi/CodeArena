import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AdminTqService } from './admin-tq.service';
import { environment } from '../../../../environments/environment';

describe('AdminTqService', () => {
  let service: AdminTqService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/terminal-quest`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AdminTqService]
    });
    service = TestBed.inject(AdminTqService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET all chapters', () => {
    service.getChapters().subscribe();
    httpMock.expectOne(`${base}/chapters`).flush([]);
  });

  it('should GET chapter by id', () => {
    service.getChapterById('c1').subscribe();
    httpMock.expectOne(`${base}/chapters/c1`).flush({});
  });

  it('should POST create chapter', () => {
    const payload = { title: 'New Chapter', description: 'desc', orderIndex: 1 };
    service.createChapter(payload).subscribe();
    const req = httpMock.expectOne(`${base}/chapters`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({});
  });

  it('should PUT update chapter', () => {
    service.updateChapter('c1', { title: 'Updated' }).subscribe();
    const req = httpMock.expectOne(`${base}/chapters/c1`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should DELETE chapter', () => {
    service.deleteChapter('c1').subscribe();
    const req = httpMock.expectOne(`${base}/chapters/c1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should GET levels by chapter', () => {
    service.getLevelsByChapter('c1').subscribe();
    httpMock.expectOne(`${base}/levels/chapter/c1`).flush([]);
  });

  it('should GET level by id', () => {
    service.getLevelById('l1').subscribe();
    httpMock.expectOne(`${base}/levels/l1`).flush({});
  });

  it('should POST create level', () => {
    const payload = { title: 'Level 1', chapterId: 'c1', acceptedAnswers: '["ls"]', scenario: '', orderIndex: 1, difficulty: 'EASY', isBoss: false, xpReward: 100 };
    service.createLevel(payload as any).subscribe();
    const req = httpMock.expectOne(`${base}/levels`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should PUT update level', () => {
    service.updateLevel('l1', { title: 'Updated' } as any).subscribe();
    const req = httpMock.expectOne(`${base}/levels/l1`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should DELETE level', () => {
    service.deleteLevel('l1').subscribe();
    const req = httpMock.expectOne(`${base}/levels/l1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should GET missions by chapter', () => {
    service.getMissionsByChapter('c1').subscribe();
    httpMock.expectOne(`${base}/missions/chapter/c1`).flush([]);
  });

  it('should GET mission by id', () => {
    service.getMissionById('m1').subscribe();
    httpMock.expectOne(`${base}/missions/m1`).flush({});
  });

  it('should POST create mission', () => {
    const payload = { title: 'M1', chapterId: 'c1' };
    service.createMission(payload).subscribe();
    const req = httpMock.expectOne(`${base}/missions`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should PUT update mission', () => {
    service.updateMission('m1', { title: 'Updated' }).subscribe();
    const req = httpMock.expectOne(`${base}/missions/m1`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should DELETE mission', () => {
    service.deleteMission('m1').subscribe();
    const req = httpMock.expectOne(`${base}/missions/m1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should GET global stats', () => {
    service.getGlobalStats().subscribe();
    httpMock.expectOne(`${base}/stats/global`).flush({});
  });

  it('should GET player timeline', () => {
    service.getPlayerTimeline('u1').subscribe();
    httpMock.expectOne(`${base}/activity/timeline/u1`).flush([]);
  });

  it('should GET daily activity with default 7 days', () => {
    service.getDailyActivity('u1').subscribe();
    httpMock.expectOne(`${base}/activity/daily/u1?days=7`).flush({});
  });

  it('should GET activity breakdown', () => {
    service.getActivityBreakdown('u1').subscribe();
    httpMock.expectOne(`${base}/activity/breakdown/u1`).flush({});
  });

  it('should GET leaderboard with default page and size', () => {
    service.getLeaderboard().subscribe();
    httpMock.expectOne(`${base}/advanced/leaderboard?page=0&size=10`).flush({ content: [] });
  });

  it('should GET search players', () => {
    service.searchPlayers('alice').subscribe();
    httpMock.expectOne(`${base}/advanced/players/search?query=alice`).flush([]);
  });

  it('should GET difficulty stats', () => {
    service.getDifficultyStats().subscribe();
    httpMock.expectOne(`${base}/advanced/difficulty-stats`).flush([]);
  });

  it('should GET overview', () => {
    service.getOverview().subscribe();
    httpMock.expectOne(`${base}/advanced/overview`).flush({});
  });

  it('should GET export player PDF as blob', () => {
    service.exportPlayerPdf('u1').subscribe();
    const req = httpMock.expectOne(`${base}/export/player/u1`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });

  it('should GET export global PDF as blob', () => {
    service.exportGlobalPdf().subscribe();
    const req = httpMock.expectOne(`${base}/export/global`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });
});
