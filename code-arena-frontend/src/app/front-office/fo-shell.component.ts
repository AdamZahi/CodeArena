import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationComponent } from './shop/notification/notification.component';

@Component({
  selector: 'app-fo-shell',
  standalone: true,
  imports: [RouterOutlet, NotificationComponent],
  template: `
    <router-outlet />
    <app-notification />
  `
})
export class FoShellComponent {}