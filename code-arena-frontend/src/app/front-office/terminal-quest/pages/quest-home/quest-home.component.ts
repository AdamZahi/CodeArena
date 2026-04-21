import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TimerAudioService } from '../../services/timer-audio.service';

@Component({
  selector: 'app-quest-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './quest-home.component.html',
  styleUrls: ['./quest-home.component.css']
})
export class QuestHomeComponent {
  constructor(public audio: TimerAudioService, private readonly router: Router) {}

  goToSkillReport(): void {
    this.router.navigate(['/terminal-quest/skill-report']);
  }
}
