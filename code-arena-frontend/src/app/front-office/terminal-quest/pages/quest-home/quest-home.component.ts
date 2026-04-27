import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TimerAudioService } from '../../services/timer-audio.service';
import { VoiceNavigationService } from '../../services/voice-navigation.service';
import { VoiceNavWidgetComponent } from '../../components/voice-nav-widget/voice-nav-widget.component';

@Component({
  selector: 'app-quest-home',
  standalone: true,
  imports: [CommonModule, RouterLink, VoiceNavWidgetComponent],
  templateUrl: './quest-home.component.html',
  styleUrls: ['./quest-home.component.css']
})
export class QuestHomeComponent implements OnInit, OnDestroy {
  constructor(
    public audio: TimerAudioService,
    private readonly router: Router,
    private readonly voiceNav: VoiceNavigationService
  ) {}

  ngOnInit(): void {
    this.voiceNav.registerPageCommands('quest-home', (cmd: string) => {
      if (cmd.includes('story') || cmd.includes('histoire')) {
        this.voiceNav.feedback$.next('Story Mode...');
        this.router.navigate(['/terminal-quest/story']);
        return true;
      }
      if (cmd.includes('survival') || cmd.includes('survie')) {
        this.voiceNav.feedback$.next('Survival Mode...');
        this.router.navigate(['/terminal-quest/survival']);
        return true;
      }
      if (cmd.includes('skill') || cmd.includes('rapport') || cmd.includes('report')) {
        this.voiceNav.feedback$.next('Skill Report...');
        this.router.navigate(['/terminal-quest/skill-report']);
        return true;
      }
      if (cmd.includes('leaderboard') || cmd.includes('classement')) {
        this.voiceNav.feedback$.next('Leaderboard...');
        this.router.navigate(['/terminal-quest/survival/leaderboard']);
        return true;
      }
      return false;
    });
    this.voiceNav.autoStart();
  }

  ngOnDestroy(): void {
    this.voiceNav.unregisterPageCommands('quest-home');
  }

  goToSkillReport(): void {
    this.router.navigate(['/terminal-quest/skill-report']);
  }
}
