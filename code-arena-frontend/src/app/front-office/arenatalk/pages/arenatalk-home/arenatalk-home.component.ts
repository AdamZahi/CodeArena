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
export class ArenaTalkHomeComponent {
  constructor(private router: Router) {}

goToCreateHub() {
  this.router.navigate(['/arenatalk/create']);
}

goToStart() {
  this.router.navigate(['/arenatalk/start']);
}
}