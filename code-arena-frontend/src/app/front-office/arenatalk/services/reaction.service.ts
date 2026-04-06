import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MessageReaction } from '../models/arenatalk.model';

@Injectable({ providedIn: 'root' })
export class ReactionService {
  private apiUrl = '/api/arenatalk';

  constructor(private http: HttpClient) {}

  toggle(messageId: number, emoji: string, keycloakId: string): Observable<MessageReaction> {
    const params = new HttpParams()
      .set('emoji', emoji)
      .set('keycloakId', keycloakId);
    return this.http.post<MessageReaction>(
      `${this.apiUrl}/messages/${messageId}/reactions`, {}, { params }
    );
  }

  getForChannel(messageIds: number[], keycloakId: string): Observable<{ [id: number]: MessageReaction }> {
    const params = new HttpParams().set('keycloakId', keycloakId);
    return this.http.post<{ [id: number]: MessageReaction }>(
      `${this.apiUrl}/messages/reactions/batch`, messageIds, { params }
    );
  }
}