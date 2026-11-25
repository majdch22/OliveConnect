import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthentificationService } from './authentification.service';
import { ESPACE_PAR_ROLE, pageDeConnexion, Role } from './modeles';

/**
 * Protège l'espace d'un rôle.
 * - Personne de connecté : renvoi vers la page de connexion adaptée.
 * - Connecté avec un autre rôle : renvoi vers son propre espace.
 */
export function roleGuard(roleAttendu: Role): CanActivateFn {
  return () => {
    const session = inject(AuthentificationService).session();
    const router = inject(Router);

    if (!session) {
      return router.parseUrl(pageDeConnexion(roleAttendu));
    }
    if (session.role !== roleAttendu) {
      return router.parseUrl(ESPACE_PAR_ROLE[session.role]);
    }
    return true;
  };
}

/** Sur une page de connexion : une personne déjà connectée va directement dans son espace. */
export const dejaConnecteGuard: CanActivateFn = () => {
  const session = inject(AuthentificationService).session();
  return session ? inject(Router).parseUrl(ESPACE_PAR_ROLE[session.role]) : true;
};
