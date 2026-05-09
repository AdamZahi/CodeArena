import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  EventCandidature,
  EventInvitation,
  EventRegistration,
  ProgrammingEvent
} from '../models/event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);

  private readonly baseUrl = `${environment.apiBaseUrl}/api/events`;

  getEvents(): Observable<any[]> {
    return this.http.get<any[]>(this.baseUrl);
  }

  getRecommendedEvents(): Observable<ProgrammingEvent[]> {
    return this.http.get<ProgrammingEvent[]>(`${this.baseUrl}/recommended`);
  }

  getEventById(id: string): Observable<ProgrammingEvent> {
    return this.http.get<ProgrammingEvent>(`${this.baseUrl}/${id}`);
  }

  createEvent(event: any): Observable<ProgrammingEvent> {
    return this.http.post<ProgrammingEvent>(this.baseUrl, event);
  }

  register(eventId: string): Observable<any> {
    return this.http.post<any>(
      `${this.baseUrl}/${eventId}/register`,
      {}
    );
  }

  cancelRegistration(eventId: string): Observable<any> {
    return this.http.delete(
      `${this.baseUrl}/${eventId}/register`
    );
  }

  getParticipants(eventId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${eventId}/participants`);
  }

  getMyRegistrations(): Observable<EventRegistration[]> {
    return this.http.get<EventRegistration[]>(`${this.baseUrl}/me/registrations`);
  }

  getEventParticipants(eventId: string): Observable<EventRegistration[]> {
    return this.http.get<EventRegistration[]>(`${this.baseUrl}/${eventId}/participants`);
  }

  getMyInvitations(): Observable<EventInvitation[]> {
    return this.http.get<EventInvitation[]>(`${this.baseUrl}/me/invitations`);
  }

  acceptInvitation(eventId: string): Observable<any> {
    return this.http.put(
      `${this.baseUrl}/${eventId}/invitation/accept`,
      {}
    );
  }

  declineInvitation(eventId: string): Observable<any> {
    return this.http.put(
      `${this.baseUrl}/${eventId}/invitation/decline`,
      {}
    );
  }

  submitCandidature(eventId: string, motivation: string): Observable<any> {
    const body = { motivation };
    return this.http.post<any>(`${this.baseUrl}/${eventId}/candidature`, body);
  }

  getCandidaturesByEvent(eventId: string): Observable<EventCandidature[]> {
    return this.http.get<EventCandidature[]>(`${this.baseUrl}/${eventId}/candidatures`);
  }

  deleteEvent(eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${eventId}`);
  }

  acceptCandidature(candidatureId: string): Observable<EventRegistration> {
    return this.http.put<EventRegistration>(
      `${this.baseUrl}/candidature/${candidatureId}/accept`,
      {}
    );
  }

  rejectCandidature(candidatureId: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/candidature/${candidatureId}/reject`, {});
  }

  inviteTop10(eventId: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${eventId}/invite-top10`, {});
  }
}
