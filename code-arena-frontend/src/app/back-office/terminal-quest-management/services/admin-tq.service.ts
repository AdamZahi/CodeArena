import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  StoryChapter,
  StoryLevel,
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

  // ── Stats ─────────────────────────────────────────────────────────────────
  getGlobalStats(): Observable<GlobalStats> {
    return this.http.get<GlobalStats>(`${this.base}/stats/global`);
  }
}
