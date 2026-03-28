import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { ProgrammingEvent } from '../../../../front-office/event/models/event.model';
import { EventService } from '../../../../front-office/event/services/event.service';

@Component({
  selector: 'app-admin-event-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-event-list.component.html',
  styleUrl: './admin-event-list.component.css'
})
export class AdminEventListComponent implements OnInit, OnDestroy {
  isLoading = true;
  events: ProgrammingEvent[] = [];
  errorMessage: string | null = null;

  private subs = new Subscription();

  constructor(
    private readonly eventService: EventService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  goCreate(): void {
    this.router.navigate(['create'], { relativeTo: this.route });
  }

  viewEvent(id: string): void {
    this.router.navigate([id], { relativeTo: this.route });
  }

  deleteEvent(id: string): void {
    const sub = this.eventService.deleteEvent(id).subscribe({
      next: () => this.loadEvents(),
      error: () => (this.errorMessage = 'Failed to delete event.')
    });
    this.subs.add(sub);
  }

  private loadEvents(): void {
    this.isLoading = true;
    this.errorMessage = null;
    const sub = this.eventService.getEvents().subscribe({
      next: (events: any[]) => {
        this.events = events;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load events.';
        this.isLoading = false;
      }
    });
    this.subs.add(sub);
  }
}
