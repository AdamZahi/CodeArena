import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-tq-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <div class="header">
        <span class="kicker">ADMIN — MODULE 8</span>
        <h1 class="title">TERMINAL QUEST <span class="accent">DASHBOARD</span></h1>
      </div>
      <div class="cards">
        <a routerLink="/admin/terminal-quest/chapters" class="card">
          <span class="card-icon">📖</span>
          <span class="card-label">CHAPTERS</span>
        </a>
        <div class="card placeholder">
          <span class="card-icon">📊</span>
          <span class="card-label">GLOBAL STATS</span>
          <span class="soon">coming soon</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Rajdhani:wght@500;700&display=swap');
    .page { font-family: 'Rajdhani', sans-serif; }
    .kicker { font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 4px; color: #06b6d4; }
    .title { font-family: 'Orbitron', monospace; font-size: 28px; font-weight: 900; color: #e2e8f0; margin: 8px 0 40px; }
    .accent { color: #8b5cf6; }
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
    .card { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 32px 20px; background: rgba(13,13,21,0.8); border: 1px solid #1a1a2e; border-radius: 8px; text-decoration: none; color: #94a3b8; transition: all 0.3s; cursor: pointer; }
    a.card:hover { border-color: rgba(139,92,246,0.4); color: #8b5cf6; box-shadow: 0 0 20px rgba(139,92,246,0.1); }
    .card-icon { font-size: 32px; }
    .card-label { font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 2px; }
    .soon { font-size: 10px; color: #475569; }
    .placeholder { cursor: default; }
  `]
})
export class TqDashboardComponent {}
