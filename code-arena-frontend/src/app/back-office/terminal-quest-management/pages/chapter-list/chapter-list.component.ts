import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-chapter-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <span class="kicker">TERMINAL QUEST</span>
          <h1 class="title">CHAPTERS</h1>
        </div>
        <a routerLink="/admin/terminal-quest/chapters/new" class="btn">+ NEW CHAPTER</a>
      </div>
      <div class="placeholder">
        <span class="icon">📖</span>
        <p>Chapter list — coming soon</p>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Rajdhani:wght@500;700&display=swap');
    .page { font-family: 'Rajdhani', sans-serif; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; }
    .kicker { font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 4px; color: #06b6d4; }
    .title { font-family: 'Orbitron', monospace; font-size: 28px; font-weight: 900; color: #e2e8f0; margin: 4px 0 0; }
    .btn { padding: 10px 24px; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.5px; background: linear-gradient(135deg, #8b5cf6, #7c3aed); color: #fff; text-decoration: none; border-radius: 4px; clip-path: polygon(10px 0%,100% 0%,calc(100% - 10px) 100%,0% 100%); }
    .placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 16px; border: 1px solid #1a1a2e; border-radius: 8px; color: #64748b; font-size: 16px; }
    .icon { font-size: 48px; }
  `]
})
export class ChapterListComponent {}
