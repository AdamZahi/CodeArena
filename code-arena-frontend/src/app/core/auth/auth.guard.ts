import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from './keycloak.service';

export const authGuard: CanActivateFn = async () => {
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);

  if (keycloakService.isAuthenticated()) {
    return true;
  }
/*
  // TODO: Trigger Keycloak login flow.
  await keycloakService.login();
  return router.parseUrl('/login');
};*/
 return true;
};
