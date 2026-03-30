import { Routes } from '@angular/router';

export const ARENATALK_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/arenatalk-home/arenatalk-home.component').then(
        (m) => m.ArenatalkHomeComponent
      )
  },
  {
    path: 'create',
    loadComponent: () =>
      import('./pages/arenatalk-create/arenatalk-create.component').then(
        (m) => m.ArenatalkCreateComponent
      )
  },
  {
    path: 'join',
    loadComponent: () =>
      import('./pages/arenatalk-join/arenatalk-join.component').then(
        (m) => m.ArenaTalkJoinComponent
      )
  },
  {
    path: 'workspace',
    loadComponent: () =>
      import('./pages/arenatalk-workspace/arenatalk-workspace.component').then(
        (m) => m.ArenatalkWorkspaceComponent
      )
  }
];