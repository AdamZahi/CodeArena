import { Routes } from '@angular/router';
import { EventListComponent } from './pages/event-list/event-list.component';
import { CreateEventComponent } from './pages/create-event/create-event.component';
import { EventDetailComponent } from './pages/event-detail/event-detail.component';
import { MyRegistrationsComponent } from './pages/my-registrations/my-registrations.component';
import { MyInvitationsComponent } from './pages/my-invitations/my-invitations.component';

export const EVENT_ROUTES: Routes = [
  { path: '', component: EventListComponent },
  { path: 'create', component: CreateEventComponent },
  { path: 'my-registrations', component: MyRegistrationsComponent },
  { path: 'my-invitations', component: MyInvitationsComponent },
  { path: ':id', component: EventDetailComponent },
];
