import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EventListComponent } from './event-list.component';
import { EventService } from '../../services/event.service';
import { RouterModule } from '@angular/router';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';

describe('EventListComponent', () => {
  let component: EventListComponent;
  let fixture: ComponentFixture<EventListComponent>;
  let mockEventService: jasmine.SpyObj<EventService>;

  beforeEach(async () => {
    mockEventService = jasmine.createSpyObj('EventService', [
      'getEvents',
      'getMyRegistrations',
      'getMyInvitations',
      'register',
      'getRecommendedEvents'
    ]);

    mockEventService.getEvents.and.returnValue(of([]));
    mockEventService.getMyRegistrations.and.returnValue(of([]));
    mockEventService.getMyInvitations.and.returnValue(of([]));
    mockEventService.getRecommendedEvents.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [
        EventListComponent,
        FormsModule,
        RouterModule.forRoot([])
      ],
      providers: [
        { provide: EventService, useValue: mockEventService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EventListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load events on init', () => {
    expect(mockEventService.getEvents).toHaveBeenCalled();
  });

  it('should filter OPEN events', () => {
    component.events = [
      { id: '1', type: 'OPEN', title: 'Open Event' } as any,
      { id: '2', type: 'EXCLUSIVE', title: 'VIP Event' } as any
    ];
    component.selectedType = 'OPEN';
    (component as any).applyFilters();

    expect(component.filteredEvents.length).toBe(1);
    expect(component.filteredEvents[0].type).toBe('OPEN');
  });

  it('should filter EXCLUSIVE events', () => {
    component.events = [
      { id: '1', type: 'OPEN', title: 'Open Event' } as any,
      { id: '2', type: 'EXCLUSIVE', title: 'VIP Event' } as any
    ];
    component.selectedType = 'EXCLUSIVE';
    (component as any).applyFilters();

    expect(component.filteredEvents.length).toBe(1);
    expect(component.filteredEvents[0].type).toBe('EXCLUSIVE');
  });

  it('should show ALL events when filter is ALL', () => {
    component.events = [
      { id: '1', type: 'OPEN' } as any,
      { id: '2', type: 'EXCLUSIVE' } as any
    ];
    component.selectedType = 'ALL';
    (component as any).applyFilters();

    expect(component.filteredEvents.length).toBe(2);
  });
});
