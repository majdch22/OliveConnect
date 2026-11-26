import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthentificationService } from './authentification.service';
import { ESPACE_PAR_ROLE, pageDeConnexion, Role } from './modeles';

/**
 * Protège l'espace d'un rôle.
 * - Personne de connecté : renvoi vers la page de connexion adaptée.
 * - Connecté avec un autre rôle : page « Accès refusé », qui indique le profil auquel la page est réservée.
 * - Mot de passe provisoire (donné par l'administrateur) : renvoi vers la page de changement du mot de passe.
 */
export function roleGuard(roleAttendu: Role): CanActivateFn {
  return () => {
    const session = inject(AuthentificationService).session();
    const router = inject(Router);

    if (!session) {
      return router.parseUrl(pageDeConnexion(roleAttendu));
    }
    if (session.role !== roleAttendu) {
      return router.createUrlTree(['/acces-refuse'], { queryParams: { profil: roleAttendu } });
    }
    if (session.motDePasseProvisoire) {
      return router.parseUrl('/mot-de-passe');
    }
    return true;
  };
}

/** Page de changement du mot de passe : réservée aux personnes connectées. */
export const connecteGuard: CanActivateFn = () => {
  const session = inject(AuthentificationService).session();
  return session ? true : inject(Router).parseUrl('/connexion');
};

/** Sur une page de connexion : une personne déjà connectée va directement dans son espace. */
export const dejaConnecteGuard: CanActivateFn = () => {
  const session = inject(AuthentificationService).session();
  return session ? inject(Router).parseUrl(ESPACE_PAR_ROLE[session.role]) : true;
};
