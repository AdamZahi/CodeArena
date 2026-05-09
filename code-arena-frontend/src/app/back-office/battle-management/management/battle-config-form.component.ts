import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BattleConfigDTO } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';

const RANK_OPTIONS = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND', 'MASTER'];

@Component({
  selector: 'app-battle-config-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card form">
      <header>
        <h3>BATTLE CONFIGURATION</h3>
        @if (config()?.updatedAt) {
          <span class="updated">Last updated {{ config()!.updatedAt | date:'medium' }} by {{ config()!.updatedBy }}</span>
        }
      </header>

      @if (loading()) {
        <div class="skeleton"></div>
      } @else if (form) {
        <div class="grid">
          <label>
            <span>MAX PARTICIPANTS</span>
            <input type="number" min="2" [(ngModel)]="form.maxParticipants" (ngModelChange)="markDirty()" />
          </label>
          <label>
            <span>TIME LIMIT (minutes)</span>
            <input type="number" min="1" [(ngModel)]="form.timeLimitMinutes" (ngModelChange)="markDirty()" />
          </label>
          <label>
            <span>XP REWARD — WINNER</span>
            <input type="number" min="0" [(ngModel)]="form.xpRewardWinner" (ngModelChange)="markDirty()" />
          </label>
          <label>
            <span>XP REWARD — LOSER</span>
            <input type="number" min="0" [(ngModel)]="form.xpRewardLoser" (ngModelChange)="markDirty()" />
          </label>
          <label>
            <span>AUTO-CLOSE ABANDONED AFTER (minutes)</span>
            <input type="number" min="1" [(ngModel)]="form.autoCloseAbandonedAfterMinutes" (ngModelChange)="markDirty()" />
          </label>
          <label>
            <span>MIN RANK REQUIRED</span>
            <select [(ngModel)]="form.minRankRequired" (ngModelChange)="markDirty()">
              <option [ngValue]="null">— None —</option>
              @for (r of ranks; track r) { <option [ngValue]="r">{{ r }}</option> }
            </select>
          </label>
          <label class="toggle full">
            <input type="checkbox" [(ngModel)]="form.allowSpectators" (ngModelChange)="markDirty()" />
            <span>ALLOW SPECTATORS</span>
          </label>

          <label class="full">
            <span>ALLOWED LANGUAGES</span>
            <div class="tag-input">
              @for (lang of form.allowedLanguages; track lang) {
                <span class="tag">{{ lang }} <button type="button" (click)="removeLanguage(lang)">×</button></span>
              }
              <input type="text" [(ngModel)]="newLanguage" (keydown.enter)="addLanguage(); $event.preventDefault()" placeholder="add language…" />
            </div>
          </label>
        </div>

        <footer>
          <span class="dirty-flag" [class.visible]="dirty()">UNSAVED CHANGES</span>
          <button class="btn ghost" (click)="reset()" [disabled]="!dirty()">DISCARD</button>
          <button class="btn primary" (click)="save()" [disabled]="!dirty() || saving()">
            {{ saving() ? 'SAVING…' : 'SAVE CONFIG' }}
          </button>
        </footer>
      }
    </div>
  `,
  styles: [`
    .card.form {
      background: rgba(13,13,21,0.7); border: 1px solid #1a1a2e; border-radius: 6px; padding: 22px;
    }
    header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 18px; }
    h3 { margin: 0; font-family: 'Orbitron', monospace; font-size: 12px; letter-spacing: 2px; color: #94a3b8; }
    .updated { font-size: 12px; color: #64748b; }

    .grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px;
    }
    label.full { grid-column: 1 / -1; }
    label {
      display: flex; flex-direction: column; gap: 6px;
      font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px; color: #64748b;
    }
    input[type="number"], input[type="text"], select {
      background: #0a0a0f; border: 1px solid #1a1a2e; color: #e2e8f0;
      padding: 8px 10px; font-family: 'Rajdhani', sans-serif; font-size: 14px;
      border-radius: 3px; color-scheme: dark;
    }
    label.toggle { flex-direction: row; align-items: center; gap: 10px; }

    .tag-input {
      display: flex; flex-wrap: wrap; gap: 6px;
      padding: 8px; background: #0a0a0f; border: 1px solid #1a1a2e; border-radius: 3px;
    }
    .tag {
      background: rgba(139,92,246,0.1); color: #8b5cf6;
      border: 1px solid rgba(139,92,246,0.3);
      padding: 3px 8px; border-radius: 2px; font-size: 12px;
      display: inline-flex; align-items: center; gap: 4px;
    }
    .tag button {
      background: transparent; border: none; color: #8b5cf6; cursor: pointer; font-size: 14px; padding: 0; line-height: 1;
    }
    .tag-input input { flex: 1; min-width: 100px; border: none; background: transparent; padding: 4px; }

    footer {
      display: flex; align-items: center; gap: 12px; margin-top: 22px; padding-top: 18px;
      border-top: 1px solid #1a1a2e;
    }
    .dirty-flag {
      flex: 1; font-family: 'Orbitron', monospace; font-size: 10px; letter-spacing: 1.5px;
      color: #f59e0b; visibility: hidden;
    }
    .dirty-flag.visible { visibility: visible; }
    .btn {
      padding: 8px 16px; font-family: 'Orbitron', monospace; font-size: 11px; letter-spacing: 1.5px;
      cursor: pointer; border-radius: 3px; border: 1px solid;
    }
    .btn.ghost { background: transparent; border-color: #1a1a2e; color: #94a3b8; }
    .btn.ghost:hover:not(:disabled) { color: #ef4444; border-color: #ef4444; }
    .btn.primary { background: rgba(139,92,246,0.15); border-color: #8b5cf6; color: #8b5cf6; }
    .btn.primary:hover:not(:disabled) { background: rgba(139,92,246,0.25); }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }

    .skeleton { height: 320px; background: linear-gradient(90deg, #0f0f1a, #1a1a2e, #0f0f1a); background-size: 200% 100%; animation: pulse 1.5s ease-in-out infinite; border-radius: 4px; }
    @keyframes pulse { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `]
})
export class BattleConfigFormComponent implements OnInit {
  private readonly api = inject(BattleAdminService);
  private readonly toast = inject(ToastService);

  readonly ranks = RANK_OPTIONS;
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly dirty = signal(false);
  readonly config = signal<BattleConfigDTO | null>(null);

  form: BattleConfigDTO | null = null;
  newLanguage = '';

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.api.getConfig().subscribe({
      next: (cfg) => {
        this.config.set(cfg);
        this.form = { ...cfg, allowedLanguages: [...cfg.allowedLanguages] };
        this.dirty.set(false);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.error('Failed to load config: ' + (e?.error?.message ?? ''));
      }
    });
  }

  markDirty() { this.dirty.set(true); }

  addLanguage() {
    const lang = this.newLanguage.trim().toLowerCase();
    if (!lang || !this.form) return;
    if (!this.form.allowedLanguages.includes(lang)) {
      this.form.allowedLanguages = [...this.form.allowedLanguages, lang];
      this.markDirty();
    }
    this.newLanguage = '';
  }

  removeLanguage(lang: string) {
    if (!this.form) return;
    this.form.allowedLanguages = this.form.allowedLanguages.filter((l) => l !== lang);
    this.markDirty();
  }

  reset() {
    const cfg = this.config();
    if (!cfg) return;
    this.form = { ...cfg, allowedLanguages: [...cfg.allowedLanguages] };
    this.dirty.set(false);
  }

  save() {
    if (!this.form) return;
    const before = this.config();
    const diff = this.formatDiff(before, this.form);
    const ok = confirm(`Save these changes?\n\n${diff || 'No detectable changes.'}`);
    if (!ok) return;
    this.saving.set(true);
    this.api.updateConfig(this.form).subscribe({
      next: (cfg) => {
        this.config.set(cfg);
        this.form = { ...cfg, allowedLanguages: [...cfg.allowedLanguages] };
        this.dirty.set(false);
        this.saving.set(false);
        this.toast.success('Battle configuration saved');
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.error('Save failed: ' + (e?.error?.message ?? ''));
      }
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  onUnload(e: BeforeUnloadEvent) {
    if (this.dirty()) {
      e.preventDefault();
      e.returnValue = '';
    }
  }

  private formatDiff(before: BattleConfigDTO | null, after: BattleConfigDTO): string {
    if (!before) return '';
    const out: string[] = [];
    const compare = (key: keyof BattleConfigDTO) => {
      const b = JSON.stringify((before as any)[key]);
      const a = JSON.stringify((after as any)[key]);
      if (b !== a) out.push(`• ${key}: ${b} → ${a}`);
    };
    (Object.keys(after) as (keyof BattleConfigDTO)[])
      .filter((k) => k !== 'updatedAt' && k !== 'updatedBy')
      .forEach(compare);
    return out.join('\n');
  }
}
