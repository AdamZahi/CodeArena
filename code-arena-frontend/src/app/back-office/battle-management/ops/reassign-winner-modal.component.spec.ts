import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { BattleRoomDetail } from '../models/battle-admin.models';
import { BattleAdminService } from '../services/battle-admin.service';
import { ToastService } from '../shared/toast.service';
import { ReassignWinnerModalComponent } from './reassign-winner-modal.component';

function makeRoom(winnerId: string | null): BattleRoomDetail {
  return {
    id: '00000000-1111-2222-3333-444444444444',
    hostId: 'auth0|host',
    hostUsername: 'host',
    mode: 'DUEL',
    maxPlayers: 2,
    challengeCount: 1,
    inviteToken: 'tok',
    isPublic: true,
    status: 'FINISHED',
    startsAt: null,
    endsAt: null,
    createdAt: new Date().toISOString(),
    challengeIds: ['1'],
    winnerId,
    participants: [
      { id: 'p1', userId: 'auth0|alice', username: 'Alice', role: 'PLAYER', ready: true, score: 90, rank: 1, eloChange: 8, joinedAt: new Date().toISOString() },
      { id: 'p2', userId: 'auth0|bob',   username: 'Bob',   role: 'PLAYER', ready: true, score: 70, rank: 2, eloChange: -8, joinedAt: new Date().toISOString() }
    ]
  };
}

describe('ReassignWinnerModalComponent', () => {
  let fixture: ComponentFixture<ReassignWinnerModalComponent>;
  let component: ReassignWinnerModalComponent;
  let api: jasmine.SpyObj<BattleAdminService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    api = jasmine.createSpyObj<BattleAdminService>('BattleAdminService', ['reassignWinner']);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [ReassignWinnerModalComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: BattleAdminService, useValue: api },
        { provide: ToastService, useValue: toast }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReassignWinnerModalComponent);
    component = fixture.componentInstance;
    component.room = makeRoom('auth0|alice');
    fixture.detectChanges();
  });

  it('disallows proceeding when no new winner or no reason', () => {
    expect(component.canProceed()).toBeFalse();
    component.newWinnerId = 'auth0|bob';
    expect(component.canProceed()).toBeFalse();
    component.reason = 'fix';
    expect(component.canProceed()).toBeTrue();
  });

  it('disallows reassigning to the current winner', () => {
    component.newWinnerId = 'auth0|alice';
    component.reason = 'invalid';
    expect(component.canProceed()).toBeFalse();
  });

  it('calls API and emits succeeded on confirm', () => {
    api.reassignWinner.and.returnValue(of({} as any));
    const succeededSpy = spyOn(component.succeeded, 'emit');
    component.newWinnerId = 'auth0|bob';
    component.reason = 'mis-scored';
    component.confirm();
    expect(api.reassignWinner).toHaveBeenCalledWith(component.room.id, 'auth0|bob', 'mis-scored');
    expect(succeededSpy).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalled();
  });

  it('toasts error and resets submitting on API failure', () => {
    api.reassignWinner.and.returnValue(throwError(() => ({ error: { message: 'nope' } })));
    component.newWinnerId = 'auth0|bob';
    component.reason = 'reason';
    component.confirm();
    expect(toast.error).toHaveBeenCalled();
    expect(component.submitting()).toBeFalse();
  });

  it('renders a sensible label when no winner is currently set', () => {
    component.room = makeRoom(null);
    expect(component.currentWinnerLabel()).toBe('No winner declared');
  });
});
