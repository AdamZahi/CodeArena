import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-chapter-map',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <div class="header">
        <span class="kicker">STORY MODE</span>
        <h1 class="title">CHAPTER <span class="accent">MAP</span></h1>
      </div>
      <div class="placeholder">
        <span class="icon">🗺</span>
        <p>Chapter selection — coming soon</p>
        <a routerLink="/terminal-quest" class="back">← BACK TO HOME</a>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Rajdhani:wght@500;700&display=swap');
    .page { padding: 40px; font-family: 'Rajdhani', sans-serif; }
    .kicker { font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 4px; color: #06b6d4; }
    .title { font-family: 'Orbitron', monospace; font-size: 32px; font-weight: 900; color: #e2e8f0; margin: 8px 0 40px; }
    .accent { color: #8b5cf6; }
    .placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 16px; border: 1px solid #1a1a2e; border-radius: 8px; color: #64748b; font-size: 16px; }
    .icon { font-size: 48px; }
    .back { font-family: 'Orbitron', monospace; font-size: 11px; color: #8b5cf6; text-decoration: none; letter-spacing: 1px; }
    .back:hover { text-decoration: underline; }
  `]
})
export class ChapterMapComponent {}
