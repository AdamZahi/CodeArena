import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SemanticSearchResult {
  id: string;
  content: string;
  sender: string;
  score: number;
}

export interface SemanticSearchResponse {
  results: SemanticSearchResult[];
}

@Injectable({ providedIn: 'root' })
export class AiSemanticSearchService {
  private apiUrl = 'http://localhost:8080/api/search/semantic';

  constructor(private http: HttpClient) {}

  search(query: string, channelId: number): Observable<SemanticSearchResponse> {
    return this.http.post<SemanticSearchResponse>(this.apiUrl, { query, channelId });
  }
}