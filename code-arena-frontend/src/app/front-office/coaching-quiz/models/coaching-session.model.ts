export interface CoachingSession {
  id: string;
  coachId: string;
  learnerId: string;
  title: string;
  description: string;
  language: string;
  level: 'BASIQUE' | 'INTERMEDIAIRE' | 'AVANCE';
  scheduledAt: string;
  durationMinutes: number;
  status: 'SCHEDULED' | 'RESERVED' | 'CANCELLED' | 'COMPLETED';
  meetingUrl: string;
  maxParticipants: number;
  currentParticipants: number;
  price?: number;
}

export interface Coach {
  id: string;
  userId: string;
  name?: string;
  bio: string;
  specializations: string[];
  rating: number;
  totalSessions: number;
}

export interface UserSkill {
  userId: string;
  language: string;
  level: 'BASIQUE' | 'INTERMEDIAIRE' | 'AVANCE';
  scoreAverage: number;
}

export interface CoachingNotification {
  id: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface CoachingBadge {
  id: string;
  name: string;
  description: string;
  iconUrl: string;
  earnedAt: string;
}

export interface Dashboard {
  totalQuizzesTaken: number;
  averageScore: number;
  overallLevel: 'BASIQUE' | 'INTERMEDIAIRE' | 'AVANCE';
  skills: UserSkill[];
  recommendedSessions: CoachingSession[];
  upcomingSessions: CoachingSession[];
  badges: CoachingBadge[];
  unreadNotifications: number;
}

export interface SessionFeedback {
  coachId: string;
  userId?: string;
  rating: number;
  comment: string;
  createdAt?: string;
}

export interface CoachApplication {
  id?: string;
  userId?: string;
  applicantName: string;
  applicantEmail?: string;
  cvContent: string;
  cvFileBase64?: string;
  cvFileName?: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'NONE';
  adminNote?: string;
  createdAt?: string;
  reviewedAt?: string;
}
