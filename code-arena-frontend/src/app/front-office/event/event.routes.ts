import { Routes } from '@angular/router';

export const EVENT_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/event-list/event-list.component').then((m) => m.EventListComponent) },
  { path: ':id', loadComponent: () => import('./pages/event-detail/event-detail.component').then((m) => m.EventDetailComponent) },
  { path: 'create', loadComponent: () => import('./pages/create-event/create-event.component').then((m) => m.CreateEventComponent) }
];
