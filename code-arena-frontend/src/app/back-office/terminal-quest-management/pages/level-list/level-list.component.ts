import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-level-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <span class="kicker">TERMINAL QUEST</span>
          <h1 class="title">LEVELS</h1>
        </div>
        <div class="header-actions">
          <a routerLink="/admin/terminal-quest/chapters" class="back">← CHAPTERS</a>
        </div>
      </div>
      <div class="placeholder">
        <span class="icon">🎯</span>
        <p>Level list — coming soon</p>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Rajdhani:wght@500;700&display=swap');
    .page { font-family: 'Rajdhani', sans-serif; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; }
    .kicker { font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 4px; color: #06b6d4; }
    .title { font-family: 'Orbitron', monospace; font-size: 28px; font-weight: 900; color: #e2e8f0; margin: 4px 0 0; }
    .placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 16px; border: 1px solid #1a1a2e; border-radius: 8px; color: #64748b; font-size: 16px; }
    .icon { font-size: 48px; }
    .back { font-family: 'Orbitron', monospace; font-size: 11px; color: #8b5cf6; text-decoration: none; letter-spacing: 1px; }
    .back:hover { text-decoration: underline; }
    .header-actions { display: flex; gap: 16px; align-items: center; }
  `]
})
export class LevelListComponent {}
