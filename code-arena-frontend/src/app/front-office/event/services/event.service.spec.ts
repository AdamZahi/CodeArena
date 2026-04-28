import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule,
         HttpTestingController } from '@angular/common/http/testing';
import { EventService } from './event.service';
import { ProgrammingEvent } from '../models/event.model';

describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/events';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EventService]
    });
    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAllEvents() should return events list', () => {
    const mockEvents: ProgrammingEvent[] = [
      {
        id: '1',
        title: 'Hackathon 2026',
        type: 'OPEN',
        category: 'HACKATHON',
        status: 'UPCOMING',
        description: 'Test',
        organizerId: 'org-1',
        startDate: '2026-05-01T09:00:00',
        endDate: '2026-05-03T18:00:00',
        maxParticipants: 50,
        currentParticipants: 10,
        availablePlaces: 40,
        isFull: false,
        fillRate: 20,
        createdAt: '2026-01-01T00:00:00'
      }
    ];

    service.getEvents().subscribe(events => {
      expect(events.length).toBe(1);
      expect(events[0].title).toBe('Hackathon 2026');
      expect(events[0].type).toBe('OPEN');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockEvents);
  });

  it('getEventById() should return single event', () => {
    const mockEvent: ProgrammingEvent = {
      id: 'uuid-123',
      title: 'Elite Bootcamp',
      type: 'EXCLUSIVE',
      category: 'BOOTCAMP',
      status: 'UPCOMING',
      description: 'VIP event',
      organizerId: 'org-1',
      startDate: '2026-06-01T09:00:00',
      endDate: '2026-06-03T18:00:00',
      maxParticipants: 20,
      currentParticipants: 5,
      availablePlaces: 15,
      isFull: false,
      fillRate: 25,
      createdAt: '2026-01-01T00:00:00'
    };

    service.getEventById('uuid-123').subscribe(event => {
      expect(event.title).toBe('Elite Bootcamp');
      expect(event.type).toBe('EXCLUSIVE');
    });

    const req = httpMock.expectOne(`${baseUrl}/uuid-123`);
    expect(req.request.method).toBe('GET');
    req.flush(mockEvent);
  });

  it('register() should POST to correct URL', () => {
    service.register('event-123').subscribe();

    const req = httpMock.expectOne(
      `${baseUrl}/event-123/register`
    );
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('cancelRegistration() should DELETE', () => {
    service.cancelRegistration('event-123').subscribe();

    const req = httpMock.expectOne(
      `${baseUrl}/event-123/register`
    );
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('submitCandidature() should POST with motivation', () => {
    service.submitCandidature('event-123',
        'I am very motivated to join').subscribe();

    const req = httpMock.expectOne(
      `${baseUrl}/event-123/candidature`
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(
      { motivation: 'I am very motivated to join' }
    );
    req.flush({});
  });

  it('getMyRegistrations() should GET correct URL', () => {
    service.getMyRegistrations().subscribe();

    const req = httpMock.expectOne(
      `${baseUrl}/me/registrations`
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getMyInvitations() should GET correct URL', () => {
    service.getMyInvitations().subscribe();

    const req = httpMock.expectOne(
      `${baseUrl}/me/invitations`
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
