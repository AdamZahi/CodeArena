import { CommonModule } from '@angular/common';
import { Component, ViewChild, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ToastService } from '../shared/toast.service';

import { AuditLogTableComponent } from './audit-log-table.component';
import { BattleOpsToolbarComponent } from './battle-ops-toolbar.component';
import { BulkCancelComponent } from './bulk-cancel.component';
import { ExportPanelComponent } from './export-panel.component';
import { StuckRoomsAlertComponent } from './stuck-rooms-alert.component';

@Component({
  selector: 'app-battle-ops-page',
  standalone: true,
  imports: [
    CommonModule,
    BattleOpsToolbarComponent,
    StuckRoomsAlertComponent,
    BulkCancelComponent,
    ExportPanelComponent,
    AuditLogTableComponent
  ],
  template: `
    <app-battle-ops-toolbar
      (scanStuck)="scrollTo('stuck')"
      (bulkCancel)="scrollTo('bulk')"
      (exportData)="scrollTo('export')" />

    <section #stuck>
      <app-stuck-rooms-alert (forceEnd)="goToRoom($event)" />
    </section>

    <section class="grid two">
      <div #bulk>
        <app-bulk-cancel />
      </div>
      <div #exportPanel>
        <app-export-panel />
      </div>
    </section>

    <section>
      <app-audit-log-table />
    </section>
  `,
  styles: [`
    :host { display: flex; flex-direction: column; gap: 18px; }
    section { scroll-margin-top: 20px; }
    .grid.two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    @media (max-width: 1100px) { .grid.two { grid-template-columns: 1fr; } }
  `]
})
export class BattleOpsPageComponent {
  @ViewChild('stuck') stuckSection?: { nativeElement: HTMLElement };
  @ViewChild('bulk') bulkSection?: { nativeElement: HTMLElement };
  @ViewChild('exportPanel') exportSection?: { nativeElement: HTMLElement };

  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  scrollTo(section: 'stuck' | 'bulk' | 'export') {
    const el =
      section === 'stuck' ? this.stuckSection?.nativeElement
      : section === 'bulk' ? this.bulkSection?.nativeElement
      : this.exportSection?.nativeElement;
    el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  goToRoom(roomId: string) {
    this.toast.info(`Opening room ${roomId.slice(0, 8)}…`);
    this.router.navigate(['/admin/battles/rooms', roomId]);
  }
}
