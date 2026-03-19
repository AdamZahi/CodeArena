import { ApiResponse } from './api-response.model';

export interface PagedResponse<T> extends ApiResponse<T[]> {
  page: number;
  size: number;
  total: number;
}
