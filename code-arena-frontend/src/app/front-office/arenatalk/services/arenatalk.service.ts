import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Hub, TextChannel, Message } from '../models/arenatalk.model';

@Injectable({
  providedIn: 'root'
})
export class ArenaTalkService {
  private apiUrl = 'http://localhost:8080/api/arenatalk';

  constructor(private http: HttpClient) {}

  // HUBS
  getHubs(): Observable<Hub[]> {
    return this.http.get<Hub[]>(`${this.apiUrl}/hubs`);
  }

  getHubById(id: number): Observable<Hub> {
    return this.http.get<Hub>(`${this.apiUrl}/hubs/${id}`);
  }

  createHub(hub: Hub): Observable<Hub> {
    return this.http.post<Hub>(`${this.apiUrl}/hubs`, hub);
  }

  deleteHub(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/hubs/${id}`);
  }

  // CHANNELS
  getChannelsByHub(hubId: number): Observable<TextChannel[]> {
    return this.http.get<TextChannel[]>(`${this.apiUrl}/hubs/${hubId}/channels`);
  }

  createChannel(hubId: number, channel: TextChannel): Observable<TextChannel> {
  return this.http.post<TextChannel>(`${this.apiUrl}/hubs/${hubId}/channels`, channel);
}

  deleteChannel(channelId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/channels/${channelId}`);
  }

  // MESSAGES
  getMessagesByChannel(channelId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/channels/${channelId}/messages`);
  }

  sendMessage(channelId: number, message: Message): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/channels/${channelId}/messages`, message);
  }

  deleteMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/messages/${messageId}`);
  }
}