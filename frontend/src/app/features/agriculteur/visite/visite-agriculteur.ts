import { Injectable } from '@angular/core';

import { EtatVisiteGuidee } from '../../../shared/visite-guidee/etat-visite-guidee';
import { EtapeVisite } from '../../../shared/visite-guidee/visite-guidee';

/** Les 5 étapes de la visite de l'espace agriculteur ; chaque cible est un attribut data-visite de la page. */
export const ETAPES_VISITE_AGRICULTEUR: EtapeVisite[] = [
  { cible: 'accueil', titre: 'Votre espace', texte: 'Tout part de cet accueil : vos actions principales, la prochaine collecte et vos notifications.' },
  { cible: 'declarer', titre: 'Déclarer une récolte', texte: 'Appuyez ici quand vos olives sont prêtes. Choisissez « Urgente » si la maturité est critique.' },
  { cible: 'signaler', titre: 'Signaler maturité ou problème', texte: 'Au verger, envoyez votre position GPS, une description et une photo à la coopérative.' },
  { cible: 'vergers', titre: 'Vos vergers', texte: 'Ajoutez vos vergers et placez-les sur la carte : ils servent à toutes vos demandes.' },
  { cible: 'aide', titre: 'Besoin d’aide ?', texte: 'La cloche vous prévient de chaque étape de vos collectes. L’aide répond aux questions fréquentes.' },
];

/** État de la visite guidée de l'espace agriculteur (voir {@link EtatVisiteGuidee}). */
@Injectable({ providedIn: 'root' })
export class VisiteAgriculteur extends EtatVisiteGuidee {}
