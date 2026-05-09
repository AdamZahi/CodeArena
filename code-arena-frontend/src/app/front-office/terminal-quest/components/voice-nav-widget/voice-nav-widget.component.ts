import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { VoiceNavigationService } from '../../services/voice-navigation.service';

@Component({
  selector: 'app-voice-nav-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './voice-nav-widget.component.html',
  styleUrls: ['./voice-nav-widget.component.css']
})
export class VoiceNavWidgetComponent implements OnInit, OnDestroy {
  isListening = false;
  lastCommand = '';
  feedback = '';
  showPanel = false;

  private subs: Subscription[] = [];

  constructor(public readonly voiceNav: VoiceNavigationService) {}

  ngOnInit(): void {
    this.subs.push(
      this.voiceNav.isListening$.subscribe(v => { this.isListening = v; }),
      this.voiceNav.lastCommand$.subscribe(v => { this.lastCommand = v; }),
      this.voiceNav.feedback$.subscribe(v => {
        this.feedback = v;
        setTimeout(() => { if (this.feedback === v) this.feedback = ''; }, 3000);
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  toggleMic(): void  { this.voiceNav.toggle(); }
  togglePanel(): void { this.showPanel = !this.showPanel; }
}
