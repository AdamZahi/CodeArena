import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';

export abstract class ApiService {
  protected constructor(protected readonly http: HttpClient, protected readonly baseUrl: string) {}

  protected get<T>(url = ''): Observable<ApiResponse<T>> {
    // TODO: Add query parameter support and typed errors.
    return this.http.get<ApiResponse<T>>(`${this.baseUrl}${url}`);
  }

  protected post<T>(url = '', payload?: unknown): Observable<ApiResponse<T>> {
    // TODO: Add idempotency and request tracing headers.
    return this.http.post<ApiResponse<T>>(`${this.baseUrl}${url}`, payload);
  }

  protected put<T>(url = '', payload?: unknown): Observable<ApiResponse<T>> {
    // TODO: Add optimistic concurrency support.
    return this.http.put<ApiResponse<T>>(`${this.baseUrl}${url}`, payload);
  }

  protected delete<T>(url = ''): Observable<ApiResponse<T>> {
    // TODO: Add soft-delete conventions where required.
    return this.http.delete<ApiResponse<T>>(`${this.baseUrl}${url}`);
  }
}
