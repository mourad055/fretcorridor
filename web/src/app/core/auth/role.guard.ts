import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './auth.models';

/**
 * FE-WEB-03 / FE-WEB-04 : aucune route n'est atteignable en modifiant l'URL sans
 * passer la garde de rôle. Non authentifié → /login. Rôle ne correspondant pas à
 * la route → /403 (réponse propre, jamais une erreur technique ni une fuite de
 * données réseau — la requête HTTP protégée n'est même pas déclenchée).
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRole = route.data['role'] as Role | undefined;
  const session = authService.session();

  if (!session) {
    return router.createUrlTree(['/login']);
  }

  if (requiredRole && session.role !== requiredRole) {
    return router.createUrlTree(['/403']);
  }

  return true;
};

/** Empêche un acteur déjà connecté de revoir l'écran de connexion. */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return router.createUrlTree(['/']);
  }
  return true;
};
