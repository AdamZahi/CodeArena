import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ApiResponse } from '../../../core/models/api-response.model';

@Injectable({ providedIn: 'root' })
export class CoachingService extends ApiService {
  constructor(http: HttpClient) {
    super(http, '/api/coaching');
  }

  getAll(): Observable<ApiResponse<unknown>> {
    // TODO: GET /api/coaching
    return this.get<unknown>('');
  }
}
