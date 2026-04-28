import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface AiRequest {
  mode: 'SESSION_PLAN' | 'QUIZ_GENERATE' | 'CHAT' | 'CODE_EXPLAIN' | 'CODE_REVIEW' | 'PRACTICE_EXERCISE' | 'DEBUG_HELP';
  topic?: string;
  language?: string;
  level?: string;
  durationMinutes?: number;
  questionCount?: number;
  message?: string;
  context?: string;
}

export interface AiGeneratedContent {
  content: string;
  mode: string;
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private baseUrl = `${environment.apiBaseUrl}/api/coaching/ai`;

  constructor(private http: HttpClient) {}

  generate(request: AiRequest): Observable<AiGeneratedContent> {
    return this.http.post<any>(`${this.baseUrl}/generate`, request).pipe(
      map(res => res.data)
    );
  }
}
