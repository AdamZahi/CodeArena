import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-quest-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <div class="hero">
        <span class="kicker">MODULE 8</span>
        <h1 class="title">TERMINAL<span class="accent"> QUEST</span></h1>
        <p class="sub">Master the command line. Conquer the terminal.</p>
        <div class="actions">
          <a routerLink="/terminal-quest/story" class="btn primary">STORY MODE</a>
          <a routerLink="/terminal-quest/survival" class="btn secondary">SURVIVAL MODE</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Rajdhani:wght@500;600;700&display=swap');
    .page { display: flex; align-items: center; justify-content: center; min-height: 60vh; font-family: 'Rajdhani', sans-serif; }
    .hero { text-align: center; }
    .kicker { font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 4px; color: #06b6d4; }
    .title { font-family: 'Orbitron', monospace; font-size: 48px; font-weight: 900; color: #e2e8f0; margin: 12px 0; letter-spacing: 4px; }
    .accent { color: #8b5cf6; text-shadow: 0 0 20px rgba(139,92,246,0.5); }
    .sub { color: #64748b; font-size: 18px; margin-bottom: 40px; }
    .actions { display: flex; gap: 20px; justify-content: center; }
    .btn { padding: 14px 32px; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; font-weight: 700; text-decoration: none; border-radius: 4px; transition: all 0.3s; clip-path: polygon(12px 0%,100% 0%,calc(100% - 12px) 100%,0% 100%); }
    .primary { background: linear-gradient(135deg, #8b5cf6, #7c3aed); color: #fff; }
    .primary:hover { box-shadow: 0 0 30px rgba(139,92,246,0.5); transform: translateY(-2px); }
    .secondary { background: transparent; border: 1px solid #06b6d4; color: #06b6d4; }
    .secondary:hover { background: rgba(6,182,212,0.1); box-shadow: 0 0 20px rgba(6,182,212,0.3); }
  `]
})
export class QuestHomeComponent {}
