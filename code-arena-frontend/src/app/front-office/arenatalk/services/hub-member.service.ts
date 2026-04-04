import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HubMember {
  id: number;
  role: 'OWNER' | 'MEMBER' | 'PENDING';
  status: 'ACTIVE' | 'PENDING' | 'BANNED';
  joinedAt: string;
  user: {
    id: string;
    firstName: string;
    lastName: string;
    avatarUrl: string;
    email: string;
  };
}

@Injectable({ providedIn: 'root' })
export class HubMemberService {

  private apiUrl = '/api/arenatalk/hubs';

  constructor(private http: HttpClient) {}

  joinHub(hubId: number, keycloakId: string): Observable<HubMember> {
    return this.http.post<HubMember>(`${this.apiUrl}/${hubId}/join`, { keycloakId });
  }

  leaveHub(hubId: number, keycloakId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${hubId}/leave`, {
      body: { keycloakId }
    });
  }

  getMembers(hubId: number): Observable<HubMember[]> {
    return this.http.get<HubMember[]>(`${this.apiUrl}/${hubId}/members`);
  }

  getPendingRequests(hubId: number, keycloakId: string): Observable<HubMember[]> {
    const params = new HttpParams().set('keycloakId', keycloakId);
    return this.http.get<HubMember[]>(`${this.apiUrl}/${hubId}/requests`, { params });
  }

  acceptRequest(hubId: number, memberId: number, keycloakId: string): Observable<HubMember> {
    return this.http.post<HubMember>(
      `${this.apiUrl}/${hubId}/requests/${memberId}/accept`,
      { keycloakId }
    );
  }

  rejectRequest(hubId: number, memberId: number, keycloakId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${hubId}/requests/${memberId}/reject`,
      { body: { keycloakId } }
    );
  }
}