import { HttpErrorResponse } from '@angular/common/http';

import { ReponseErreur } from '../authentification/modeles';

/** Message général à afficher après un appel au serveur qui a échoué. */
export function texteErreur(erreur: HttpErrorResponse): string {
  if (erreur.status === 0) {
    return 'Le serveur ne répond pas. Vérifiez votre connexion puis réessayez.';
  }
  const corps = erreur.error as Partial<ReponseErreur> | null;
  return corps?.message ?? 'Une erreur inattendue est survenue. Réessayez dans un instant.';
}

/** Messages par champ renvoyés par le serveur (ex. { telephone: "Ce numéro est déjà utilisé…" }). */
export function erreursParChamp(erreur: HttpErrorResponse): Record<string, string> {
  const corps = erreur.error as Partial<ReponseErreur> | null;
  return corps?.champs ?? {};
}
