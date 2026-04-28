export interface Quiz {
  id: string;
  title: string;
  description: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  language: string;
  totalPoints: number;
  category: string;
  createdBy: string;
  createdAt: string;
  questions?: Question[];
}

export interface Question {
  id: string;
  quizId: string;
  content: string;
  type: 'MCQ' | 'CODE_COMPLETION' | 'CODE_ANALYSIS';
  language: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  points: number;
  correctAnswer: string;
  explanation: string;
  codeSnippet?: string;
  options?: string;
}

export interface SubmitQuizRequest {
  quizId: string;
  answers: { [questionId: string]: string };
}

export interface QuizResult {
  attemptId: string;
  quizId: string;
  score: number;
  totalPoints: number;
  percentage: number;
  level: 'BASIQUE' | 'INTERMEDIAIRE' | 'AVANCE';
  weakTopics: string[];
  answerResults: AnswerResult[];
  completedAt: string;
}

export interface AnswerResult {
  questionId: string;
  userAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
  explanation: string;
  points: number;
}
