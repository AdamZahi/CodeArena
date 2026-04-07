import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { EventService } from '../../../../front-office/event/services/event.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-admin-event-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-event-edit.component.html',
  styleUrl: './admin-event-edit.component.css'
})
export class AdminEventEditComponent implements OnInit {
  eventId: string | null = null;

  title = '';
  description = '';
  location = '';

  eventType: 'OPEN' | 'EXCLUSIVE' = 'OPEN';
  category: string = 'HACKATHON';

  startDate = '';
  endDate = '';
  maxParticipants = 50;
  status = 'UPCOMING';

  errorMessage: string | null = null;
  successMessage: string | null = null;
  isSubmitting = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly eventService: EventService,
    private readonly http: HttpClient
  ) { }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.eventId = id;
      this.loadEvent(id);
    } else {
      this.errorMessage = 'EVENT ID NOT FOUND';
    }
  }

  get isExclusive(): boolean {
    return this.eventType === 'EXCLUSIVE';
  }

  loadEvent(id: string): void {
    this.eventService.getEventById(id).subscribe({
      next: (event: any) => {
        this.title = event.title;
        this.description = event.description;
        this.location = event.location || '';
        this.eventType = event.type || 'OPEN';
        this.category = event.category || 'HACKATHON';
        this.maxParticipants = event.maxParticipants;
        this.status = event.status || 'UPCOMING';
        
        if (event.startDate) {
          this.startDate = event.startDate.substring(0, 16);
        }
        if (event.endDate) {
          this.endDate = event.endDate.substring(0, 16);
        }
      },
      error: (err: any) => {
        console.error('Failed to load event', err);
        this.errorMessage = 'FAILED TO LOAD EVENT DETAILS.';
      }
    });
  }

  normalizeLocalDateTimeInput(value: string): string {
    if (!value) return '';
    if (value.length === 16) return `${value}:00`;
    return value;
  }

  submit(): void {
    if (!this.eventId) return;
    
    this.errorMessage = null;
    this.successMessage = null;
    this.isSubmitting = true;

    const payload = {
      title: this.title.trim(),
      description: this.description.trim(),
      location: this.location.trim(),
      startDate: this.normalizeLocalDateTimeInput(this.startDate),
      endDate: this.normalizeLocalDateTimeInput(this.endDate),
      maxParticipants: Number(this.maxParticipants),
      type: this.eventType,
      category: this.category,
      status: this.status
    };

    const baseUrl = `${environment.apiBaseUrl}/api/events`;
    
    this.http.put(`${baseUrl}/${this.eventId}`, payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage = '✓ EVENT UPDATED SUCCESSFULLY!';
        window.setTimeout(() => {
          this.router.navigate(['/back-office/events', this.eventId]);
        }, 2000);
      },
      error: (err: any) => {
        console.error('Failed to update event', err);
        this.isSubmitting = false;
        this.errorMessage = 'EVENT UPDATE FAILED.';
      }
    });
  }
}
