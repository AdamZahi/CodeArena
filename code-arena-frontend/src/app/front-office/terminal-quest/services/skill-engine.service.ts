import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SkillAnalysis } from '../models/terminal-quest.model';

@Injectable({ providedIn: 'root' })
export class SkillEngineService {
  private readonly base = `${environment.apiBaseUrl}/api/terminal-quest/skill`;

  constructor(private http: HttpClient) {}

  analyzePlayer(userId: string): Observable<SkillAnalysis> {
    return this.http.get<SkillAnalysis>(`${this.base}/analyze/${userId}`);
  }
}
