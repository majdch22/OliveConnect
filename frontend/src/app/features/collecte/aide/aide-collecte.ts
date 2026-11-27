import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { GABARIT_AIDE, GuideAide, PageAide, QuestionAide } from '../../../shared/aide/page-aide';

const GUIDES: GuideAide[] = [
  {
    titre: 'Examiner une demande',
    page: '/collecte/demandes',
    lien: '/collecte/demandes',
    etapes: [
      'Ouvrez « Demandes » : l’onglet « À examiner » présente les urgentes d’abord, puis les vergers les plus mûrs.',
      'Touchez une ligne : la fiche montre le verger, la maturité mesurée, l’agriculteur et le pressoir le plus proche.',
      '« Valider la demande » l’accepte ; « Valider et planifier » ouvre directement la planification avec cette demande.',
      '« Rejeter… » demande un motif (des motifs sont proposés) : il est envoyé à l’agriculteur.',
    ],
  },
  {
    titre: 'Planifier une tournée sans conflit',
    page: '/collecte/planifier',
    lien: '/collecte/planifier',
    etapes: [
      'Cochez les demandes validées dans l’ordre où l’équipe passera dans les vergers.',
      'Choisissez la date : seuls les chefs d’équipe et les ressources libres ce jour-là sont proposés.',
      'Choisissez le chef d’équipe et le pressoir (sa capacité libre ce jour-là est affichée).',
      'Ajoutez tracteurs, bennes et une équipe d’ouvriers en un clic.',
      'Lisez le contrôle : ✖ empêche la création (ressource déjà prise…), ⚠ est un point à vérifier (bennes trop petites…).',
      '« Créer la tournée » prévient chaque agriculteur et le chef d’équipe.',
    ],
  },
  {
    titre: 'Suivre et annuler une tournée',
    page: '/collecte/tournees',
    lien: '/collecte/tournees',
    etapes: [
      'Ouvrez « Tournées » et changez de jour avec ◀ et ▶ : la liste se met à jour en direct.',
      'Touchez une tournée pour voir ses vergers dans l’ordre de passage et ses ressources.',
      '« Annuler la tournée » (motif obligatoire) n’est possible qu’avant son démarrage : ses demandes redeviennent à planifier.',
    ],
  },
  {
    titre: 'Traiter un signalement',
    page: '/collecte/alertes',
    lien: '/collecte/alertes',
    etapes: [
      'Ouvrez « Alertes » : la pastille rouge du menu compte les signalements ouverts.',
      'Écrivez l’action prévue puis « Prendre en charge » : l’agriculteur est prévenu.',
      'Maturité critique ? « Créer une demande urgente » crée une demande validée pour ce verger.',
      'Une fois le problème réglé, « Marquer résolu » avec la réponse que l’agriculteur lira.',
    ],
  },
  {
    titre: 'Lire la carte',
    page: '/collecte/carte',
    lien: '/collecte/carte',
    etapes: [
      'La couleur d’un verger indique son statut de récolte ; un cercle pointillé signale une tournée du jour.',
      'Les triangles rouges sont les signalements non résolus.',
      'Filtrez par statut, variété ou maturité minimale ; la liste sous la carte reprend les mêmes vergers.',
    ],
  },
];

const QUESTIONS: QuestionAide[] = [
  {
    question: 'Pourquoi une ressource n’apparaît pas dans la planification ?',
    reponse: 'Elle est déjà affectée à une autre tournée ce jour-là, ou la logistique l’a marquée en panne, en révision ou en congé. Une ressource ne sert qu’à une tournée par jour.',
  },
  {
    question: 'Pourquoi un chef d’équipe n’est pas proposé ?',
    reponse: 'Il a déjà une tournée ce jour-là : un chef d’équipe n’a qu’une seule tournée par jour.',
  },
  {
    question: 'Que se passe-t-il si les bennes sont trop petites ?',
    reponse: 'C’est un avertissement : la tournée peut être créée, mais ajoutez une benne si possible pour transporter toute la récolte estimée.',
  },
  {
    question: 'Le pressoir est complet ce jour-là',
    reponse: 'Le contrôle propose les pressoirs qui ont encore la capacité nécessaire, du plus proche au plus éloigné du premier verger. Touchez-en un pour le choisir.',
  },
  {
    question: 'Un agriculteur a annulé une demande déjà planifiée',
    reponse: 'Son verger est retiré de la tournée et le chef d’équipe est prévenu. Une tournée qui n’a plus aucun verger est annulée d’elle-même.',
  },
  {
    question: 'Deux responsables traitent la même demande',
    reponse: 'Le premier enregistrement compte ; le second reçoit le message « déjà traitée » et la liste se met à jour.',
  },
];

/** Page /collecte/aide : guides et questions du responsable de collecte. */
@Component({
  selector: 'app-aide-collecte',
  imports: [RouterLink],
  template: GABARIT_AIDE,
})
export class AideCollecte extends PageAide {
  protected readonly tousLesGuides = GUIDES;
  protected readonly toutesLesQuestions = QUESTIONS;
  protected readonly accueil = '/collecte';
  readonly titreContact = 'Une règle bloque votre travail ?';
  readonly texteContact = 'Les dates de campagne et la tolérance de pesée se règlent par l’administrateur ; l’état des ressources par le responsable logistique.';
}
