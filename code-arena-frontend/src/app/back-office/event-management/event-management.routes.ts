import { Routes } from '@angular/router';
import { AdminEventListComponent } from './pages/admin-event-list/admin-event-list.component';
import { AdminEventDetailComponent } from './pages/admin-event-detail/admin-event-detail.component';
import { CreateEventComponent } from './pages/create-event/create-event.component';

export const EVENT_MANAGEMENT_ROUTES: Routes = [
  { path: '', component: AdminEventListComponent },
  { path: 'create', component: CreateEventComponent },
  { path: ':id', component: AdminEventDetailComponent }
];

