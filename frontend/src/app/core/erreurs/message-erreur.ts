import { HttpErrorResponse } from '@angular/common/http';

import { ReponseErreur } from '../authentification/modeles';

/** Message général à afficher après un appel au serveur qui a échoué. */
export function texteErreur(erreur: HttpErrorResponse): string {
  const corps = erreur.error as Partial<ReponseErreur> | null;
  // 0 : pas de réseau ; 502, 503, 504 sans message du serveur : le serveur est arrêté (le relais ne le joint pas)
  const serveurInjoignable = erreur.status === 0 || ([502, 503, 504].includes(erreur.status) && !corps?.message);
  if (serveurInjoignable) {
    return 'Le serveur ne répond pas. Vérifiez votre connexion puis réessayez.';
  }
  return corps?.message ?? 'Une erreur inattendue est survenue. Réessayez dans un instant.';
}

/** Messages par champ renvoyés par le serveur (ex. { telephone: "Ce numéro est déjà utilisé…" }). */
export function erreursParChamp(erreur: HttpErrorResponse): Record<string, string> {
  const corps = erreur.error as Partial<ReponseErreur> | null;
  return corps?.champs ?? {};
}
