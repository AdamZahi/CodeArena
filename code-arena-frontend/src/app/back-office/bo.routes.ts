import { Routes } from '@angular/router';
import { BoShellComponent } from './bo-shell.component';

export const BO_ROUTES: Routes = [
  {
    path: '',
    component: BoShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'users', loadComponent: () => import('./user-management/pages/user-list/user-list.component').then((m) => m.UserListComponent) },
      { path: 'users/:id', loadComponent: () => import('./user-management/pages/user-detail/user-detail.component').then((m) => m.UserDetailComponent) },
      { path: 'challenges', loadComponent: () => import('./challenge-management/challenge-management.component').then((m) => m.ChallengeManagementComponent) },
      { path: 'problems', loadChildren: () => import('./problem-management/problem-management.routes').then((m) => m.PROBLEM_MANAGEMENT_ROUTES) },
      { path: 'battles', loadComponent: () => import('./battle-management/battle-management.component').then((m) => m.BattleManagementComponent) },
      { path: 'shop', loadChildren: () => import('./shop-management/shop-management.routes').then((m) => m.SHOP_MANAGEMENT_ROUTES) },
      { path: 'reports', loadComponent: () => import('./support-management/support-management.component').then((m) => m.SupportManagementComponent) },
      { path: 'events', loadChildren: () => import('./event-management/event-management.routes').then((m) => m.EVENT_MANAGEMENT_ROUTES) },
      { path: 'coaching', loadComponent: () => import('./coaching-management/coaching-management.component').then((m) => m.CoachingManagementComponent) },
      { path: 'terminal-quest', loadChildren: () => import('./terminal-quest-management/terminal-quest-management.routes').then((m) => m.TERMINAL_QUEST_MANAGEMENT_ROUTES) }
    ]
  }
];
