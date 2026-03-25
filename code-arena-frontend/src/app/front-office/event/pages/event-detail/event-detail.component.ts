import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { EventService } from '../../services/event.service';
import QRCode from 'qrcode';
import {
  EventCandidature,
  EventInvitation,
  EventRegistration,
  ProgrammingEvent
} from '../../models/event.model';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './event-detail.component.html',
  styleUrl: './event-detail.component.css'
})
export class EventDetailComponent implements OnInit, OnDestroy {
  event: any = null;
  eventId: string | null = null;

  isLoading = true;
  error: string | null = null;
  private errorTimeoutId: number | null = null;

  successMsg = '';
  errorMsg = '';

  myRegistration: EventRegistration | null = null;
  myInvitation: EventInvitation | null = null;
  motivationText = '';

  exclusiveInvitation: EventInvitation | null = null;
  exclusiveCandidature: EventCandidature | null = null;
  exclusiveLoading = false;

  countdownDisplay = '';
  countdownEnded = false;
  private countdownInterval: any;

  showQROverlay = false;
  qrCodeImageUrl = '';

  private subs = new Subscription();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly eventService: EventService
  ) { }

  ngOnInit(): void {
    const sub = this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (!id) return;
      this.eventId = id;
      this.loadAll(id);
    });
    this.subs.add(sub);
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    if (this.errorTimeoutId != null) {
      window.clearTimeout(this.errorTimeoutId);
    }
    this.subs.unsubscribe();
  }

  goBack(): void {
    this.router.navigate(['/events']);
  }

  isOpenEvent(): boolean {
    return this.event?.type === 'OPEN' || this.event?.eventType === 'OPEN';
  }

  isExclusiveEvent(): boolean {
    return this.event?.type === 'EXCLUSIVE' || this.event?.eventType === 'EXCLUSIVE';
  }

  hasConfirmedRegistration(): boolean {
    return this.myRegistration?.status === 'CONFIRMED';
  }

  hasWaitlistRegistration(): boolean {
    return this.myRegistration?.status === 'WAITLIST';
  }

  getWaitlistPosition(): number {
    // Backend does not expose an explicit waitlist rank.
    // We use a reasonable derived position from occupancy.
    const max = this.event?.maxParticipants ?? 0;
    const cur = this.event?.currentParticipants ?? 0;
    if (max <= 0) return 1;
    return cur + 1;
  }

  formatDateTime(isoLike: string): string {
    const date = new Date(isoLike);
    if (Number.isNaN(date.getTime())) return isoLike;
    return date.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  acceptInvitation(): void {
    if (!this.eventId) return;
    this.error = null;
    const sub = this.eventService.acceptInvitation(this.eventId).subscribe({
      next: () => {
        this.refreshEventAndUserState();
      },
      error: (err) => {
        console.error('Failed to accept invitation', err);
        this.setErrorTemporarily('INVITATION ACCEPT FAILED.');
      }
    });
    this.subs.add(sub);
  }

  declineInvitation(): void {
    if (!this.eventId) return;
    this.error = null;
    const sub = this.eventService.declineInvitation(this.eventId).subscribe({
      next: () => {
        this.refreshEventAndUserState();
      },
      error: (err) => {
        console.error('Failed to decline invitation', err);
        this.setErrorTemporarily('INVITATION DECLINE FAILED.');
      }
    });
    this.subs.add(sub);
  }

  async generateQR(): Promise<void> {
    const data = this.event?.qrCode || this.event?.id || 'CODEARENA';
    try {
      const QRCode = await import('qrcode');
      this.qrCodeImageUrl = await QRCode.toDataURL(String(data), {
        width: 200,
        margin: 2,
        color: { dark: '#8b5cf6', light: '#0d0d15' }
      });
    } catch (e) {
      console.error('QR failed', e);
      this.qrCodeImageUrl = '';
    }
  }

  downloadQR(): void {
    const link = document.createElement('a');
    link.download = 'codearena-qr.png';
    link.href = this.qrCodeImageUrl;
    link.click();
  }

  private setErrorTemporarily(message: string): void {
    this.error = message;
    if (this.errorTimeoutId != null) {
      window.clearTimeout(this.errorTimeoutId);
    }
    this.errorTimeoutId = window.setTimeout(() => {
      this.error = null;
      this.errorTimeoutId = null;
    }, 3000);
  }

  private getMockEvents(): ProgrammingEvent[] {
    return [
      {
        id: '1',
        title: 'HACKATHON 2025',
        description: 'Build fast.',
        organizerId: 'org-1',
        status: 'UPCOMING',
        startDate: '2026-06-01T09:00:00',
        endDate: '2026-06-03T18:00:00',
        maxParticipants: 50,
        currentParticipants: 12,
        type: 'OPEN',
        category: 'HACKATHON',
        createdAt: '2026-01-01T00:00:00',
        availablePlaces: 38,
        isFull: false,
        fillRate: 24
      },
      {
        id: '2',
        title: 'ELITE BOOTCAMP',
        description: 'Exclusive training.',
        organizerId: 'org-1',
        status: 'UPCOMING',
        startDate: '2026-07-10T09:00:00',
        endDate: '2026-07-12T18:00:00',
        maxParticipants: 20,
        currentParticipants: 20,
        type: 'EXCLUSIVE',
        category: 'BOOTCAMP',
        createdAt: '2026-01-01T00:00:00',
        availablePlaces: 0,
        isFull: true,
        fillRate: 100
      }
    ];
  }

  async joinEvent(): Promise<void> {
    if (!this.eventId) return;
    if (this.myRegistration && this.myRegistration.status !== 'CANCELLED') return;
    this.errorMsg = '';

    const sub = this.eventService.register(this.eventId).subscribe({
      next: async () => {
        this.loadMyRegistration();
        this.successMsg = 'Registration confirmed!';
        await this.generateQR();
        this.showQROverlay = true;
      },
      error: (err) => {
        console.error('Registration failed', err);
        this.setErrorTemporarily('Registration failed. Please try again.');
        this.errorMsg = 'Registration failed: ' + (err?.error?.message || err?.status || 'unknown error');
      }
    });
    this.subs.add(sub);
  }

  cancelRegistration(): void {
    this.eventService.cancelRegistration(this.event.id).subscribe({
      next: () => {
        this.successMsg = 'Registration cancelled.';
        this.errorMsg = '';
        this.qrCodeImageUrl = '';
        this.showQROverlay = false;
        this.myRegistration = null;
        this.eventService.getEventById(this.event.id).subscribe(updated => {
          this.event = updated;
        });
      },
      error: (err) => {
        console.error('Cancel error full:', err);
        this.errorMsg = 'Cancel failed: ' + (err?.error?.message || err?.status || 'unknown error');
      }
    });
  }

  submitCandidature(): void {
    if (!this.eventId) return;
    if (!this.event) return;
    if (this.exclusiveCandidature?.status === 'PENDING') return;

    const motivation = this.motivationText.trim();
    if (!motivation) {
      this.setErrorTemporarily('MOTIVATION REQUIRED.');
      return;
    }

    this.error = null;
    const sub = this.eventService.submitCandidature(this.eventId, motivation).subscribe({
      next: () => {
        this.motivationText = '';
        this.loadExclusiveState(this.eventId!);
      },
      error: (err) => {
        console.error('Candidature submit failed', err);
        this.setErrorTemporarily('Candidature failed');
      }
    });
    this.subs.add(sub);
  }

  private loadAll(id: string): void {
    this.isLoading = true;
    this.error = null;
    if (this.errorTimeoutId != null) {
      window.clearTimeout(this.errorTimeoutId);
      this.errorTimeoutId = null;
    }
    this.successMsg = '';
    this.event = null;
    this.myRegistration = null;
    this.myInvitation = null;
    this.motivationText = '';
    this.qrCodeImageUrl = '';
    this.showQROverlay = false;
    this.exclusiveInvitation = null;
    this.exclusiveCandidature = null;
    this.exclusiveLoading = false;

    this.startCountdown();

    const eventSub = this.eventService.getEventById(id).subscribe({
      next: (ev) => {
        this.event = ev;
      },
      error: (err) => {
        console.error('Failed to load event', err);
        // Fallback to mock data if backend is down
        const mock = this.getMockEvents().find((e) => e.id === id) ?? null;
        if (mock) {
          this.event = mock;
          this.isLoading = false;
        } else {
          this.setErrorTemporarily('FAILED TO LOAD EVENT.');
          this.isLoading = false;
        }
      },
      complete: () => {
        this.isLoading = false;
        this.startCountdown();
      }
    });
    this.subs.add(eventSub);

    this.loadMyRegistration();

    const invSub = this.eventService.getMyInvitations().subscribe({
      next: (inv) => {
        this.myInvitation = inv.find((i) => i.eventId === id) ?? null;
        this.exclusiveInvitation = this.myInvitation;
        this.loadExclusiveState(id);
      },
      error: (err) => {
        console.error('Failed to load invitations', err);
      }
    });
    this.subs.add(invSub);
  }

  loadMyRegistration(): void {
    if (!this.eventId) return;
    const sub = this.eventService.getParticipants(this.eventId).subscribe({
      next: async (participants: any[]) => {
        this.myRegistration = participants.find((p: any) => p.participantId === 'mock-player-1') || null;
        if (this.myRegistration?.status === 'CONFIRMED') {
          await this.generateQR();
        }
      },
      error: (err) => {
        console.error('Failed to load registrations', err);
      }
    });
    this.subs.add(sub);
  }

  private refreshEventAndUserState(): void {
    if (!this.eventId) return;

    const eventSub = this.eventService.getEventById(this.eventId).subscribe({
      next: (ev) => {
        this.event = ev;
      }
    });
    this.subs.add(eventSub);

    this.loadMyRegistration();

    const invSub = this.eventService.getMyInvitations().subscribe({
      next: (inv) => {
        this.myInvitation = inv.find((i) => i.eventId === this.eventId) ?? null;
        this.exclusiveInvitation = this.myInvitation;
        this.loadExclusiveState(this.eventId!);
      }
    });
    this.subs.add(invSub);
  }



  private loadExclusiveState(eventId: string): void {
    if (this.event?.eventType !== 'EXCLUSIVE' && this.event?.type !== 'EXCLUSIVE') return;

    // If invitation exists, candidature UI depends on invitation status.
    if (this.exclusiveInvitation) {
      if (this.exclusiveInvitation.status === 'PENDING') {
        this.exclusiveCandidature = null;
      }
      return;
    }

    this.exclusiveLoading = true;
    const sub = this.eventService.getCandidaturesByEvent(eventId).subscribe({
      next: (list) => {
        this.exclusiveCandidature =
          list.find((c) => c.participantId === 'mock-player-1') ?? null;
        this.exclusiveLoading = false;
      },
      error: (err) => {
        console.error('Failed to load candidatures', err);
        this.exclusiveCandidature = null;
        this.exclusiveLoading = false;
      }
    });
    this.subs.add(sub);
  }

  startCountdown(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    this.updateCountdown();
    this.countdownInterval = setInterval(() => this.updateCountdown(), 1000);
  }

  updateCountdown(): void {
    if (!this.event?.startDate) return;
    const now = new Date().getTime();
    const target = new Date(this.event.startDate).getTime();
    const diff = target - now;
    if (diff <= 0) {
      this.countdownDisplay = 'EVENT ENDED';
      this.countdownEnded = true;
      clearInterval(this.countdownInterval);
      return;
    }
    this.countdownEnded = false;
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);
    this.countdownDisplay =
      `${String(days).padStart(2,'0')}D : ${String(hours).padStart(2,'0')}H : ${String(minutes).padStart(2,'0')}M : ${String(seconds).padStart(2,'0')}S`;
  }

  getParticipantsPercent(): number {
    if (!this.event) return 0;
    const max = this.event.maxParticipants || 0;
    if (max <= 0) return 0;
    const pct = (this.event.currentParticipants / max) * 100;
    return Math.max(0, Math.min(100, pct));
  }

  getPlacesLeft(): number {
    if (!this.event) return 0;
    const max = this.event.maxParticipants || 0;
    const cur = this.event.currentParticipants || 0;
    return Math.max(0, max - cur);
  }

  getEventStatusClass(): string {
    const status = (this.event?.status || '').toUpperCase();
    if (status === 'ACTIVE') return 'event-status-active';
    if (status === 'COMPLETED') return 'event-status-completed';
    return 'event-status-upcoming';
  }

  getEventStatusLabel(): string {
    const status = (this.event?.status || '').toUpperCase();
    if (status === 'ACTIVE') return 'ACTIVE';
    if (status === 'COMPLETED') return 'COMPLETED';
    return 'UPCOMING';
  }
}
