import { Routes } from '@angular/router';

export const REWARD_PROFILE_ROUTES: Routes = [
  { path: 'my-profile', loadComponent: () => import('./pages/my-profile/my-profile.component').then((m) => m.MyProfileComponent) },
  { path: 'public/:id', loadComponent: () => import('./pages/public-profile/public-profile.component').then((m) => m.PublicProfileComponent) },
  { path: 'leaderboard', loadComponent: () => import('./pages/global-leaderboard/global-leaderboard.component').then((m) => m.GlobalLeaderboardComponent) }
];
