import { Injectable } from '@angular/core';

import { EtatVisiteGuidee } from '../../../shared/visite-guidee/etat-visite-guidee';
import { EtapeVisite } from '../../../shared/visite-guidee/visite-guidee';

/** Les 5 étapes de la visite de l'espace du responsable de collecte ; chaque cible est un attribut data-visite. */
export const ETAPES_VISITE_COLLECTE: EtapeVisite[] = [
  { cible: 'accueil', titre: 'Votre tableau de bord', texte: 'Les demandes à examiner, les urgences, les tournées du jour et la charge des pressoirs, d’un coup d’œil.' },
  { cible: 'demandes', titre: 'Examiner les demandes', texte: 'Les urgentes et les vergers les plus mûrs arrivent en premier. Validez ou rejetez avec un motif.' },
  { cible: 'planifier', titre: 'Planifier une tournée', texte: 'Choisissez des demandes validées, une date, un chef d’équipe, un pressoir et les ressources libres ce jour-là.' },
  { cible: 'alertes', titre: 'Traiter les signalements', texte: 'Prenez en charge, répondez à l’agriculteur, ou créez directement une récolte urgente.' },
  { cible: 'aide', titre: 'Besoin d’aide ?', texte: 'La cloche vous prévient de chaque nouvelle demande et de chaque signalement. L’aide explique les règles de planification.' },
];

/** État de la visite guidée de l'espace du responsable de collecte. */
@Injectable({ providedIn: 'root' })
export class VisiteCollecte extends EtatVisiteGuidee {}
