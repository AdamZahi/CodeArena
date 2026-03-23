import { Routes } from '@angular/router';

export const SUPPORT_ROUTES: Routes = [
  { path: 'submit', loadComponent: () => import('./pages/submit-report/submit-report.component').then((m) => m.SubmitReportComponent) },
  { path: 'my-reports', loadComponent: () => import('./pages/my-reports/my-reports.component').then((m) => m.MyReportsComponent) }
];
