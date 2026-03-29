import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-arenatalk-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './arenatalk-home.component.html',
  styleUrl: './arenatalk-home.component.css'
})
export class ArenatalkHomeComponent {
  constructor(private router: Router) {}

  goToJoin(): void {
    this.router.navigate(['/arenatalk/workspace']);
  }

  goToCreate(): void {
    this.router.navigate(['/arenatalk/create']);
  }

  goToMySpace(): void {
    this.router.navigate(['/arenatalk/workspace']);
  }
}