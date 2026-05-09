import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  Coach,
  CoachingSession,
  CoachingNotification,
  Dashboard,
  SessionFeedback,
  CoachApplication
} from '../models/coaching-session.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CoachingService extends ApiService {
  constructor(http: HttpClient) {
    super(http, `${environment.apiBaseUrl}/api/coaching`);
  }

  // ─── Coaches ───
  getAllCoaches(): Observable<Coach[]> {
    return this.http.get<any>(`${this.baseUrl}/coaches`).pipe(map(res => res.data));
  }

  getCoachById(id: string): Observable<Coach> {
    return this.http.get<any>(`${this.baseUrl}/coaches/${id}`).pipe(map(res => res.data));
  }

  // ─── Sessions ───
  getAllSessions(): Observable<CoachingSession[]> {
    return this.http.get<any>(`${this.baseUrl}/sessions`).pipe(map(res => res.data));
  }

  createSession(sessionData: any): Observable<CoachingSession> {
    return this.http.post<any>(`${this.baseUrl}/sessions`, sessionData).pipe(map(res => res.data));
  }

  getSessionById(id: string): Observable<CoachingSession> {
    return this.http.get<any>(`${this.baseUrl}/sessions/${id}`).pipe(map(res => res.data));
  }

  // ─── Reservations ───
  bookSession(sessionId: string): Observable<CoachingSession> {
    return this.http.post<any>(`${this.baseUrl}/book`, { sessionId }).pipe(map(res => res.data));
  }

  cancelReservation(sessionId: string): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/reservations/${sessionId}`);
  }

  sendMeetingLinks(sessionId: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/sessions/${sessionId}/send-link`, {});
  }

  rejectSession(sessionId: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/sessions/${sessionId}/reject`, {});
  }

  deleteSession(sessionId: string): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/sessions/${sessionId}`);
  }

  getMyReservations(): Observable<CoachingSession[]> {
    return this.http.get<any>(`${this.baseUrl}/reservations`).pipe(map(res => res.data));
  }

  // ─── Recommendations ───
  getRecommendations(): Observable<CoachingSession[]> {
    return this.http.get<any>(`${this.baseUrl}/recommendations`).pipe(map(res => res.data));
  }

  // ─── Feedback ───
  submitFeedback(feedback: SessionFeedback): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/feedback`, {
      coachId: feedback.coachId,
      userId: feedback.userId || 'anonymous',
      rating: feedback.rating,
      comment: feedback.comment || ''
    });
  }

  getCoachFeedbacks(coachId: string): Observable<SessionFeedback[]> {
    return this.http.get<any>(`${this.baseUrl}/coaches/${coachId}/feedbacks`).pipe(map(res => res.data));
  }

  // ─── Dashboard ───
  getDashboard(): Observable<Dashboard> {
    return this.http.get<any>(`${this.baseUrl}/dashboard`).pipe(map(res => res.data));
  }

  // ─── Notifications ───
  getNotifications(): Observable<CoachingNotification[]> {
    return this.http.get<any>(`${this.baseUrl}/notifications`).pipe(map(res => res.data));
  }

  markNotificationRead(id: string): Observable<any> {
    return this.http.patch<any>(`${this.baseUrl}/notifications/${id}/read`, {});
  }

  // ─── Coach Applications ───
  submitCoachApplication(application: Partial<CoachApplication>): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/applications`, application);
  }

  getMyApplicationStatus(): Observable<CoachApplication> {
    return this.http.get<any>(`${this.baseUrl}/applications/my-status`).pipe(map(res => res.data));
  }

  getAllApplications(): Observable<CoachApplication[]> {
    return this.http.get<any>(`${this.baseUrl}/applications`).pipe(map(res => res.data));
  }

  getPendingApplications(): Observable<CoachApplication[]> {
    return this.http.get<any>(`${this.baseUrl}/applications/pending`).pipe(map(res => res.data));
  }

  approveApplication(id: string, adminNote?: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/applications/${id}/approve`, { adminNote: adminNote || '' });
  }

  rejectApplication(id: string, adminNote?: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/applications/${id}/reject`, { adminNote: adminNote || '' });
  }
}
