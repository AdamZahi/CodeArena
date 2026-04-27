import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CommandExplainerService {

  private readonly base = `${environment.apiBaseUrl}/api/terminal-quest/explain`;

  constructor(private http: HttpClient) {}

  explain(
    command: string,
    missionTask: string,
    missionContext: string,
    difficulty: string,
    isCorrect: boolean
  ): Observable<{ explanation: string }> {
    return this.http.post<{ explanation: string }>(this.base, {
      command,
      missionTask,
      missionContext,
      difficulty,
      isCorrect
    });
  }
}
