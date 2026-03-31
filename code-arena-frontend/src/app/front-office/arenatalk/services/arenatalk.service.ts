import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Hub, TextChannel, Message } from '../models/arenatalk.model';

@Injectable({
  providedIn: 'root'
})
export class ArenatalkService {
  private apiUrl = '/api/arenatalk';

  constructor(private http: HttpClient) {}

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

  getChannelsByHub(hubId: number): Observable<TextChannel[]> {
    return this.http.get<TextChannel[]>(`${this.apiUrl}/hubs/${hubId}/channels`);
  }

  createChannel(hubId: number, channel: TextChannel): Observable<TextChannel> {
    return this.http.post<TextChannel>(`${this.apiUrl}/hubs/${hubId}/channels`, channel);
  }

  deleteChannel(channelId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/channels/${channelId}`);
  }

  getMessagesByChannel(channelId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/channels/${channelId}/messages`);
  }

  sendMessage(channelId: number, message: Message): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/channels/${channelId}/messages`, message);
  }

  deleteMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/messages/${messageId}`);
  }
  updateHub(id: number, hub: Hub) {
  return this.http.put<Hub>(`${this.apiUrl}/hubs/${id}`, hub);
}

updateChannel(channelId: number, channel: TextChannel) {
  return this.http.put<TextChannel>(`${this.apiUrl}/channels/${channelId}`, channel);
}

updateMessage(messageId: number, message: Message) {
  return this.http.put<Message>(`${this.apiUrl}/messages/${messageId}`, message);
}
joinHub(hubId: number, userId: string): Observable<any> {
  return this.http.post(`${this.apiUrl}/memberships/join?hubId=${hubId}&userId=${userId}`, {});
}

getMembersByHub(hubId: number): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiUrl}/memberships/hub/${hubId}`);
}

getHubsByUser(userId: string): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiUrl}/memberships/user/${userId}`);
}

leaveHub(hubId: number, userId: string): Observable<void> {
  return this.http.delete<void>(`${this.apiUrl}/memberships/leave?hubId=${hubId}&userId=${userId}`);
}
}