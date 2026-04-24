import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CyberAlertComponent } from './coaching-quiz/components/cyber-alert/cyber-alert.component';

@Component({
  selector: 'app-fo-shell',
  standalone: true,
  imports: [RouterOutlet, CyberAlertComponent],
  template: `
    <app-cyber-alert></app-cyber-alert>
    <router-outlet />
  `
})
export class FoShellComponent {}
