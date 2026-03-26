import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from '../../../core/services/api.service';
import { ApiResponse } from '../../../core/models/api-response.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ChallengeService {
  private baseUrl = `${environment.apiBaseUrl}/api/challenges`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<any> {
    return this.http.get<any>(this.baseUrl);
  }

  getById(id: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  createChallenge(challenge: any): Observable<any> {
    return this.http.post<any>(this.baseUrl, challenge);
  }

  updateChallenge(id: string, challenge: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, challenge);
  }

  deleteChallenge(id: string): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/${id}`);
  }

  // Discussion & Comments
  getComments(challengeId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${challengeId}/comments`);
  }

  addComment(challengeId: number, content: string, userName?: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${challengeId}/comments`, { content, userName });
  }

  deleteComment(commentId: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/comments/${commentId}`);
  }

  // Voting
  getVotes(challengeId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${challengeId}/votes`);
  }

  upvote(challengeId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${challengeId}/upvote`, {});
  }

  downvote(challengeId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${challengeId}/downvote`, {});
  }
}
