import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { EventService } from '../../../../front-office/event/services/event.service';

@Component({
  selector: 'app-create-event',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './create-event.component.html',
  styleUrl: './create-event.component.css'
})
export class CreateEventComponent {
  title = '';
  description = '';
  organizerName = '';
  location = '';

  eventType: 'OPEN' | 'EXCLUSIVE' = 'OPEN';
  category:
    | 'HACKATHON'
    | 'NETWORKING'
    | 'BOOTCAMP'
    | 'CONFERENCE'
    | 'REMISE_PRIX' = 'HACKATHON';

  startDate = '';
  endDate = '';
  maxParticipants = 50;
  status = 'UPCOMING';

  errorMessage: string | null = null;
  successMessage: string | null = null;
  isSubmitting = false;
  type: string = 'OPEN';

  constructor(
    private readonly eventService: EventService,
    private readonly router: Router
  ) { }

  get isExclusive(): boolean {
    return this.eventType === 'EXCLUSIVE';
  }

  normalizeLocalDateTimeInput(value: string): string {
    if (!value) return '';
    // datetime-local returns "YYYY-MM-DDTHH:mm"
    // Backend expects LocalDateTime, so we add seconds if missing.
    if (value.length === 16) return `${value}:00`;
    return value;
  }

  submit(): void {
    this.errorMessage = null;
    this.successMessage = null;
    this.isSubmitting = true;

    const payload = {
      title: this.title.trim(),
      description: this.description.trim(),
      organizerId: this.organizerName.trim(),
      organizerName: this.organizerName.trim(),
      location: this.location.trim(),
      startDate: this.normalizeLocalDateTimeInput(this.startDate),
      endDate: this.normalizeLocalDateTimeInput(this.endDate),
      maxParticipants: Number(this.maxParticipants),
      type: this.eventType,
      category: this.category,
      status: this.status
    };

    this.eventService.createEvent(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage = '✓ EVENT CREATED SUCCESSFULLY!';
        window.setTimeout(() => {
          this.router.navigate(['/events']);
        }, 2000);
      },
      error: (err: any) => {
        console.error('Failed to create event', err);
        this.isSubmitting = false;
        this.errorMessage = 'EVENT CREATION FAILED.';
      }
    });
  }
}
