import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Logo } from '../../../shared/logo/logo';

/** Page d'accueil publique : présentation de la coopérative et accès à la connexion. */
@Component({
  selector: 'app-accueil',
  imports: [RouterLink, Logo],
  templateUrl: './accueil.html',
})
export class Accueil {
  /** Engagements de la plateforme, affichés sous la présentation. */
  readonly atouts = [
    { titre: 'Déclaration rapide', texte: 'Une récolte déclarée en quatre saisies, depuis le verger.' },
    { titre: 'Suivi en temps réel', texte: 'Chaque étape de la collecte notifiée à l\'agriculteur.' },
    { titre: 'Planification sans conflit', texte: 'Ouvriers, tracteurs et bennes affectés sans double réservation.' },
    { titre: 'Qualité tracée', texte: 'Pesée, acidité et catégorie enregistrées au pressoir.' },
  ];

  /** Les trois étapes du parcours d'une récolte. */
  readonly etapes = [
    {
      titre: 'Je déclare ma récolte',
      texte: 'L\'agriculteur choisit son verger, une date et une quantité estimée. Il peut signaler la maturité ou un problème avec sa position GPS.',
    },
    {
      titre: 'La coopérative planifie',
      texte: 'Le responsable de collecte organise la tournée selon la maturité des vergers et la disponibilité des équipes, tracteurs et bennes.',
    },
    {
      titre: 'Je suis la collecte',
      texte: 'L\'agriculteur suit l\'avancement de la récolte, puis reçoit le poids et la qualité mesurés au pressoir.',
    },
  ];

  /** Les profils qui utilisent la plateforme. */
  readonly profils = [
    { titre: 'Agriculteurs', texte: 'Déclarent leurs récoltes, signalent la maturité et suivent leurs collectes.' },
    { titre: 'Responsables de collecte', texte: 'Valident les demandes, planifient les tournées et traitent les alertes.' },
    { titre: 'Chefs d\'équipe', texte: 'Pilotent la récolte sur le terrain et livrent au pressoir.' },
    { titre: 'Logistique', texte: 'Gèrent les ouvriers saisonniers, les tracteurs, les bennes et le matériel.' },
    { titre: 'Pressoirs', texte: 'Réceptionnent les bennes, pèsent et notent la qualité des olives.' },
  ];

  /** Statuts d'une demande, affichés dans l'aperçu de l'application. */
  readonly statutsApercu = ['Soumise', 'Validée', 'Planifiée', 'En cours', 'Terminée'];
}
