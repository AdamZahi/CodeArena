import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { EventService } from '../../services/event.service';
import { ProgrammingEvent } from '../../models/event.model';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './event-list.component.html',
  styleUrl: './event-list.component.css'
})
export class EventListComponent implements OnInit, OnDestroy {
  events: any[] = [];
  filteredEvents: any[] = [];

  selectedType: 'ALL' | 'OPEN' | 'EXCLUSIVE' = 'ALL';
  searchText = '';

  isLoading = true;
  error = '';
  actionError: string = '';

  countdowns: Record<string, string> = {};

  currentPage = 0;
  pageSize = 6;
  totalPages = 0;
  pages: number[] = [];
  pagedEvents: any[] = [];

  private countdownIntervalId: number | null = null;
  private subs = new Subscription();

  constructor(
    private readonly eventService: EventService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadEvents();
    this.startCountdownLoop();
  }

  ngOnDestroy(): void {
    if (this.countdownIntervalId != null) {
      window.clearInterval(this.countdownIntervalId);
    }
    this.subs.unsubscribe();
  }

  setFilter(type: 'ALL' | 'OPEN' | 'EXCLUSIVE'): void {
    this.selectedType = type;
    this.applyFilters();
  }

  onSearch(): void {
    this.applyFilters();
  }

  private applyFilters(): void {
    this.currentPage = 0;
    const q = this.searchText.trim().toLowerCase();
    this.filteredEvents = this.events.filter((e) => {
      const matchesType = this.selectedType === 'ALL' ? true : (e.type === this.selectedType || e.eventType === this.selectedType);
      const matchesTitle = q ? e.title.toLowerCase().includes(q) : true;
      return matchesType && matchesTitle;
    });
    this.updatePagination();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredEvents.length / this.pageSize);
    this.pages = Array.from({length: this.totalPages}, (_, i) => i);
    this.pagedEvents = this.filteredEvents.slice(
      this.currentPage * this.pageSize,
      (this.currentPage + 1) * this.pageSize
    );
  }

  goToPage(page: number): void { this.currentPage = page; this.updatePagination(); }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.updatePagination(); } }
  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.updatePagination(); } }
  minVal(a: number, b: number): number { return Math.min(a, b); }

  openDetail(id: string): void {
    this.router.navigate(['/events', id]);
  }

  openMyRegistrations(): void {
    this.router.navigate(['/events/my-registrations']);
  }

  openMyInvitations(): void {
    this.router.navigate(['/events/my-invitations']);
  }

  getTypeColor(type: string): string {
    if (type === 'OPEN') return 'var(--neon3)';
    if (type === 'EXCLUSIVE') return 'var(--neon)';
    return 'var(--neon2)';
  }

  getPlacesLeft(event: any): number {
    return (event.maxParticipants || 0) - (event.currentParticipants || 0);
  }

  getProgressPercent(event: any): number {
    const max = event.maxParticipants || 1;
    const current = event.currentParticipants || 0;
    return Math.min(100, (current / max) * 100);
  }

  getCountdownShort(dateStr: string): string {
    if (!dateStr) return '—';
    const start = new Date(dateStr).getTime();
    if (Number.isNaN(start)) return '—';
    const diff = start - Date.now();
    if (diff <= 0) return 'ENDED';
    
    const totalSeconds = Math.floor(diff / 1000);
    const days = Math.floor(totalSeconds / (3600 * 24));
    const hours = Math.floor((totalSeconds % (3600 * 24)) / 3600);
    return `${days}D ${hours}H`;
  }

  getStatusBadgeClass(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'ACTIVE') return 'event-status-active';
    if (s === 'COMPLETED') return 'event-status-completed';
    return 'event-status-upcoming';
  }

  formatDate(isoLike: string): string {
    if (!isoLike) return '';
    const date = new Date(isoLike);
    if (Number.isNaN(date.getTime())) return isoLike;
    
    const day = date.getDate().toString().padStart(2, '0');
    const month = date.toLocaleString('en-US', { month: 'short' }).toUpperCase();
    const year = date.getFullYear();
    const hours = date.getHours().toString().padStart(2, '0');
    const mins = date.getMinutes().toString().padStart(2, '0');
    return `${day} ${month} ${year} · ${hours}:${mins}`;
  }

  private loadEvents(): void {
    this.isLoading = true;
    const sub = this.eventService.getEvents().subscribe({
      next: (events) => {
        this.events = events;
        this.applyFilters();
        this.updatePagination();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load events', err);
        this.error = 'Failed to load events';
        this.isLoading = false;
      }
    });
    this.subs.add(sub);
  }

  private startCountdownLoop(): void {
    // We update on every frame roughly, but 1000ms is fine.
    // getCountdownShort automatically computes from Date.now() in HTML.
  }
}
