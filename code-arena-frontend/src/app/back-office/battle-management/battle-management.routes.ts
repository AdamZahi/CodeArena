import { Routes } from '@angular/router';

export const BATTLE_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./battle-management.component').then((m) => m.BattleManagementComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'analytics' },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./analytics/battle-analytics-dashboard.component').then(
            (m) => m.BattleAnalyticsDashboardComponent
          )
      },
      {
        path: 'rooms',
        loadComponent: () =>
          import('./management/battle-rooms-list.component').then((m) => m.BattleRoomsListComponent)
      },
      {
        path: 'rooms/:id',
        loadComponent: () =>
          import('./management/battle-room-detail.component').then((m) => m.BattleRoomDetailComponent)
      },
      {
        path: 'config',
        loadComponent: () =>
          import('./management/battle-config-form.component').then((m) => m.BattleConfigFormComponent)
      },
      {
        path: 'ops',
        loadComponent: () =>
          import('./ops/battle-ops-page.component').then((m) => m.BattleOpsPageComponent)
      }
    ]
  }
];
