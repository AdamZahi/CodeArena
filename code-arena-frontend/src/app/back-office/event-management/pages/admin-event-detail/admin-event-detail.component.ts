import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '@auth0/auth0-angular';
import {
  EventCandidature,
  EventInvitation,
  EventRegistration,
  ProgrammingEvent
} from '../../../../front-office/event/models/event.model';
import { EventService } from '../../../../front-office/event/services/event.service';
import { AdminUserService, AdminUser } from '../../../user-management/services/admin-user.service';

@Component({
  selector: 'app-admin-event-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-event-detail.component.html',
  styleUrl: './admin-event-detail.component.css'
})
export class AdminEventDetailComponent implements OnInit, OnDestroy {
  event: ProgrammingEvent | null = null;
  participants: EventRegistration[] = [];
  candidatures: EventCandidature[] = [];
  invitations: EventInvitation[] = [];
  eventId = '';
  message: string | null = null;
  error: string | null = null;

  totalInvitations: number = 0;
  pendingInvitations: number = 0;
  acceptedInvitations: number = 0;
  declinedInvitations: number = 0;

  totalCandidatures: number = 0;
  pendingCandidatures: number = 0;
  acceptedCandidatures: number = 0;
  rejectedCandidatures: number = 0;

  private subs = new Subscription();
  userMap: { [auth0Id: string]: AdminUser } = {};
  loggedInUser: any = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly eventService: EventService,
    private readonly adminUserService: AdminUserService,
    private readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    const authSub = this.auth.user$.subscribe(user => {
      this.loggedInUser = user;
    });
    this.subs.add(authSub);

    const sub = this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (!id) return;
      this.eventId = id;
      this.loadAll();
    });
    this.subs.add(sub);
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  goBack(): void {
    this.router.navigate(['/admin/events']);
  }

  acceptCandidature(id: string): void {
    const sub = this.eventService.acceptCandidature(id).subscribe({
      next: () => {
        this.message = 'Candidature accepted.';
        this.loadAll();
      },
      error: () => (this.error = 'Failed to accept candidature.')
    });
    this.subs.add(sub);
  }

  rejectCandidature(id: string): void {
    const sub = this.eventService.rejectCandidature(id).subscribe({
      next: () => {
        this.message = 'Candidature rejected.';
        this.loadAll();
      },
      error: () => (this.error = 'Failed to reject candidature.')
    });
    this.subs.add(sub);
  }

  resolveParticipantLabel(participantName?: string, participantId?: string): string {
    const safeId = (participantId ?? '').trim();

    // 1. If it's the current user, get name directly from Auth0 SDK (most up to date)
    if (this.loggedInUser && safeId && this.loggedInUser.sub === safeId) {
      const authName = this.loggedInUser.nickname || this.loggedInUser.name;
      if (authName && !this.looksLikeTechnicalIdentifier(authName)) {
        return authName;
      }
    }

    // 2. Fetch from backend user map (for other users)
    if (safeId && this.userMap[safeId]) {
      const u = this.userMap[safeId];
      
      // Try Nickname first
      if (u.nickname && !this.looksLikeTechnicalIdentifier(u.nickname)) {
        return u.nickname;
      }

      // Try Firstname + Lastname
      if (u.firstName || u.lastName) {
        const full = [u.firstName, u.lastName].filter(x => !!x).join(' ').trim();
        if (full && !this.looksLikeTechnicalIdentifier(full)) {
          return full;
        }
      }

      // Try Email prefix
      if (u.email) {
        const prefix = u.email.split('@')[0];
        if (prefix && !this.looksLikeTechnicalIdentifier(prefix)) {
          return prefix;
        }
      }
    }

    // 3. Fallback to ParticipantName provided by event service
    const safeName = (participantName ?? '').trim();
    if (safeName && !this.looksLikeTechnicalIdentifier(safeName)) {
      return safeName;
    }

    // 4. If we still have a technical name but found a user in the map with ANY info, try one last time
    if (this.userMap[safeId]) {
       const u = this.userMap[safeId];
       if (u.email) return u.email.split('@')[0];
    }

    if (safeId && !this.looksLikeTechnicalIdentifier(safeId)) {
      return safeId;
    }

    // 5. Default dynamic user_xxx format for unmapped users
    if (safeId) {
       const parts = safeId.split('|');
       const idPart = parts.length > 1 ? parts[1] : parts[0];
       if (idPart.length > 8) {
         return 'user_' + idPart.substring(idPart.length - 8);
       }
       return 'user_' + idPart;
    }

    return 'Unknown Hacker';
  }

  inviteTop10(): void {
    const sub = this.eventService.inviteTop10(this.eventId).subscribe({
      next: () => (this.message = '10 invitations sent!'),
      error: () => (this.error = 'Failed to send invitations.')
    });
    this.subs.add(sub);
  }

  private loadAll(): void {
    this.message = null;
    this.error = null;

    const eventSub = this.eventService.getEventById(this.eventId).subscribe({
      next: (ev) => (this.event = ev),
      error: () => (this.error = 'Failed to load event.')
    });
    this.subs.add(eventSub);

    const participantSub = this.eventService.getEventParticipants(this.eventId).subscribe({
      next: (p) => (this.participants = p),
      error: () => (this.participants = [])
    });
    this.subs.add(participantSub);

    const candidatureSub = this.eventService.getCandidaturesByEvent(this.eventId).subscribe({
      next: (c) => {
        this.candidatures = c;
        this.totalCandidatures = this.candidatures.length;
        this.pendingCandidatures = this.candidatures.filter((c) => c.status === 'PENDING').length;
        this.acceptedCandidatures = this.candidatures.filter((c) => c.status === 'ACCEPTED').length;
        this.rejectedCandidatures = this.candidatures.filter((c) => c.status === 'REJECTED').length;
      },
      error: () => (this.candidatures = [])
    });
    this.subs.add(candidatureSub);

    const inviteSub = this.eventService.getMyInvitations().subscribe({
      next: (inv) => {
        this.invitations = inv.filter((i) => i.eventId === this.eventId);
        this.totalInvitations = this.invitations.length;
        this.pendingInvitations = this.invitations.filter((i) => i.status === 'PENDING').length;
        this.acceptedInvitations = this.invitations.filter((i) => i.status === 'ACCEPTED').length;
        this.declinedInvitations = this.invitations.filter((i) => i.status === 'DECLINED').length;
      },
      error: () => (this.invitations = [])
    });
    this.subs.add(inviteSub);

    const userSub = this.adminUserService.listUsers().subscribe({
      next: (res) => {
        const users = res.content || [];
        users.forEach((u) => {
          if (u.auth0Id) {
            this.userMap[u.auth0Id] = u;
          }
          if (u.id) {
            this.userMap[u.id] = u;
          }
        });
        // Force refresh GUI
        this.participants = [...this.participants];
        this.candidatures = [...this.candidatures];
        this.invitations = [...this.invitations];
      },
      error: () => console.warn('Failed to load users for mapping.')
    });
    this.subs.add(userSub);
  }

  private looksLikeTechnicalIdentifier(value: string): boolean {
    const lower = value.toLowerCase();
    const compact = lower.replace(/[\s_-]/g, '');
    return (
      lower.startsWith('auth0|') ||
      lower.startsWith('google-oauth2|') ||
      lower.startsWith('github|') ||
      lower.startsWith('facebook|') ||
      /^\d{12,}$/.test(compact)
    );
  }
}
