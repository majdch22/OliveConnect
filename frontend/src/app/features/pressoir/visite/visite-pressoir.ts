import { Injectable } from '@angular/core';

import { EtatVisiteGuidee } from '../../../shared/visite-guidee/etat-visite-guidee';
import { EtapeVisite } from '../../../shared/visite-guidee/visite-guidee';

/** Les 5 étapes de la visite de l'espace du responsable du pressoir ; chaque cible est un attribut data-visite. */
export const ETAPES_VISITE_PRESSOIR: EtapeVisite[] = [
  { cible: 'reception', titre: 'Réceptionner une benne', texte: 'Choisissez la tournée arrivée, la benne, puis saisissez le poids net et l’acidité. La catégorie est calculée pour vous.' },
  { cible: 'historique', titre: 'Historique', texte: 'Toutes les pesées, par jour ou par période, exportables pour un tableur.' },
  { cible: 'capacite', titre: 'Publier la capacité', texte: 'Maintenance, panne ou journée renforcée : la capacité publiée est aussitôt utilisée par la planification des tournées.' },
  { cible: 'tableau', titre: 'Tableau de bord', texte: 'Volumes reçus, acidité moyenne, remplissage des bennes et qualité par variété.' },
  { cible: 'aide', titre: 'Besoin d’aide ?', texte: 'La cloche vous prévient des tournées qui arrivent. L’aide rappelle la règle des 110 % et les catégories.' },
];

/** État de la visite guidée de l'espace du responsable du pressoir. */
@Injectable({ providedIn: 'root' })
export class VisitePressoir extends EtatVisiteGuidee {}
