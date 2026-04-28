import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  StoryChapter,
  StoryLevel,
  StoryMission,
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
  // userId is extracted from the JWT on the backend — only answer is needed
  submitAnswer(levelId: string, answer: string): Observable<SubmitAnswerResponse> {
    return this.http.post<SubmitAnswerResponse>(`${this.base}/levels/${levelId}/submit`, { answer });
  }

  // ── Missions ──────────────────────────────────────────────────────────────
  getMissionsByChapter(chapterId: string): Observable<StoryMission[]> {
    return this.http.get<StoryMission[]>(`${this.base}/missions/chapter/${chapterId}`);
  }

  getMissionById(id: string): Observable<StoryMission> {
    return this.http.get<StoryMission>(`${this.base}/missions/${id}`);
  }

  // userId is extracted from the JWT on the backend — only answer is needed
  submitMissionAnswer(missionId: string, answer: string): Observable<SubmitAnswerResponse> {
    return this.http.post<SubmitAnswerResponse>(`${this.base}/missions/${missionId}/submit`, { answer });
  }

  // ── Progress ──────────────────────────────────────────────────────────────
  // /progress/me resolves userId from the JWT on the backend
  getProgress(): Observable<LevelProgress[]> {
    return this.http.get<LevelProgress[]>(`${this.base}/progress/me`);
  }

  getLevelProgress(levelId: string): Observable<LevelProgress> {
    return this.http.get<LevelProgress>(`${this.base}/progress/level/${levelId}`);
  }

  // ── Survival ──────────────────────────────────────────────────────────────
  // userId is extracted from the JWT on the backend
  startSurvivalSession(): Observable<SurvivalSession> {
    return this.http.post<SurvivalSession>(`${this.base}/survival/sessions`, {});
  }

  submitSurvivalAnswer(sessionId: string, levelId: string, answer: string): Observable<SurvivalAnswerResponse> {
    return this.http.post<SurvivalAnswerResponse>(`${this.base}/survival/sessions/${sessionId}/submit`, { levelId, answer });
  }

  endSurvivalSession(sessionId: string): Observable<SurvivalSession> {
    return this.http.post<SurvivalSession>(`${this.base}/survival/sessions/${sessionId}/end`, {});
  }

  getMySessions(): Observable<SurvivalSession[]> {
    return this.http.get<SurvivalSession[]>(`${this.base}/survival/sessions/me`);
  }

  getSurvivalSessionById(sessionId: string): Observable<SurvivalSession> {
    return this.http.get<SurvivalSession>(`${this.base}/survival/sessions/${sessionId}`);
  }

  // ── Leaderboard ───────────────────────────────────────────────────────────
  getLeaderboard(): Observable<SurvivalLeaderboardEntry[]> {
    return this.http.get<SurvivalLeaderboardEntry[]>(`${this.base}/survival/leaderboard`);
  }

  getMyRanking(): Observable<SurvivalLeaderboardEntry> {
    return this.http.get<SurvivalLeaderboardEntry>(`${this.base}/survival/leaderboard/me`);
  }

  getUserRanking(userId: string): Observable<SurvivalLeaderboardEntry> {
    return this.http.get<SurvivalLeaderboardEntry>(`${this.base}/survival/leaderboard/user/${userId}`);
  }

  // ── Stats ─────────────────────────────────────────────────────────────────
  getPlayerStats(userId: string): Observable<PlayerStats> {
    return this.http.get<PlayerStats>(`${this.base}/stats/${userId}`);
  }
}
