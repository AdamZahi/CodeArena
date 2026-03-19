import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ApiResponse } from '../../../core/models/api-response.model';

@Injectable({ providedIn: 'root' })
export class PurchaseService extends ApiService {
  constructor(http: HttpClient) {
    super(http, '/api/purchases');
  }

  getAll(): Observable<ApiResponse<unknown>> {
    // TODO: GET /api/purchases
    return this.get<unknown>('');
  }
}
