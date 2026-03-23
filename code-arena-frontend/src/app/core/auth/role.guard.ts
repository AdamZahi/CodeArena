import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from './keycloak.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);
  const requiredRole = route.data['role'] as string;

  if (requiredRole && keycloakService.hasRole(requiredRole)) {
    return true;
  }

  // TODO: Replace with dedicated forbidden page handling.
  return router.parseUrl('/forbidden');
};
