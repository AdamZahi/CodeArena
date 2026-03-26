import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SubmissionService {
  private baseUrl = `${environment.apiBaseUrl}/api/submissions`;

  constructor(private http: HttpClient) {}

  submitCode(payload: { code: string, language: string, challengeId: string }): Observable<any> {
    return this.http.post<any>(this.baseUrl, payload);
  }

  getSubmissionStatus(id: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  getUserSubmissions(userId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/user/${userId}`);
  }
}
