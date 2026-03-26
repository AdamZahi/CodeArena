import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-arenatalk-start',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './arenatalk-start.component.html',
  styleUrl: './arenatalk-start.component.css'
})
export class ArenaTalkStartComponent {
  constructor(private router: Router) {}

  goToCreateHub(): void {
    this.router.navigate(['/arenatalk/create']);
  }

  goToJoinHub(): void {
    this.router.navigate(['/arenatalk/hubs']);
  }
}