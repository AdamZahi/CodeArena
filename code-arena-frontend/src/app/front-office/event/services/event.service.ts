import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EventCandidature,
  EventInvitation,
  EventRegistration,
  ProgrammingEvent
} from '../models/event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);

  private readonly baseUrl = 'http://localhost:8080/api/events';
  private readonly participantId = 'mock-player-1';

  getEvents(): Observable<any[]> {
    return this.http.get<any[]>(this.baseUrl);
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
      {},
      { params: { participantId: this.participantId } }
    );
  }

  cancelRegistration(eventId: string): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/${eventId}/participants/${this.participantId}`);
  }

  getParticipants(eventId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${eventId}/participants`);
  }

  getMyRegistrations(): Observable<EventRegistration[]> {
    return this.http.get<EventRegistration[]>(`${this.baseUrl}/me/registrations`, {
      params: { participantId: this.participantId }
    });
  }

  getEventParticipants(eventId: string): Observable<EventRegistration[]> {
    return this.http.get<EventRegistration[]>(`${this.baseUrl}/${eventId}/participants`);
  }

  getMyInvitations(): Observable<EventInvitation[]> {
    return this.http.get<EventInvitation[]>(`${this.baseUrl}/me/invitations`, {
      params: { participantId: this.participantId }
    });
  }

  acceptInvitation(eventId: string): Observable<any> {
    return this.http.put(
      `${this.baseUrl}/${eventId}/invitation/accept`,
      {},
      { params: { participantId: this.participantId } }
    );
  }

  declineInvitation(eventId: string): Observable<any> {
    return this.http.put(
      `${this.baseUrl}/${eventId}/invitation/decline`,
      {},
      { params: { participantId: this.participantId } }
    );
  }

  submitCandidature(eventId: string, motivation: string): Observable<any> {
    const body = { motivation };
    return this.http.post<any>(`${this.baseUrl}/${eventId}/candidature`, body, {
      params: { participantId: this.participantId }
    });
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
