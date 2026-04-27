import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { of, throwError } from 'rxjs';
import { ChapterListComponent } from './chapter-list.component';
import { AdminTqService } from '../../services/admin-tq.service';

describe('ChapterListComponent', () => {
  let component: ChapterListComponent;
  let adminTqMock: any;

  const mockChapters = [
    { id: 'c1', title: 'Chapter 1', description: 'desc', orderIndex: 1, isLocked: false, missions: [{ id: 'm1' }], levels: [{ id: 'l1' }, { id: 'l2' }] },
    { id: 'c2', title: 'Chapter 2', description: 'desc', orderIndex: 2, isLocked: true, missions: [], levels: [] }
  ];

  beforeEach(async () => {
    adminTqMock = {
      getChapters: jasmine.createSpy('getChapters').and.returnValue(of(mockChapters)),
      deleteChapter: jasmine.createSpy('deleteChapter').and.returnValue(of(undefined))
    };

    await TestBed.configureTestingModule({
      imports: [ChapterListComponent],
      providers: [
        { provide: AdminTqService, useValue: adminTqMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(ChapterListComponent, { set: { imports: [CommonModule], schemas: [NO_ERRORS_SCHEMA] } })
      .compileComponents();

    const fixture = TestBed.createComponent(ChapterListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should load chapters on init', () => {
    expect(adminTqMock.getChapters).toHaveBeenCalled();
    expect(component.chapters.length).toBe(2);
  });

  it('should set isLoading = false after load', () => {
    expect(component.isLoading).toBeFalse();
  });

  it('should count missions correctly', () => {
    expect(component.missionCount(mockChapters[0] as any)).toBe(1);
    expect(component.missionCount(mockChapters[1] as any)).toBe(0);
  });

  it('should count levels correctly', () => {
    expect(component.levelCount(mockChapters[0] as any)).toBe(2);
    expect(component.levelCount(mockChapters[1] as any)).toBe(0);
  });

  it('should set errorMsg and isLoading=false on load error', () => {
    adminTqMock.getChapters.and.returnValue(throwError(() => new Error('fail')));
    component.isLoading = true;
    component.ngOnInit();
    expect(component.isLoading).toBeFalse();
    expect(component.errorMsg).toBeTruthy();
  });
});
