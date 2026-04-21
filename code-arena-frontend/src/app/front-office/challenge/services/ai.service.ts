import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface RecommendationDto {
  challengeId: number;
  title: string;
  difficulty: string;
  tags: string;
  aiDifficultyScore: number;
  matchScore: number;
  reason: string;
}

export interface UserSkillProfileDto {
  userId: string;
  skillMap: { [key: string]: number };
  overallRating: number;
  strongestTag: string;
  weakestTag: string;
}

export interface ChallengeDifficultyDto {
  challengeId: number;
  aiDifficultyScore: number;
  passRate: number;
  avgAttempts: number;
  humanDifficulty: string;
}

export interface HintResponseDto {
  title: string;
  tags: string;
  predicted_category: string;
  hint: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/ai`;

  constructor(private http: HttpClient) {}

  getRecommendations(count: number = 3): Observable<RecommendationDto[]> {
    return this.http.get<RecommendationDto[]>(`${this.baseUrl}/recommendations?count=${count}`);
  }

  getSkillProfile(): Observable<UserSkillProfileDto> {
    return this.http.get<UserSkillProfileDto>(`${this.baseUrl}/skill-profile`);
  }

  getChallengeDifficulty(id: number): Observable<ChallengeDifficultyDto> {
    return this.http.get<ChallengeDifficultyDto>(`${this.baseUrl}/challenge/${id}/difficulty`);
  }

  getChallengeHint(id: number): Observable<HintResponseDto> {
    return this.http.get<HintResponseDto>(`${this.baseUrl}/challenge/${id}/hint`);
  }
}
