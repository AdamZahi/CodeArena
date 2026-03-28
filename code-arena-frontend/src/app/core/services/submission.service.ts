import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SubmissionDto {
  id: number;
  challengeId: number;
  challengeTitle: string;
  userId: string;
  code: string;
  language: string;
  status: string;
  xpEarned: string;
  submittedAt: string | null;
  executionTime: number;
  memoryUsed: number;
  errorOutput: string;
}

@Injectable({
  providedIn: 'root'
})
export class SubmissionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/api/submissions`;

  getUserSubmissions(userId: string): Observable<SubmissionDto[]> {
    return this.http.get<SubmissionDto[]>(`${this.apiUrl}/user/${userId}`);
  }
}
