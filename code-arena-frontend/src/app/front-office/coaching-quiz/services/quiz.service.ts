import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Quiz, QuizResult, SubmitQuizRequest } from '../models/quiz.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class QuizService extends ApiService {
  constructor(http: HttpClient) {
    super(http, `${environment.apiBaseUrl}/api/quizzes`);
  }

  getAllQuizzes(): Observable<Quiz[]> {
    return this.http.get<any>(`${this.baseUrl}`).pipe(map(res => res.data));
  }

  getQuizById(id: string): Observable<Quiz> {
    return this.http.get<any>(`${this.baseUrl}`, { params: { id } }).pipe(map(res => res.data));
  }

  submitQuiz(request: SubmitQuizRequest): Observable<QuizResult> {
    return this.http.post<any>(`${this.baseUrl}/submit`, request).pipe(map(res => res.data));
  }

  getHistory(): Observable<QuizResult[]> {
    return this.http.get<any>(`${this.baseUrl}/history`).pipe(map(res => res.data));
  }
}
