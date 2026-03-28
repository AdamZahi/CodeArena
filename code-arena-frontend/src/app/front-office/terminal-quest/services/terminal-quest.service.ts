import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  StoryChapter,
  StoryLevel,
  LevelProgress,
  SubmitAnswerResponse,
  SurvivalSession,
  SurvivalAnswerResponse,
  SurvivalLeaderboardEntry,
  PlayerStats,
} from '../models/terminal-quest.model';

@Injectable({ providedIn: 'root' })
export class TerminalQuestService {
  private readonly base = `${environment.apiBaseUrl}/api/terminal-quest`;

  constructor(private http: HttpClient) {}

  // ── Chapters ──────────────────────────────────────────────────────────────
  getChapters(): Observable<StoryChapter[]> {
    return this.http.get<StoryChapter[]>(`${this.base}/chapters`);
  }

  getChapterById(id: string): Observable<StoryChapter> {
    return this.http.get<StoryChapter>(`${this.base}/chapters/${id}`);
  }

  // ── Levels ────────────────────────────────────────────────────────────────
  getLevelsByChapter(chapterId: string): Observable<StoryLevel[]> {
    return this.http.get<StoryLevel[]>(`${this.base}/levels/chapter/${chapterId}`);
  }

  getLevelById(levelId: string): Observable<StoryLevel> {
    return this.http.get<StoryLevel>(`${this.base}/levels/${levelId}`);
  }

  // ── Story answer submission ───────────────────────────────────────────────
  submitAnswer(levelId: string, userId: string, answer: string): Observable<SubmitAnswerResponse> {
    return this.http.post<SubmitAnswerResponse>(`${this.base}/levels/${levelId}/submit`, { userId, answer });
  }

  // ── Progress ──────────────────────────────────────────────────────────────
  getProgress(userId: string): Observable<LevelProgress[]> {
    return this.http.get<LevelProgress[]>(`${this.base}/progress/${userId}`);
  }

  getLevelProgress(userId: string, levelId: string): Observable<LevelProgress> {
    return this.http.get<LevelProgress>(`${this.base}/progress/${userId}/level/${levelId}`);
  }

  // ── Survival ──────────────────────────────────────────────────────────────
  startSurvivalSession(userId: string): Observable<SurvivalSession> {
    return this.http.post<SurvivalSession>(`${this.base}/survival/sessions`, { userId });
  }

  submitSurvivalAnswer(sessionId: string, userId: string, levelId: string, answer: string): Observable<SurvivalAnswerResponse> {
    return this.http.post<SurvivalAnswerResponse>(`${this.base}/survival/sessions/${sessionId}/submit`, { userId, levelId, answer });
  }

  endSurvivalSession(sessionId: string): Observable<SurvivalSession> {
    return this.http.post<SurvivalSession>(`${this.base}/survival/sessions/${sessionId}/end`, {});
  }

  getSurvivalSessionsByUser(userId: string): Observable<SurvivalSession[]> {
    return this.http.get<SurvivalSession[]>(`${this.base}/survival/sessions/user/${userId}`);
  }

  getSurvivalSessionById(sessionId: string): Observable<SurvivalSession> {
    return this.http.get<SurvivalSession>(`${this.base}/survival/sessions/${sessionId}`);
  }

  // ── Leaderboard ───────────────────────────────────────────────────────────
  getLeaderboard(): Observable<SurvivalLeaderboardEntry[]> {
    return this.http.get<SurvivalLeaderboardEntry[]>(`${this.base}/survival/leaderboard`);
  }

  getUserRanking(userId: string): Observable<SurvivalLeaderboardEntry> {
    return this.http.get<SurvivalLeaderboardEntry>(`${this.base}/survival/leaderboard/user/${userId}`);
  }

  // ── Stats ─────────────────────────────────────────────────────────────────
  getPlayerStats(userId: string): Observable<PlayerStats> {
    return this.http.get<PlayerStats>(`${this.base}/stats/${userId}`);
  }
}
