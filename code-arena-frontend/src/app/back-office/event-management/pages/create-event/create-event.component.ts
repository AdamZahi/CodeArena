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

    if (!this.validateForm()) {
      return;
    }

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
        
        if (err.status === 400 && err.error) {
          const errors = err.error;
          if (typeof errors === 'object') {
            this.errorMessage = Object.values(errors).join(' | ').toUpperCase();
          } else {
            this.errorMessage = String(errors).toUpperCase();
          }
        } else {
          this.errorMessage = 'EVENT CREATION FAILED.';
        }
      }
    });
  }

  private validateForm(): boolean {
    if (!this.title || this.title.trim().length < 3) {
      this.errorMessage = 'TITLE MUST BE AT LEAST 3 CHARACTERS.';
      return false;
    }
    if (!this.description || this.description.trim().length < 10) {
      this.errorMessage = 'DESCRIPTION MUST BE AT LEAST 10 CHARACTERS.';
      return false;
    }
    if (!this.startDate || !this.endDate) {
      this.errorMessage = 'START AND END DATES ARE REQUIRED.';
      return false;
    }

    const start = new Date(this.startDate).getTime();
    const end = new Date(this.endDate).getTime();
    const now = new Date().getTime();

    if (start <= now) {
      this.errorMessage = 'START DATE MUST BE IN THE FUTURE.';
      return false;
    }
    if (end <= start) {
      this.errorMessage = 'END DATE MUST BE AFTER START DATE.';
      return false;
    }
    if (this.maxParticipants < 1 || this.maxParticipants > 1000) {
      this.errorMessage = 'PARTICIPANTS MUST BE BETWEEN 1 AND 1000.';
      return false;
    }

    return true;
  }
}

