import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  imports: [CommonModule]
})
export class HomeComponent {
  private readonly router = inject(Router);

  navigate(path: string): void {
    this.router.navigate([path]);
  }
}
