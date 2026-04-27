import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  StoryChapter,
  StoryLevel,
  StoryMission,
  GlobalStats,
} from '../../../front-office/terminal-quest/models/terminal-quest.model';

@Injectable({ providedIn: 'root' })
export class AdminTqService {
  private readonly base = `${environment.apiBaseUrl}/api/terminal-quest`;

  constructor(private http: HttpClient) {}

  // ── Chapters ──────────────────────────────────────────────────────────────
  getChapters(): Observable<StoryChapter[]> {
    return this.http.get<StoryChapter[]>(`${this.base}/chapters`);
  }

  getChapterById(id: string): Observable<StoryChapter> {
    return this.http.get<StoryChapter>(`${this.base}/chapters/${id}`);
  }

  createChapter(payload: Partial<StoryChapter>): Observable<StoryChapter> {
    return this.http.post<StoryChapter>(`${this.base}/chapters`, payload);
  }

  updateChapter(id: string, payload: Partial<StoryChapter>): Observable<StoryChapter> {
    return this.http.put<StoryChapter>(`${this.base}/chapters/${id}`, payload);
  }

  deleteChapter(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/chapters/${id}`);
  }

  // ── Levels ────────────────────────────────────────────────────────────────
  getLevelsByChapter(chapterId: string): Observable<StoryLevel[]> {
    return this.http.get<StoryLevel[]>(`${this.base}/levels/chapter/${chapterId}`);
  }

  getLevelById(id: string): Observable<StoryLevel> {
    return this.http.get<StoryLevel>(`${this.base}/levels/${id}`);
  }

  createLevel(payload: Partial<StoryLevel> & { chapterId: string; acceptedAnswers: string }): Observable<StoryLevel> {
    return this.http.post<StoryLevel>(`${this.base}/levels`, payload);
  }

  updateLevel(id: string, payload: Partial<StoryLevel> & { chapterId?: string; acceptedAnswers?: string }): Observable<StoryLevel> {
    return this.http.put<StoryLevel>(`${this.base}/levels/${id}`, payload);
  }

  deleteLevel(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/levels/${id}`);
  }

  // ── Missions ─────────────────────────────────────────────────────────────
  getMissionsByChapter(chapterId: string): Observable<StoryMission[]> {
    return this.http.get<StoryMission[]>(`${this.base}/missions/chapter/${chapterId}`);
  }

  getMissionById(id: string): Observable<StoryMission> {
    return this.http.get<StoryMission>(`${this.base}/missions/${id}`);
  }

  createMission(payload: any): Observable<StoryMission> {
    return this.http.post<StoryMission>(`${this.base}/missions`, payload);
  }

  updateMission(id: string, payload: any): Observable<StoryMission> {
    return this.http.put<StoryMission>(`${this.base}/missions/${id}`, payload);
  }

  deleteMission(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/missions/${id}`);
  }

  // ── Stats ─────────────────────────────────────────────────────────────────
  getGlobalStats(): Observable<GlobalStats> {
    return this.http.get<GlobalStats>(`${this.base}/stats/global`);
  }

  // ── Activity Log ──────────────────────────────────────────────────────────
  getPlayerTimeline(userId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/activity/timeline/${userId}`);
  }

  getDailyActivity(userId: string, days = 7): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.base}/activity/daily/${userId}?days=${days}`);
  }

  getActivityBreakdown(userId: string): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.base}/activity/breakdown/${userId}`);
  }

  // ── Advanced Stats ────────────────────────────────────────────────────────
  getLeaderboard(page = 0, size = 10): Observable<any> {
    return this.http.get<any>(`${this.base}/advanced/leaderboard?page=${page}&size=${size}`);
  }

  searchPlayers(query: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/advanced/players/search?query=${encodeURIComponent(query)}`);
  }

  getDifficultyStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/advanced/difficulty-stats`);
  }

  getOverview(): Observable<any> {
    return this.http.get<any>(`${this.base}/advanced/overview`);
  }

  // ── PDF Export ────────────────────────────────────────────────────────────
  exportPlayerPdf(userId: string): Observable<Blob> {
    return this.http.get(`${this.base}/export/player/${userId}`, { responseType: 'blob' });
  }

  exportGlobalPdf(): Observable<Blob> {
    return this.http.get(`${this.base}/export/global`, { responseType: 'blob' });
  }
}
