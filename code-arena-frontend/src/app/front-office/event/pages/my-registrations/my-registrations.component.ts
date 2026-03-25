import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import QRCode from 'qrcode';
import { EventRegistration, ProgrammingEvent } from '../../models/event.model';
import { EventService } from '../../services/event.service';

type RegistrationCard = {
  registration: EventRegistration;
  event: ProgrammingEvent | null;
  qrImageUrl: string;
};

@Component({
  selector: 'app-my-registrations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './my-registrations.component.html',
  styleUrl: './my-registrations.component.css'
})
export class MyRegistrationsComponent implements OnInit, OnDestroy {
  isLoading = true;
  cards: RegistrationCard[] = [];
  errorMessage: string | null = null;
  showQROverlay = false;
  activeQRImageUrl = '';
  activeEventTitle = '';

  private subs = new Subscription();

  constructor(
    private readonly eventService: EventService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadRegistrations();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  goBack(): void {
    this.router.navigate(['/events']);
  }

  viewEvent(eventId: string): void {
    this.router.navigate(['/events', eventId]);
  }

  openQR(card: RegistrationCard): void {
    this.activeQRImageUrl = card.qrImageUrl;
    this.activeEventTitle = card.event?.title || 'CodeArena Event';
    this.showQROverlay = true;
  }

  downloadQR(): void {
    const link = document.createElement('a');
    link.download = 'codearena-event-qr.png';
    link.href = this.activeQRImageUrl;
    link.click();
  }

  private loadRegistrations(): void {
    this.isLoading = true;
    this.errorMessage = null;

    const sub = this.eventService.getMyRegistrations().subscribe({
      next: (regs) => {
        if (!regs.length) {
          this.cards = [];
          this.isLoading = false;
          return;
        }

        const requests = regs.map((registration) =>
          this.eventService.getEventById(registration.eventId).pipe(
            map((event) => ({ registration, event })),
            catchError(() => of({ registration, event: null }))
          )
        );

        const joinSub = forkJoin(requests).subscribe({
          next: async (baseCards) => {
            const cards: RegistrationCard[] = [];
            for (const c of baseCards) {
              cards.push({
                ...c,
                qrImageUrl:
                  c.registration.status === 'CONFIRMED' && c.registration.qrCode
                    ? await this.toQR(c.registration.qrCode)
                    : ''
              });
            }
            this.cards = cards;
            this.isLoading = false;
          },
          error: () => {
            this.errorMessage = 'Failed to load registration details.';
            this.isLoading = false;
          }
        });
        this.subs.add(joinSub);
      },
      error: () => {
        this.errorMessage = 'Failed to load registrations.';
        this.isLoading = false;
      }
    });
    this.subs.add(sub);
  }

  private async toQR(value: string): Promise<string> {
    try {
      return await QRCode.toDataURL(value, {
        width: 250,
        margin: 2,
        color: { dark: '#8b5cf6', light: '#0d0d15' }
      });
    } catch {
      return '';
    }
  }
}
