import { Routes } from '@angular/router';

export const CHALLENGE_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/challenge-list/challenge-list.component').then((m) => m.ChallengeListComponent) },
  { path: ':id', loadComponent: () => import('./pages/challenge-detail/challenge-detail.component').then((m) => m.ChallengeDetailComponent) },
  { path: ':id/submit', loadComponent: () => import('./pages/submit-code/submit-code.component').then((m) => m.SubmitCodeComponent) },
  { path: ':id/leaderboard', loadComponent: () => import('./pages/leaderboard/leaderboard.component').then((m) => m.LeaderboardComponent) }
];
