import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { BattleRoomAdmin, SpringPage } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';
import { BulkCancelComponent } from './bulk-cancel.component';

function makeRoom(id: string, status: BattleRoomAdmin['status']): BattleRoomAdmin {
  return {
    id, challengeId: null, challengeTitle: null, hostId: 'auth0|host', hostUsername: 'host',
    status, mode: 'DUEL', roomKey: null, createdAt: new Date().toISOString(),
    participantCount: 2, winnerId: null
  };
}

function pageOf(rooms: BattleRoomAdmin[]): SpringPage<BattleRoomAdmin> {
  return {
    content: rooms, totalElements: rooms.length, totalPages: 1, number: 0, size: rooms.length,
    numberOfElements: rooms.length, first: true, last: true
  };
}

describe('BulkCancelComponent', () => {
  let fixture: ComponentFixture<BulkCancelComponent>;
  let component: BulkCancelComponent;
  let api: jasmine.SpyObj<BattleAdminService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    api = jasmine.createSpyObj<BattleAdminService>('BattleAdminService', ['listRooms', 'bulkCancel']);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error', 'info']);

    api.listRooms.and.returnValue(of(pageOf([
      makeRoom('aaa', 'WAITING'),
      makeRoom('bbb', 'IN_PROGRESS'),
      makeRoom('ccc', 'FINISHED')
    ])));

    await TestBed.configureTestingModule({
      imports: [BulkCancelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: BattleAdminService, useValue: api },
        { provide: ToastService, useValue: toast }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BulkCancelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('filters out finished rooms from the selectable list', () => {
    expect(component.rooms().map((r) => r.id)).toEqual(['aaa', 'bbb']);
  });

  it('blocks proceeding without selection or reason', () => {
    expect(component.canProceed()).toBeFalse();
    component.selected.set(new Set(['aaa']));
    expect(component.canProceed()).toBeFalse();
    component.reason = 'cleanup';
    expect(component.canProceed()).toBeTrue();
  });

  it('submits bulk cancel and reports success and skipped ids', () => {
    api.bulkCancel.and.returnValue(of({ requested: 2, cancelled: 1, notFound: ['bbb'] }));
    component.selected.set(new Set(['aaa', 'bbb']));
    component.reason = 'maintenance';
    component.submit();

    expect(api.bulkCancel).toHaveBeenCalledWith(['aaa', 'bbb'], 'maintenance');
    expect(toast.success).toHaveBeenCalled();
    expect(toast.info).toHaveBeenCalled();
    expect(component.selected().size).toBe(0);
  });

  it('toggles all on and off', () => {
    component.toggleAll({ target: { checked: true } } as unknown as Event);
    expect(component.allSelected()).toBeTrue();
    component.toggleAll({ target: { checked: false } } as unknown as Event);
    expect(component.selected().size).toBe(0);
  });
});
