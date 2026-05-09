import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  // ── FRONT OFFICE ─────────────────────────────
  // Protected by authGuard — must be logged in
  {
    path: '',
    canActivate: [authGuard],
    loadChildren: () => import('./front-office/fo.routes')
      .then((m) => m.FO_ROUTES)
  },

  // ── BACK OFFICE (Admin ONLY) ──────────────────
  // Protected by BOTH guards:
  // 1. authGuard — must be logged in
  // 2. roleGuard — must have ADMIN role
  // data.role = 'ADMIN' is what roleGuard checks against
  // If not ADMIN → redirects to /forbidden automatically
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],  // ← THE FIX
    data: { role: 'ADMIN' },
    loadChildren: () => import('./back-office/bo.routes')
      .then((m) => m.BO_ROUTES)
  },

  // ── PUBLIC ROUTES ─────────────────────────────
  // No guards — anyone can access these
  {
    path: 'login',
    loadComponent: () => import('./login/login.component')
      .then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./register/register.component')
      .then((m) => m.RegisterComponent)
  },

  // ── FORBIDDEN PAGE ────────────────────────────
  // Shown when roleGuard redirects non-admin users
  {
    path: 'forbidden',
    loadComponent: () => import('./forbidden/forbidden.component')
      .then((m) => m.ForbiddenComponent)
  }
];