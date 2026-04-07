export interface StoryMission {
  id: string;
  chapterId: string;
  title: string;
  context: string;
  task: string;
  hint: string;
  orderIndex: number;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  isBoss: boolean;
  xpReward: number;
  speakerName?: string;
  speakerVoice?: string;
  createdAt: string;
}

export interface StoryChapter {
  id: string;
  title: string;
  description: string;
  orderIndex: number;
  isLocked: boolean;
  speakerName?: string;
  speakerVoice?: string;
  createdAt: string;
  levels?: StoryLevel[];
  missions?: StoryMission[];
}

export interface StoryLevel {
  id: string;
  chapterId: string;
  title: string;
  scenario: string;
  hint: string;
  orderIndex: number;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  isBoss: boolean;
  xpReward: number;
  createdAt: string;
}

export interface LevelProgress {
  id: string;
  userId: string;
  levelId?: string;
  missionId?: string;
  completed: boolean;
  starsEarned: number;
  attempts: number;
  completedAt: string;
  createdAt: string;
}

export interface SubmitAnswerResponse {
  correct: boolean;
  starsEarned: number;
  xpEarned: number;
  attempts: number;
  message: string;
}

export interface SurvivalSession {
  id: string;
  userId: string;
  waveReached: number;
  score: number;
  livesRemaining: number;
  startedAt: string;
  endedAt: string;
  createdAt: string;
}

export interface SurvivalAnswerResponse {
  correct: boolean;
  livesRemaining: number;
  waveReached: number;
  score: number;
  gameOver: boolean;
  message: string;
  nextChallenge: StoryLevel | null;
}

export interface SurvivalLeaderboardEntry {
  id: string;
  userId: string;
  bestWave: number;
  bestScore: number;
}

export interface PlayerStats {
  userId: string;
  totalLevelsCompleted: number;
  totalStarsEarned: number;
  totalAttempts: number;
  bestWave: number;
  bestScore: number;
  totalSurvivalSessions: number;
}

export interface ChapterCompletionStats {
  chapterId: string;
  chapterTitle: string;
  totalLevels: number;
  totalAttempts: number;
  totalCompletions: number;
  completionRate: number;
}

export interface GlobalStats {
  totalActivePlayers: number;
  totalSurvivalSessions: number;
  totalStoryAttempts: number;
  totalStoryCompletions: number;
  overallCompletionRate: number;
  chapterStats: ChapterCompletionStats[];
}
