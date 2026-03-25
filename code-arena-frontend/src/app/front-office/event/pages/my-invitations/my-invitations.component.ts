import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { EventInvitation, ProgrammingEvent } from '../../models/event.model';
import { EventService } from '../../services/event.service';

type InvitationCard = {
  invitation: EventInvitation;
  event: ProgrammingEvent | null;
};

@Component({
  selector: 'app-my-invitations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './my-invitations.component.html',
  styleUrl: './my-invitations.component.css'
})
export class MyInvitationsComponent implements OnInit, OnDestroy {
  isLoading = true;
  successMessage: string | null = null;
  errorMessage: string | null = null;
  cards: InvitationCard[] = [];

  private subs = new Subscription();
  private successTimeoutId: number | null = null;

  constructor(
    private readonly eventService: EventService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadInvitations();
  }

  ngOnDestroy(): void {
    if (this.successTimeoutId != null) {
      window.clearTimeout(this.successTimeoutId);
    }
    this.subs.unsubscribe();
  }

  goBack(): void {
    this.router.navigate(['/events']);
  }

  viewEvent(eventId: string): void {
    this.router.navigate(['/events', eventId]);
  }

  accept(eventId: string): void {
    const sub = this.eventService.acceptInvitation(eventId).subscribe({
      next: () => {
        this.setSuccess('Invitation accepted.');
        this.loadInvitations();
      },
      error: () => {
        this.errorMessage = 'Failed to accept invitation.';
      }
    });
    this.subs.add(sub);
  }

  decline(eventId: string): void {
    const sub = this.eventService.declineInvitation(eventId).subscribe({
      next: () => {
        this.loadInvitations();
      },
      error: () => {
        this.errorMessage = 'Failed to decline invitation.';
      }
    });
    this.subs.add(sub);
  }

  private loadInvitations(): void {
    this.isLoading = true;
    this.errorMessage = null;

    const sub = this.eventService.getMyInvitations().subscribe({
      next: (invites) => {
        if (!invites.length) {
          this.cards = [];
          this.isLoading = false;
          return;
        }

        const requests = invites.map((invitation) =>
          this.eventService.getEventById(invitation.eventId).pipe(
            map((event) => ({ invitation, event })),
            catchError(() => of({ invitation, event: null }))
          )
        );

        const joinSub = forkJoin(requests).subscribe({
          next: (cards) => {
            this.cards = cards;
            this.isLoading = false;
          },
          error: () => {
            this.errorMessage = 'Failed to load invitation details.';
            this.isLoading = false;
          }
        });
        this.subs.add(joinSub);
      },
      error: () => {
        this.errorMessage = 'Failed to load invitations.';
        this.isLoading = false;
      }
    });
    this.subs.add(sub);
  }

  private setSuccess(message: string): void {
    this.successMessage = message;
    if (this.successTimeoutId != null) {
      window.clearTimeout(this.successTimeoutId);
    }
    this.successTimeoutId = window.setTimeout(() => {
      this.successMessage = null;
      this.successTimeoutId = null;
    }, 2500);
  }
}
