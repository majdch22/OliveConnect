import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthentificationService } from './authentification.service';

/**
 * Ajoute le jeton d'accès à chaque appel au serveur.
 * Si le serveur répond 401 (jeton expiré ou invalide) en dehors de la connexion, la session est fermée.
 */
export const jetonInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const authentification = inject(AuthentificationService);
  const jeton = authentification.jeton();
  const estConnexion = requete.url.endsWith('/connexion');

  const requeteAvecJeton =
    jeton && requete.url.startsWith('/api') && !estConnexion
      ? requete.clone({ setHeaders: { Authorization: `Bearer ${jeton}` } })
      : requete;

  return suivant(requeteAvecJeton).pipe(
    catchError((erreur: HttpErrorResponse) => {
      if (erreur.status === 401 && !estConnexion) {
        authentification.deconnecter();
      }
      return throwError(() => erreur);
    }),
  );
};
