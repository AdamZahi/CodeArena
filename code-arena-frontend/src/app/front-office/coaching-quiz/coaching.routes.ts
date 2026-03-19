import { Routes } from '@angular/router';

export const COACHING_ROUTES: Routes = [
  { path: 'coaches', loadComponent: () => import('./pages/coach-list/coach-list.component').then((m) => m.CoachListComponent) },
  { path: 'book', loadComponent: () => import('./pages/book-session/book-session.component').then((m) => m.BookSessionComponent) },
  { path: 'quizzes', loadComponent: () => import('./pages/quiz-list/quiz-list.component').then((m) => m.QuizListComponent) },
  { path: 'quizzes/:id', loadComponent: () => import('./pages/take-quiz/take-quiz.component').then((m) => m.TakeQuizComponent) }
];
