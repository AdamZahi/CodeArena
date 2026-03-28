import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-quest-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './quest-home.component.html',
  styleUrls: ['./quest-home.component.css']
})
export class QuestHomeComponent {}
