import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { BattleRoomDetail } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';
import { ForceEndModalComponent } from './force-end-modal.component';

function makeRoom(): BattleRoomDetail {
  return {
    id: '11111111-aaaa-bbbb-cccc-222222222222',
    hostId: 'auth0|host',
    hostUsername: 'host',
    mode: 'DUEL',
    maxPlayers: 2,
    challengeCount: 1,
    inviteToken: 'tok',
    isPublic: true,
    status: 'IN_PROGRESS',
    startsAt: null,
    endsAt: null,
    createdAt: new Date().toISOString(),
    challengeIds: ['1'],
    winnerId: null,
    participants: [
      { id: 'p1', userId: 'auth0|alice', username: 'Alice', role: 'PLAYER', ready: true, score: null, rank: null, eloChange: null, joinedAt: new Date().toISOString() },
      { id: 'p2', userId: 'auth0|bob',   username: 'Bob',   role: 'PLAYER', ready: true, score: null, rank: null, eloChange: null, joinedAt: new Date().toISOString() }
    ]
  };
}

describe('ForceEndModalComponent', () => {
  let fixture: ComponentFixture<ForceEndModalComponent>;
  let component: ForceEndModalComponent;
  let api: jasmine.SpyObj<BattleAdminService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    api = jasmine.createSpyObj<BattleAdminService>('BattleAdminService', ['forceEnd']);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [ForceEndModalComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: BattleAdminService, useValue: api },
        { provide: ToastService, useValue: toast }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ForceEndModalComponent);
    component = fixture.componentInstance;
    component.room = makeRoom();
    fixture.detectChanges();
  });

  it('blocks step transition until reason is provided', () => {
    expect(component.canProceed()).toBeFalse();
    component.reason = 'stuck for 30 minutes';
    expect(component.canProceed()).toBeTrue();
  });

  it('emits succeeded and toasts on success', () => {
    api.forceEnd.and.returnValue(of({} as any));
    const succeededSpy = spyOn(component.succeeded, 'emit');

    component.winnerId = 'auth0|alice';
    component.reason = 'admin call';
    component.confirm();

    expect(api.forceEnd).toHaveBeenCalledWith(component.room.id, 'auth0|alice', 'admin call');
    expect(toast.success).toHaveBeenCalled();
    expect(succeededSpy).toHaveBeenCalled();
    expect(component.submitting()).toBeFalse();
  });

  it('shows an error toast and clears submitting on failure', () => {
    api.forceEnd.and.returnValue(throwError(() => ({ error: { message: 'boom' } })));
    component.reason = 'reason';
    component.confirm();
    expect(toast.error).toHaveBeenCalled();
    expect(component.submitting()).toBeFalse();
  });

  it('treats null winnerId as a draw label', () => {
    component.winnerId = null;
    expect(component.winnerLabel()).toBe('Draw (no winner)');
  });
});
