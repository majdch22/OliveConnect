import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { GABARIT_AIDE, GuideAide, PageAide, QuestionAide } from '../../../shared/aide/page-aide';

const GUIDES: GuideAide[] = [
  {
    titre: 'Réceptionner une benne',
    page: '/pressoir',
    lien: '/pressoir',
    etapes: [
      'Sur « Réception », choisissez la tournée arrivée : seules celles démarrées par leur chef d’équipe sont proposées.',
      'Touchez la benne pesée (une benne déjà reçue est grisée).',
      'Saisissez le poids net : le taux de remplissage s’affiche.',
      'Saisissez l’acidité : la catégorie de qualité s’affiche.',
      '« Confirmer la réception ». À la dernière benne, la tournée est clôturée et chaque agriculteur reçoit son résultat.',
    ],
  },
  {
    titre: 'Publier la capacité d’un jour',
    page: '/pressoir/capacite',
    lien: '/pressoir/capacite',
    etapes: [
      'Ouvrez « Capacité » et touchez le jour à changer (aujourd’hui ou à venir).',
      'Indiquez la capacité de traitement en kilos et, si vous voulez, le motif (maintenance…).',
      '« Publier » : la planification des tournées l’utilise aussitôt.',
    ],
  },
  {
    titre: 'Retrouver et exporter des pesées',
    page: '/pressoir/historique',
    lien: '/pressoir/historique',
    etapes: [
      'Ouvrez « Historique » et choisissez la période (aujourd’hui, 7 jours, 30 jours ou des dates).',
      'Touchez une ligne pour voir la part de chaque verger.',
      '« Exporter la période » enregistre un fichier CSV qui s’ouvre dans un tableur.',
    ],
  },
  {
    titre: 'Comprendre les catégories de qualité',
    page: '/pressoir/tableau-de-bord',
    lien: '/pressoir/tableau-de-bord',
    etapes: [
      'Extra vierge : acidité jusqu’à 0,8 %.',
      'Vierge : jusqu’à 2 %.',
      'Courante : jusqu’à 3,3 %.',
      'Lampante : au-delà de 3,3 %, huile à raffiner.',
    ],
  },
];

const QUESTIONS: QuestionAide[] = [
  {
    question: 'Pourquoi ma pesée est refusée ?',
    reponse: 'Le poids dépasse 110 % de la capacité de la benne : la pesée est jugée incohérente. Vérifiez la tare puis pesez de nouveau.',
  },
  {
    question: 'La tournée arrivée n’apparaît pas',
    reponse: 'Elle n’a pas encore été démarrée par son chef d’équipe, ou elle se rend à un autre pressoir. Contactez le responsable de collecte.',
  },
  {
    question: 'Comment le poids est-il partagé entre les agriculteurs ?',
    reponse: 'Le poids de chaque benne est réparti entre les vergers de la tournée, au prorata des quantités prévues : chaque kilo n’est compté qu’une fois.',
  },
  {
    question: 'Une benne fait un deuxième voyage',
    reponse: 'Une benne n’est reçue qu’une fois par tournée. Pour un deuxième voyage, le responsable de collecte planifie une autre tournée.',
  },
  {
    question: 'Que signifie « encore attendu » ?',
    reponse: 'C’est ce que les tournées du jour doivent encore apporter : leur quantité prévue moins ce qui a déjà été pesé.',
  },
];

/** Page /pressoir/aide : guides et questions du responsable du pressoir. */
@Component({
  selector: 'app-aide-pressoir',
  imports: [RouterLink],
  template: GABARIT_AIDE,
})
export class AidePressoir extends PageAide {
  protected readonly tousLesGuides = GUIDES;
  protected readonly toutesLesQuestions = QUESTIONS;
  protected readonly accueil = '/pressoir';
  readonly titreContact = 'Un problème avec une tournée ?';
  readonly texteContact = 'Pour une tournée qui n’arrive pas ou une benne manquante, contactez le responsable de collecte de la coopérative.';
}
