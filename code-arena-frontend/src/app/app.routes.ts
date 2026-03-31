import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./front-office/fo.routes').then((m) => m.FO_ROUTES)
  },
  {
    path: 'admin',
    data: { role: 'ADMIN' },
    loadChildren: () => import('./back-office/bo.routes').then((m) => m.BO_ROUTES)
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./register/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'forbidden',
    loadComponent: () => import('./forbidden/forbidden.component').then((m) => m.ForbiddenComponent)
  }
];
