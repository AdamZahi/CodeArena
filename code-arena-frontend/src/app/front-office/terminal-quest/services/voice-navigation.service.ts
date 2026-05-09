import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class VoiceNavigationService {

  private recognition: any = null;
  private isListening = false;
  private paused = false;
  private currentPage = '';
  private pageCommands = new Map<string, (command: string) => boolean>();
  private lastProcessedCommand = '';

  readonly isListening$ = new BehaviorSubject<boolean>(false);
  readonly lastCommand$ = new BehaviorSubject<string>('');
  readonly feedback$    = new BehaviorSubject<string>('');

  constructor(private readonly router: Router) {}

  init(): void {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) {
      console.warn('[voice-nav] Web Speech API not supported');
      return;
    }

    this.recognition = new SR();
    this.recognition.continuous      = true;
    this.recognition.interimResults  = true;
    this.recognition.maxAlternatives = 1;
    this.recognition.lang            = 'en-US';

    const knownCommands = [
      'story mode', 'survival mode', 'skill report',
      'play', 'hint', 'back', 'home', 'stop', 'quit',
      'chapter 1', 'chapter 2', 'chapter 3', 'chapter 4',
      'mission 1', 'mission 2', 'mission 3', 'mission 4'
    ];

    this.recognition.onresult = (event: any) => {
      const last = event.results[event.results.length - 1];
      const transcript = last[0].transcript.trim().toLowerCase();

      if (last.isFinal) {
        if (transcript !== this.lastProcessedCommand) {
          console.log('[voice-nav] heard:', transcript);
          this.lastCommand$.next(transcript);
          this.processCommand(transcript);
        }
        this.lastProcessedCommand = '';
      } else {
        for (const cmd of knownCommands) {
          if (transcript.includes(cmd)) {
            this.processCommand(cmd);
            this.lastProcessedCommand = cmd;
            return;
          }
        }
      }
    };

    this.recognition.onerror = (event: any) => {
      console.warn('[voice-nav] error:', event.error);
      if ((event.error === 'no-speech' || event.error === 'aborted') && this.isListening) {
        try { this.recognition.start(); } catch (_) {}
      }
    };

    this.recognition.onend = () => {
      if (this.isListening) {
        try { this.recognition.start(); } catch (_) {}
      }
    };
  }

  startListening(): void {
    if (!this.recognition) this.init();
    if (!this.recognition) return;
    try {
      this.recognition.start();
      this.isListening = true;
      this.isListening$.next(true);
      this.feedback$.next('Listening...');
    } catch (_) {}
  }

  stopListening(): void {
    if (!this.recognition) return;
    this.isListening = false;
    this.isListening$.next(false);
    try { this.recognition.stop(); } catch (_) {}
    this.feedback$.next('Mic off');
  }

  autoStart(): void {
    if (!this.isListening) {
      this.startListening();
    }
  }

  toggle(): void {
    this.isListening ? this.stopListening() : this.startListening();
  }

  pause(): void  { this.paused = true; }
  resume(): void { this.paused = false; }

  registerPageCommands(page: string, handler: (command: string) => boolean): void {
    this.currentPage = page;
    this.pageCommands.set(page, handler);
  }

  unregisterPageCommands(page: string): void {
    this.pageCommands.delete(page);
    if (this.currentPage === page) this.currentPage = '';
  }

  private processCommand(command: string): void {
    if (this.paused) return;
    if (this.handleGlobalCommand(command)) return;

    const handler = this.pageCommands.get(this.currentPage);
    if (handler && handler(command)) return;

    this.feedback$.next(`"${command}" — not recognized`);
  }

  private handleGlobalCommand(command: string): boolean {
    if (command.includes('terminal quest') || command === 'home') {
      this.feedback$.next('Going to Terminal Quest...');
      this.router.navigate(['/terminal-quest']);
      return true;
    }
    if (command === 'go back' || command === 'back' || command === 'retour') {
      this.feedback$.next('Going back...');
      window.history.back();
      return true;
    }
    if (command.includes('stop listening') || command === 'stop' || command === 'arrête') {
      this.stopListening();
      return true;
    }
    return false;
  }
}
