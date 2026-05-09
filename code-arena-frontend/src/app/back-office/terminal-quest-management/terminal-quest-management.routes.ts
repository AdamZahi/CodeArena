import { Routes } from '@angular/router';

export const TERMINAL_QUEST_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/tq-dashboard/tq-dashboard.component').then((m) => m.TqDashboardComponent)
  },
  {
    path: 'chapters',
    loadComponent: () => import('./pages/chapter-list/chapter-list.component').then((m) => m.ChapterListComponent)
  },
  {
    path: 'chapters/new',
    loadComponent: () => import('./pages/chapter-form/chapter-form.component').then((m) => m.ChapterFormComponent)
  },
  {
    path: 'chapters/edit/:id',
    loadComponent: () => import('./pages/chapter-form/chapter-form.component').then((m) => m.ChapterFormComponent)
  },
  {
    path: 'levels/:chapterId',
    loadComponent: () => import('./pages/level-list/level-list.component').then((m) => m.LevelListComponent)
  },
  {
    path: 'levels/:chapterId/new',
    loadComponent: () => import('./pages/level-form/level-form.component').then((m) => m.LevelFormComponent)
  },
  {
    path: 'levels/:chapterId/edit/:id',
    loadComponent: () => import('./pages/level-form/level-form.component').then((m) => m.LevelFormComponent)
  },
  {
    path: 'missions/:chapterId',
    loadComponent: () => import('./pages/mission-list/mission-list.component').then((m) => m.MissionListComponent)
  },
  {
    path: 'missions/:chapterId/new',
    loadComponent: () => import('./pages/mission-form/mission-form.component').then((m) => m.MissionFormComponent)
  },
  {
    path: 'missions/:chapterId/edit/:id',
    loadComponent: () => import('./pages/mission-form/mission-form.component').then((m) => m.MissionFormComponent)
  },
  {
    path: 'player/:userId',
    loadComponent: () => import('./pages/player-timeline/player-timeline.component').then((m) => m.PlayerTimelineComponent)
  }
];
