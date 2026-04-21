import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AdaptivePrediction } from '../models/terminal-quest.model';

@Injectable({ providedIn: 'root' })
export class AdaptiveLearningService {
  private readonly base = `${environment.apiBaseUrl}/api/terminal-quest/adaptive`;

  constructor(private http: HttpClient) {}

  predictAdaptation(userId: string, missionId: string): Observable<AdaptivePrediction> {
    return this.http.post<AdaptivePrediction>(`${this.base}/predict`, { userId, missionId });
  }
}
