import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '@auth0/auth0-angular';
import { BehaviorSubject, EMPTY, Subscription, Observable, of } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, switchMap, take, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface CurrentUser {
  id?: string;
  auth0Id?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  nickname?: string;
  role?: string;
  level?: number;
  totalXp?: number;
  activeIconId?: string;
  activeBorderId?: string;
  activeTitle?: string;
  activeBadge1?: string;
  activeBadge2?: string;
  activeBadge3?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthUserSyncService {
  private static readonly pendingProfileKey = 'codearena.pendingSignupProfile';
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly subscription: Subscription;
  private readonly currentUserSubject = new BehaviorSubject<CurrentUser | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor() {
    this.subscription = this.auth.isAuthenticated$
      .pipe(
        distinctUntilChanged(),
        filter((isAuthenticated) => isAuthenticated),
        switchMap(() => this.auth.user$.pipe(take(1))),
        filter(user => !!user),
        switchMap((user) => 
          this.http.patch<CurrentUser>(`${environment.apiBaseUrl}/api/users/me`, {
            firstName: user?.given_name || (user?.name?.includes(' ') ? user.name.split(' ')[0] : user?.name),
            lastName: user?.family_name || (user?.name?.includes(' ') ? user.name.split(' ').slice(1).join(' ') : null),
            nickname: user?.nickname || user?.name,
            email: user?.email,
            avatarUrl: user?.picture
          }).pipe(
            catchError(err => {
              console.warn('Silent profile sync failed', err);
              return of(null);
            }),
            switchMap(() => this.http.get<CurrentUser>(`${environment.apiBaseUrl}/api/users/me`))
          )
        ),
        switchMap((user) => this.applyPendingSignupProfileIfAny(user!)),
        tap((user) => this.currentUserSubject.next(user)),
        catchError(() => {
          this.currentUserSubject.next(null);
          return EMPTY;
        })
      )
      .subscribe();
  }

  forceSync(): void {
    this.getCurrentUserSnapshot().subscribe();
  }

  getCurrentUserSnapshot(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${environment.apiBaseUrl}/api/users/me`)
      .pipe(
        switchMap((user) => this.applyPendingSignupProfileIfAny(user)),
        tap((user) => this.currentUserSubject.next(user)),
        catchError(() => EMPTY)
      );
  }

  keepAlive(): void {}

  private applyPendingSignupProfileIfAny(user: CurrentUser): Observable<CurrentUser> {
    const raw = sessionStorage.getItem(AuthUserSyncService.pendingProfileKey);
    if (!raw) {
      return of(user);
    }

    let pending: {
      firstName?: string;
      lastName?: string | null;
      nickname?: string;
      bio?: string;
      avatarUrl?: string;
    };

    try {
      pending = JSON.parse(raw) as {
        firstName?: string;
        lastName?: string | null;
        nickname?: string;
        bio?: string;
        avatarUrl?: string;
      };
    } catch {
      sessionStorage.removeItem(AuthUserSyncService.pendingProfileKey);
      return of(user);
    }

    return this.http.patch<CurrentUser>(`${environment.apiBaseUrl}/api/users/me`, {
      firstName: pending.firstName || null,
      lastName: pending.lastName || null,
      nickname: pending.nickname || null,
      bio: pending.bio || null,
      avatarUrl: pending.avatarUrl || null
    }).pipe(
      tap(() => sessionStorage.removeItem(AuthUserSyncService.pendingProfileKey)),
      catchError(() => {
        sessionStorage.removeItem(AuthUserSyncService.pendingProfileKey);
        return of(user);
      }),
      map((patched) => patched ?? user)
    );
  }
}