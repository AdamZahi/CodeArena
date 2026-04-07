import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BattleService } from '../../services/battle.service';
import { SharedResultDTO } from '../../models/battle-room.model';

@Component({
  selector: 'app-battle-share',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './battle-share.component.html',
  styleUrls: ['./battle-share.component.css'],
})
export class BattleShareComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly battleService = inject(BattleService);

  result: SharedResultDTO | null = null;
  loading = true;
  notFound = false;

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token') ?? '';
    if (!token) {
      this.notFound = true;
      this.loading = false;
      return;
    }
    this.battleService.getSharedResult(token).subscribe({
      next: (res) => {
        this.result = res;
        this.loading = false;
      },
      error: () => {
        this.notFound = true;
        this.loading = false;
      },
    });
  }

  formatDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }
}
