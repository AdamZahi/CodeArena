import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ApiResponse } from '../../../core/models/api-response.model';

@Injectable({ providedIn: 'root' })
export class ChallengeService extends ApiService {
  constructor(http: HttpClient) {
    super(http, '/api/challenges');
  }

  getAll(): Observable<ApiResponse<unknown>> {
    // TODO: GET /api/challenges
    return this.get<unknown>('');
  }
}
