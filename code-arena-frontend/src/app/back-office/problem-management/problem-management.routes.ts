import { Routes } from '@angular/router';

export const PROBLEM_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/problem-list/problem-list.component')
      .then(m => m.ProblemListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./pages/problem-form/problem-form.component')
      .then(m => m.ProblemFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./pages/problem-form/problem-form.component')
      .then(m => m.ProblemFormComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/problem-dashboard/problem-dashboard.component')
      .then(m => m.ProblemDashboardComponent)
  }
];
