import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { environment } from '../../../environments/environment';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const http = inject(HttpClient);
  const requiredRole = route.data['role'] as string;

  return http.get<{ role?: string }>(`${environment.apiBaseUrl}/api/users/me`).pipe(
    map((user) => (user?.role === requiredRole ? true : router.parseUrl('/forbidden'))),
    catchError(() => of(router.parseUrl('/forbidden')))
  );
};