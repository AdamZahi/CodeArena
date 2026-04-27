import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VoiceChannel {
  id: number;
  name: string;
  maxParticipants: number;
}

@Injectable({ providedIn: 'root' })
export class VoiceChannelService {
  private apiUrl = '/api/arenatalk/hubs';

  constructor(private http: HttpClient) {}

  getByHub(hubId: number): Observable<VoiceChannel[]> {
    return this.http.get<VoiceChannel[]>(`${this.apiUrl}/${hubId}/voice-channels`);
  }

  create(hubId: number, name: string): Observable<VoiceChannel> {
    return this.http.post<VoiceChannel>(
      `${this.apiUrl}/${hubId}/voice-channels?name=${encodeURIComponent(name)}`,
      {}
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/voice-channels/${id}`);
  }
}