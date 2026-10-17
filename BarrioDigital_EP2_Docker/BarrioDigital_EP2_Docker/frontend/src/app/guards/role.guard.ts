import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { Rol } from '../models/barrio.models';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const roles = (route.data['roles'] ?? []) as Rol[];

  return auth.hasAnyRole(...roles)
    ? true
    : router.createUrlTree(['/dashboard']);
};