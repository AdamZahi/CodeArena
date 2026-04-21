import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/role.guard';
import { authGuard } from '../../core/auth/auth.guard';

export const COACHING_ROUTES: Routes = [
  { path: 'coaches', loadComponent: () => import('./pages/coach-list/coach-list.component').then((m) => m.CoachListComponent) },
  { path: 'coaches/:userId/sessions', loadComponent: () => import('./pages/coach-sessions-view/coach-sessions-view.component').then((m) => m.CoachSessionsViewComponent) },
  { path: 'evaluate/:coachId', canActivate: [authGuard], loadComponent: () => import('./pages/coach-evaluate/coach-evaluate.component').then((m) => m.CoachEvaluateComponent) },
  { path: 'sessions', canActivate: [authGuard, roleGuard], data: { role: '!COACH', fallbackUrl: '/coaching-quiz/coach-dashboard' }, loadComponent: () => import('./pages/book-session/book-session.component').then((m) => m.BookSessionComponent) },
  { path: 'my-training', canActivate: [authGuard, roleGuard], data: { role: '!COACH', fallbackUrl: '/coaching-quiz/coach-dashboard' }, loadComponent: () => import('./pages/my-training/my-training.component').then((m) => m.MyTrainingComponent) },
  { path: 'pay/:sessionId', canActivate: [authGuard], loadComponent: () => import('./pages/session-payment/session-payment.component').then((m) => m.SessionPaymentComponent) },
  { path: 'coach-dashboard', canActivate: [authGuard, roleGuard], data: { role: 'COACH', fallbackUrl: '/coaching-quiz/sessions' }, loadComponent: () => import('./pages/coach-dashboard/coach-dashboard.component').then((m) => m.CoachDashboardComponent) },
  { path: 'coach-create-session', canActivate: [authGuard, roleGuard], data: { role: 'COACH', fallbackUrl: '/coaching-quiz/sessions' }, loadComponent: () => import('./pages/coach-create-session/coach-create-session.component').then((m) => m.CoachCreateSessionComponent) },
  { path: 'coach-reservations', canActivate: [authGuard, roleGuard], data: { role: 'COACH', fallbackUrl: '/coaching-quiz/sessions' }, loadComponent: () => import('./pages/coach-reservations/coach-reservations.component').then((m) => m.CoachReservationsComponent) },
  { path: 'coach-ai-assistant', canActivate: [authGuard, roleGuard], data: { role: 'COACH', fallbackUrl: '/coaching-quiz/sessions' }, loadComponent: () => import('./pages/coach-ai-assistant/coach-ai-assistant.component').then((m) => m.CoachAiAssistantComponent) },
  { path: 'book', redirectTo: 'sessions', pathMatch: 'full' },
  { path: 'quizzes', canActivate: [authGuard, roleGuard], data: { role: '!COACH', fallbackUrl: '/coaching-quiz/coach-dashboard' }, loadComponent: () => import('./pages/quiz-list/quiz-list.component').then((m) => m.QuizListComponent) },
  { path: 'quizzes/:id', canActivate: [authGuard, roleGuard], data: { role: '!COACH', fallbackUrl: '/coaching-quiz/coach-dashboard' }, loadComponent: () => import('./pages/take-quiz/take-quiz.component').then((m) => m.TakeQuizComponent) },
  { path: 'apply-coach', canActivate: [authGuard], loadComponent: () => import('./pages/coach-apply/coach-apply.component').then((m) => m.CoachApplyComponent) },
  { path: 'ai-code-mentor', canActivate: [authGuard, roleGuard], data: { role: '!COACH', fallbackUrl: '/coaching-quiz/coach-dashboard' }, loadComponent: () => import('./pages/ai-code-mentor/ai-code-mentor.component').then((m) => m.AiCodeMentorComponent) },
  { path: 'admin/applications', canActivate: [authGuard], loadComponent: () => import('./pages/admin-applications/admin-applications.component').then((m) => m.AdminApplicationsComponent) },
  { path: 'admin/quizzes', canActivate: [authGuard], loadComponent: () => import('./pages/admin-quizzes/admin-quizzes.component').then((m) => m.AdminQuizzesComponent) },
  { path: '', redirectTo: 'coaches', pathMatch: 'full' }
];
