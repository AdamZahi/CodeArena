import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface AdminUser {
  id: string;
  auth0Id: string;
  email: string;
  nickname: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  constructor(private http: HttpClient) {}

  listUsers(): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(`${environment.apiBaseUrl}/api/users?size=1000`);
  }
}
