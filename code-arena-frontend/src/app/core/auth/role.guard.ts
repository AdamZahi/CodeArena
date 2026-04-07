import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthProfileService } from './auth-profile.service';

export const roleGuard: CanActivateFn = async (route: ActivatedRouteSnapshot) => {
  const profileService = inject(AuthProfileService);
  const router = inject(Router);
  const requiredRole = route.data['role'] as string;
  const fallbackUrl = (route.data['fallbackUrl'] as string) || '/forbidden';

  const role = await profileService.getRole();

  // Negative check: role must NOT be present (e.g., '!COACH' means must NOT be a coach)
  if (requiredRole && requiredRole.startsWith('!')) {
    const forbiddenRole = requiredRole.substring(1);
    return (role === forbiddenRole) ? router.parseUrl(fallbackUrl) : true;
  }

  // Positive check: role must be exactly the string
  if (requiredRole) {
    return (role === requiredRole) ? true : router.parseUrl(fallbackUrl);
  }

  // No role required
  return true;
};
