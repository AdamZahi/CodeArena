import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from './keycloak.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);
  const requiredRole = route.data['role'] as string;

  // TODO: Remove bypass when Keycloak is running
  // DEVELOPMENT BYPASS
  if (!keycloakService.isAuthenticated()) {
    return true; // allow through for dev
  }

  if (requiredRole && keycloakService.hasRole(requiredRole)) {
    return true;
  }

  return router.parseUrl('/forbidden');
};
