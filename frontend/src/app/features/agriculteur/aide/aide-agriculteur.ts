import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { GABARIT_AIDE, GuideAide, PageAide, QuestionAide } from '../../../shared/aide/page-aide';

const GUIDES: GuideAide[] = [
  {
    titre: 'Déclarer une récolte en 1 minute',
    page: '/agriculteur/demandes/nouvelle',
    lien: '/agriculteur/demandes/nouvelle',
    etapes: [
      'Touchez « Déclarer une récolte » sur l’accueil.',
      'Choisissez le verger, puis « Planifiée » (à partir de demain) ou « Urgente » (dès aujourd’hui, traitée en priorité).',
      'Choisissez la date et la quantité : touchez la suggestion pour reprendre l’estimation de votre verger.',
      'Envoyez : la coopérative est prévenue tout de suite.',
    ],
  },
  {
    titre: 'Signaler la maturité ou un problème avec le GPS',
    page: '/agriculteur/signalements',
    lien: '/agriculteur/signalements',
    etapes: [
      'Au verger, touchez « Signaler maturité ou problème ».',
      'Choisissez le type (maturité, pluie, ravageur…) et le verger.',
      'Touchez « Ma position » : le téléphone indique où vous êtes. Sans GPS, touchez la carte.',
      'Décrivez ce que vous voyez et ajoutez une photo si vous le souhaitez.',
    ],
  },
  {
    titre: 'Modifier ou annuler une collecte',
    page: '/agriculteur/demandes',
    lien: '/agriculteur/demandes',
    etapes: [
      'Ouvrez « Demandes » et touchez la demande.',
      '« Modifier » change la date ou la quantité ; « Annuler la demande » la retire (le motif est facultatif).',
      'C’est possible tant que la collecte n’a pas démarré.',
    ],
  },
  {
    titre: 'Ajouter et placer un verger',
    page: '/agriculteur/vergers',
    lien: '/agriculteur/vergers',
    etapes: [
      'Ouvrez « Vergers » puis « Ajouter un verger ».',
      'Touchez la carte au milieu du verger, ou « Ma position » si vous y êtes.',
      'Indiquez la variété, la superficie et le nombre d’arbres.',
    ],
  },
  {
    titre: 'Comprendre les statuts',
    page: '',
    lien: '/agriculteur/demandes',
    etapes: [
      'Soumise : la coopérative a reçu votre demande.',
      'Validée : elle est acceptée, la tournée reste à organiser.',
      'Planifiée : une date, une équipe et un pressoir sont choisis.',
      'En cours : l’équipe récolte ; la demande ne peut plus changer.',
      'Terminée : les olives sont pesées au pressoir.',
    ],
  },
];

const QUESTIONS: QuestionAide[] = [
  {
    question: 'Pourquoi je ne peux plus annuler ?',
    reponse: 'Une collecte démarrée ne peut plus être modifiée. Contactez le responsable de collecte de la coopérative.',
  },
  {
    question: 'Le GPS ne fonctionne pas',
    reponse: 'Autorisez la localisation pour OliveConnect dans les réglages du téléphone, ou touchez simplement la carte pour placer le point.',
  },
  {
    question: 'Mon signalement est marqué « loin du verger »',
    reponse: 'La position envoyée est éloignée du verger choisi. Il est bien reçu : la coopérative le vérifiera.',
  },
  {
    question: 'J’ai oublié mon mot de passe',
    reponse: 'Sur la page de connexion, touchez « Mot de passe oublié ? » : un code vous est envoyé par SMS.',
  },
  {
    question: 'Je ne reçois plus de SMS',
    reponse: 'Vérifiez dans « Mon profil » que « SMS pour les collectes » est activé et que votre numéro est le bon.',
  },
];

/** Page /agriculteur/aide (maquette P5) : guides et questions de l'agriculteur. */
@Component({
  selector: 'app-aide-agriculteur',
  imports: [RouterLink],
  template: GABARIT_AIDE,
})
export class AideAgriculteur extends PageAide {
  protected readonly tousLesGuides = GUIDES;
  protected readonly toutesLesQuestions = QUESTIONS;
  protected readonly accueil = '/agriculteur';
  readonly titreContact = 'Contacter la coopérative';
  readonly texteContact = 'Pour une collecte déjà démarrée ou une information à corriger, adressez-vous au bureau de la coopérative.';
}
