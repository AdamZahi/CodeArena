import { Routes } from '@angular/router';

export const TERMINAL_QUEST_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/quest-home/quest-home.component').then((m) => m.QuestHomeComponent)
  },
  {
    path: 'story',
    loadComponent: () => import('./pages/chapter-map/chapter-map.component').then((m) => m.ChapterMapComponent)
  },
  {
    path: 'story/play/:levelId',
    loadComponent: () => import('./pages/level-play/level-play.component').then((m) => m.LevelPlayComponent)
  },
  {
    path: 'survival',
    loadComponent: () => import('./pages/survival-play/survival-play.component').then((m) => m.SurvivalPlayComponent)
  },
  {
    path: 'survival/leaderboard',
    loadComponent: () => import('./pages/survival-leaderboard/survival-leaderboard.component').then((m) => m.SurvivalLeaderboardComponent)
  }
];
