import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { EventService } from '../../services/event.service';
import QRCode from 'qrcode';
import * as L from 'leaflet';
import {
  EventCandidature,
  EventInvitation,
  EventRegistration,
  ProgrammingEvent
} from '../../models/event.model';
import { AuthService } from '@auth0/auth0-angular';

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
  map: any = null;

  isLoading = true;
  registering = false;
  error: string | null = null;
  private errorTimeoutId: number | null = null;

  successMsg = '';
  errorMsg = '';

  myRegistration: EventRegistration | null = null;
  waitlistPosition: number = 0;
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
  qrLoading = false;

  private subs = new Subscription();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly eventService: EventService,
    private readonly auth: AuthService
  ) { }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;
    this.eventId = id;
    const savedCandidature = sessionStorage.getItem(
      `candidature_${this.eventId}`
    );
    if (savedCandidature === 'PENDING') {
      this.exclusiveCandidature = { status: 'PENDING' } as any;
    }
    this.successMsg = '';
    this.isLoading = true;
    this.eventService.getEventById(id).subscribe({
      next: async (event) => {
        this.event = event;
        this.isLoading = false;
        this.initMap();
        this.startCountdown();
        await this.loadMyRegistration();

        // Load invitations for exclusive events
        const invSub = this.eventService.getMyInvitations().subscribe({
          next: (inv) => {
            this.myInvitation = inv.find((i) => i.eventId === id) ?? null;
            this.exclusiveInvitation = this.myInvitation;
            this.loadExclusiveState(id);
          },
          error: (err) => console.error('Failed to load invitations', err)
        });
        this.subs.add(invSub);
      },
      error: () => {
        this.isLoading = false;
        // Fallback to mock data if needed
        const mock = this.getMockEvents().find((e) => e.id === id) ?? null;
        if (mock) {
          this.event = mock;
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    if (this.errorTimeoutId != null) {
      window.clearTimeout(this.errorTimeoutId);
    }
    if (this.map) { this.map.remove(); this.map = null; }
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

  hasSavedPendingCandidature(): boolean {
    if (!this.eventId) return false;
    return sessionStorage.getItem(`candidature_${this.eventId}`) === 'PENDING';
  }

  hasConfirmedRegistration(): boolean {
    return this.myRegistration?.status === 'CONFIRMED';
  }

  hasWaitlistRegistration(): boolean {
    return this.myRegistration?.status === 'WAITLIST';
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
    const data = this.myRegistration?.qrCode || this.event?.id || 'CODEARENA';
    this.qrLoading = true;
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
    } finally {
      this.qrLoading = false;
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

  register(): void {
    if (this.registering) return;
    this.registering = true;
    this.errorMsg = '';
    this.eventService.register(this.event.id).subscribe({
      next: async () => {
        // Reload event to get updated currentParticipants
        this.eventService.getEventById(this.event.id).subscribe(updated => {
          this.event = updated;
        });
        // Reload registration status
        await this.loadMyRegistration();
        await this.generateQR();
        this.showQROverlay = true;
        this.successMsg = 'Registration confirmed!';
        this.registering = false;
      },
      error: (err) => {
        this.errorMsg = 'Registration failed. Please try again.';
        console.error('Register error:', err);
        this.registering = false;
      }
    });
  }

  cancelRegistration(): void {
    if (!this.eventId) return;
    this.eventService.cancelRegistration(this.eventId).subscribe({
      next: () => {
        this.myRegistration = null;
        this.qrCodeImageUrl = '';
        this.showQROverlay = false;
        this.successMsg = 'Registration cancelled.';
        this.eventService.getEventById(this.eventId!).subscribe(
          updated => this.event = updated
        );
      },
      error: (err) => {
        this.errorMsg = 'Cancel failed.';
      }
    });
  }

  submitCandidature(): void {
    if (!this.eventId) return;
    if (!this.event) return;
    if (this.exclusiveCandidature?.status === 'PENDING') return;
    if (
      this.hasSavedPendingCandidature() &&
      this.exclusiveCandidature?.status !== 'REJECTED'
    ) {
      return;
    }

    const motivation = this.motivationText.trim();
    if (!motivation) {
      this.setErrorTemporarily('MOTIVATION REQUIRED.');
      return;
    }

    this.error = null;
    this.eventService.submitCandidature(this.eventId, motivation).subscribe({
      next: (response) => {
        this.exclusiveCandidature = response;
        sessionStorage.setItem(`candidature_${this.eventId}`, 'PENDING');
        this.motivationText = '';
        this.error = null;
        this.successMsg = '⏳ YOUR CANDIDATURE IS UNDER REVIEW';
      },
      error: (err) => {
        if (err.status === 400) {
          sessionStorage.setItem(`candidature_${this.eventId}`, 'PENDING');
          this.exclusiveCandidature = { status: 'PENDING' } as any;
          this.motivationText = '';
          this.error = null;
          this.successMsg = '⏳ YOUR CANDIDATURE IS UNDER REVIEW';
        } else {
          this.setErrorTemporarily('Candidature submission failed.');
        }
      }
    });
  }



  async loadMyRegistration(): Promise<void> {
    return new Promise((resolve) => {
      this.eventService.getMyRegistrations().subscribe({
        next: async (registrations) => {
          console.log('REGISTRATIONS:', registrations);
          const myReg = registrations.find(r =>
            String(r.eventId) === String(this.eventId) ||
            String(r.eventId) === String(this.event?.id)
          );
          this.myRegistration = myReg || null;
          if (myReg && myReg.status === 'CONFIRMED') {
            await this.generateQR();
          }
          this.loadWaitlistPosition();
          resolve();
        },
        error: () => resolve()
      });
    });
  }

  loadWaitlistPosition(): void {
    if (this.myRegistration?.status !== 'WAITLIST') return;
    this.eventService.getParticipants(this.event.id).subscribe({
      next: (participants: any[]) => {
        const waitlist = participants.filter(p => p.status === 'WAITLIST');
        this.auth.user$.subscribe(user => {
          const userId = user?.sub;
          const myIndex = waitlist.findIndex(
            p => p.participantId === userId
          );
          this.waitlistPosition = myIndex !== -1 ? myIndex + 1 : 0;
        });
      },
      error: (err) => console.error('Waitlist error:', err)
    });
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
    if (this.event?.eventType !== 'EXCLUSIVE' &&
      this.event?.type !== 'EXCLUSIVE') return;

    if (this.exclusiveInvitation) {
      if (this.exclusiveInvitation.status === 'PENDING') {
        this.exclusiveCandidature = null;
      }
      return;
    }

    this.exclusiveLoading = false;
    const saved = sessionStorage.getItem(`candidature_${eventId}`);
    if (saved === 'PENDING') {
      this.exclusiveCandidature = { status: 'PENDING' } as any;
    } else {
      this.exclusiveCandidature = null;
    }
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

  initMap(): void {
    if (!this.event?.location) return;
    const location = this.event.location;
    
    setTimeout(() => {
      const mapEl = document.getElementById('event-map');
      if (!mapEl) return;
      
      if (this.map) { 
        this.map.remove(); 
        this.map = null; 
      }

      fetch(`https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(location)}&format=json&limit=1`)
        .then(res => res.json())
        .then(data => {
          const lat = data?.[0] ? parseFloat(data[0].lat) : 36.8;
          const lon = data?.[0] ? parseFloat(data[0].lon) : 10.1;
          
          this.map = L.map('event-map').setView([lat, lon], 13);
          
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap'
          }).addTo(this.map);

          const icon = L.icon({
            iconUrl: 'assets/marker-icon.png',
            iconRetinaUrl: 'assets/marker-icon-2x.png',
            shadowUrl: 'assets/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41]
          });

          L.marker([lat, lon], { icon })
            .addTo(this.map)
            .bindPopup(`📍 ${location}`)
            .openPopup();

          setTimeout(() => {
            this.map.invalidateSize();
          }, 200);
        })
        .catch(err => {
          console.error('Map error:', err);
          // Show default Tunisia map if location not found
          this.map = L.map('event-map').setView([36.8, 10.1], 8);
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap'
          }).addTo(this.map);
        });
    }, 500);
  }
}
