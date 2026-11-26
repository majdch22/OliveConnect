import { Routes } from '@angular/router';

import { connecteGuard, dejaConnecteGuard, roleGuard } from './core/authentification/role.guard';
import { ConnexionAdministration } from './features/administration/connexion-administration/connexion-administration';
import { PageErreur } from './features/erreurs/page-erreur';
import { EspaceProvisoire } from './features/espaces/espace-provisoire';
import { Accueil } from './features/public/accueil/accueil';
import { Connexion } from './features/public/connexion/connexion';
import { Inscription } from './features/public/inscription/inscription';
import { InscriptionEnvoyee } from './features/public/inscription/inscription-envoyee';

/** Adresses de l'application. Chaque espace n'est accessible qu'au rôle indiqué. */
export const routes: Routes = [
  // Pages publiques
  { path: '', component: Accueil, title: 'OliveConnect' },
  { path: 'connexion', component: Connexion, canActivate: [dejaConnecteGuard], title: 'Connexion · OliveConnect' },
  { path: 'inscription', component: Inscription, canActivate: [dejaConnecteGuard], title: 'Inscription · OliveConnect' },
  { path: 'inscription/envoyee', component: InscriptionEnvoyee, title: 'Demande envoyée · OliveConnect' },
  {
    path: 'carte',
    title: 'Carte des vergers · OliveConnect',
    loadComponent: () => import('./features/public/carte-publique/carte-publique').then((m) => m.CartePublique),
  },
  {
    path: 'mot-de-passe-oublie',
    title: 'Mot de passe oublié · OliveConnect',
    canActivate: [dejaConnecteGuard],
    loadComponent: () => import('./features/public/mot-de-passe-oublie/mot-de-passe-oublie').then((m) => m.MotDePasseOublie),
  },
  {
    path: 'administration/connexion',
    component: ConnexionAdministration,
    canActivate: [dejaConnecteGuard],
    title: 'Espace administrateur · OliveConnect',
  },

  // Espace de l'administrateur : menu commun et ses pages.
  // Chargées seulement quand un administrateur les ouvre : les autres visiteurs ne téléchargent pas ce code.
  {
    path: 'administration',
    loadComponent: () =>
      import('./features/administration/mise-en-page/mise-en-page-administration').then((m) => m.MiseEnPageAdministration),
    canActivate: [roleGuard('ADMINISTRATEUR')],
    children: [
      {
        path: '',
        title: 'Administration · OliveConnect',
        loadComponent: () =>
          import('./features/administration/accueil/accueil-administration').then((m) => m.AccueilAdministration),
      },
      {
        path: 'inscriptions',
        title: 'Inscriptions · Administration',
        loadComponent: () =>
          import('./features/administration/inscriptions/inscriptions-administration').then((m) => m.InscriptionsAdministration),
      },
      {
        path: 'utilisateurs',
        title: 'Utilisateurs · Administration',
        loadComponent: () =>
          import('./features/administration/utilisateurs/utilisateurs-administration').then((m) => m.UtilisateursAdministration),
      },
      {
        path: 'ressources',
        title: 'Ressources · Administration',
        loadComponent: () =>
          import('./features/administration/ressources/ressources-administration').then((m) => m.RessourcesAdministration),
      },
      {
        path: 'carte',
        title: 'Carte · Administration',
        loadComponent: () => import('./features/administration/carte/carte-administration').then((m) => m.CarteAdministration),
      },
      {
        path: 'referentiels',
        title: 'Référentiels · Administration',
        loadComponent: () =>
          import('./features/administration/referentiels/referentiels-administration').then((m) => m.ReferentielsAdministration),
      },
      {
        path: 'parametres',
        title: 'Paramètres · Administration',
        loadComponent: () =>
          import('./features/administration/parametres/parametres-administration').then((m) => m.ParametresAdministration),
      },
      {
        path: 'supervision',
        title: 'Supervision · Administration',
        loadComponent: () =>
          import('./features/administration/supervision/supervision-administration').then((m) => m.SupervisionAdministration),
      },
      { path: 'notifications', title: 'Notifications · Administration', loadComponent: () => import('./features/compte/notifications/page-notifications').then((m) => m.PageNotifications), },
      { path: 'profil', title: 'Mon profil · Administration', loadComponent: () => import('./features/compte/profil/page-profil').then((m) => m.PageProfil), },
    ],
  },

  // Espace de l'agriculteur : menu commun (barre d'onglets sur téléphone) et ses pages, chargées à la demande
  {
    path: 'agriculteur',
    loadComponent: () =>
      import('./features/agriculteur/mise-en-page/mise-en-page-agriculteur').then((m) => m.MiseEnPageAgriculteur),
    canActivate: [roleGuard('AGRICULTEUR')],
    children: [
      {
        path: '',
        title: 'Mon espace · OliveConnect',
        loadComponent: () => import('./features/agriculteur/accueil/accueil-agriculteur').then((m) => m.AccueilAgriculteur),
      },
      {
        path: 'vergers',
        title: 'Mes vergers · OliveConnect',
        loadComponent: () => import('./features/agriculteur/vergers/vergers-agriculteur').then((m) => m.VergersAgriculteur),
      },
      {
        path: 'demandes',
        title: 'Mes demandes · OliveConnect',
        loadComponent: () => import('./features/agriculteur/demandes/demandes-agriculteur').then((m) => m.DemandesAgriculteur),
      },
      {
        path: 'demandes/nouvelle',
        title: 'Déclarer une récolte · OliveConnect',
        loadComponent: () => import('./features/agriculteur/demandes/declarer-recolte').then((m) => m.DeclarerRecolte),
      },
      {
        path: 'signalements',
        title: 'Mes signalements · OliveConnect',
        loadComponent: () =>
          import('./features/agriculteur/signalements/signalements-agriculteur').then((m) => m.SignalementsAgriculteur),
      },
      { path: 'compte', title: 'Mon profil · OliveConnect', loadComponent: () => import('./features/compte/profil/page-profil').then((m) => m.PageProfil), },
      { path: 'notifications', title: 'Notifications · OliveConnect', loadComponent: () => import('./features/compte/notifications/page-notifications').then((m) => m.PageNotifications), },
      {
        path: 'aide',
        title: 'Aide · OliveConnect',
        loadComponent: () => import('./features/agriculteur/aide/aide-agriculteur').then((m) => m.AideAgriculteur),
      },
    ],
  },

  // Changement obligatoire d'un mot de passe provisoire (tous les rôles)
  {
    path: 'mot-de-passe',
    title: 'Choisir mon mot de passe · OliveConnect',
    canActivate: [connecteGuard],
    loadComponent: () => import('./features/compte/changement-mot-de-passe').then((m) => m.ChangementMotDePasse),
  },

  // Espaces des autres rôles (provisoires, remplacés au fil des fonctionnalités)
  { path: 'collecte', component: EspaceProvisoire, canActivate: [roleGuard('RESPONSABLE_COLLECTE')] },
  { path: 'chef-equipe', component: EspaceProvisoire, canActivate: [roleGuard('CHEF_EQUIPE')] },
  { path: 'logistique', component: EspaceProvisoire, canActivate: [roleGuard('RESPONSABLE_LOGISTIQUE')] },
  { path: 'pressoir', component: EspaceProvisoire, canActivate: [roleGuard('RESPONSABLE_PRESSOIR')] },

  // Pages d'erreur : page réservée à un autre profil, adresse inconnue
  { path: 'acces-refuse', component: PageErreur, data: { type: 'acces-refuse' }, title: 'Accès refusé · OliveConnect' },
  { path: '**', component: PageErreur, data: { type: 'introuvable' }, title: 'Page introuvable · OliveConnect' },
];
