import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { map } from 'rxjs';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requiredRole = route.data['role'] as string;

  return auth.idTokenClaims$.pipe(
    map((claims) => {
      const roles = ((claims as any)?.['https://codearena.com/roles'] as string[]) ?? [];
      if (requiredRole && roles.includes(requiredRole)) {
        return true;
      }
      return router.parseUrl('/forbidden');
    })
  );
};
