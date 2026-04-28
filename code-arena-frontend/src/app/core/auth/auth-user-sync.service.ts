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
  avatarUrl?: string;
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

interface Auth0UserProfile {
  email?: string;
  given_name?: string;
  family_name?: string;
  name?: string;
  nickname?: string;
  picture?: string;
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
        switchMap(() =>
          this.http.get<CurrentUser>(`${environment.apiBaseUrl}/api/users/me`)
            .pipe(
              switchMap((user) => this.applyPendingSignupProfileIfAny(user)),
              switchMap((user) => this.applyAuth0ProfileIfMissing(user)),
              tap((user) => this.currentUserSubject.next(user)),
              catchError(() => {
                this.currentUserSubject.next(null);
                return EMPTY;
              })
            )
        )
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
        switchMap((user) => this.applyAuth0ProfileIfMissing(user)),
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

  private applyAuth0ProfileIfMissing(user: CurrentUser): Observable<CurrentUser> {
    return this.auth.user$.pipe(
      take(1),
      switchMap((authUser) => {
        const profile = (authUser ?? {}) as Auth0UserProfile;

        const fullName = this.clean(profile.name);
        const parsed = this.splitName(fullName);
        const firstName = this.clean(profile.given_name) ?? parsed.firstName ?? this.clean(profile.nickname);
        const lastName = this.clean(profile.family_name) ?? parsed.lastName;
        const email = this.clean(profile.email);
        const nickname = this.clean(profile.nickname);
        const avatarUrl = this.clean(profile.picture);

        const patch: {
          email?: string | null;
          firstName?: string | null;
          lastName?: string | null;
          nickname?: string | null;
          avatarUrl?: string | null;
        } = {};

        if (!this.clean(user.email) && email) {
          patch.email = email;
        }
        if (!this.clean(user.firstName) && firstName) {
          patch.firstName = firstName;
        }
        if (!this.clean(user.lastName) && lastName) {
          patch.lastName = lastName;
        }
        if (!this.clean(user.nickname) && nickname) {
          patch.nickname = nickname;
        }
        if (!this.clean(user.avatarUrl) && avatarUrl) {
          patch.avatarUrl = avatarUrl;
        }

        if (Object.keys(patch).length === 0) {
          return of(user);
        }

        return this.http.patch<CurrentUser>(`${environment.apiBaseUrl}/api/users/me`, patch).pipe(
          catchError(() => of(user)),
          map((patched) => patched ?? user)
        );
      })
    );
  }

  private clean(value?: string | null): string | null {
    if (typeof value !== 'string') {
      return null;
    }
    const trimmed = value.trim();
    return trimmed.length ? trimmed : null;
  }

  private splitName(fullName: string | null): { firstName: string | null; lastName: string | null } {
    if (!fullName) {
      return { firstName: null, lastName: null };
    }

    const parts = fullName.split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
      return { firstName: null, lastName: null };
    }
    if (parts.length === 1) {
      return { firstName: parts[0], lastName: null };
    }

    return {
      firstName: parts[0],
      lastName: parts.slice(1).join(' ')
    };
  }
}
