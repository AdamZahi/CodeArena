import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface CodeSubmission {
  studentId: string;
  language: string;
  code: string;
}

export interface AnalysisResult {
  errors_detected: boolean;
  errors: CodeError[];
  message: string;
  recommendations: string[];
}

export interface CodeError {
  line: number;
  error_type: string;
  category: string;
  severity: string;
  message: string;
  code_line: string;
}

export interface WeaknessProfile {
  student_id: string;
  overall_score: number;
  language_weaknesses: { [key: string]: number };
  error_type_frequency: { [key: string]: number };
  improvement_trend: TrendPoint[];
  top_weaknesses: TopWeakness[];
  recommendations: string[];
  total_mistakes: number;
}

export interface TrendPoint {
  date: string;
  errors: number;
}

export interface TopWeakness {
  error_type: string;
  count: number;
  category: string;
}

export interface MistakeRecord {
  id: number;
  language: string;
  error_type: string;
  error_category: string;
  code_snippet: string;
  ai_feedback: string;
  severity: string;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class AiMemoryService {
  private baseUrl = `${environment.apiBaseUrl}/api/coaching/ai-memory`;

  constructor(private http: HttpClient) {}

  analyzeCode(submission: CodeSubmission): Observable<AnalysisResult> {
    return this.http.post<any>(`${this.baseUrl}/analyze`, submission).pipe(
      map(res => res.data)
    );
  }

  getProfile(studentId: string): Observable<WeaknessProfile> {
    return this.http.get<any>(`${this.baseUrl}/profile/${studentId}`).pipe(
      map(res => res.data)
    );
  }

  getMistakes(studentId: string, limit: number = 20): Observable<MistakeRecord[]> {
    return this.http.get<any>(`${this.baseUrl}/mistakes/${studentId}?limit=${limit}`).pipe(
      map(res => res.data)
    );
  }
}
