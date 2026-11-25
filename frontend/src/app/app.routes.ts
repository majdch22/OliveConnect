import { Routes } from '@angular/router';

import { dejaConnecteGuard, roleGuard } from './core/authentification/role.guard';
import { ConnexionAdministration } from './features/administration/connexion-administration/connexion-administration';
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
    path: 'administration/connexion',
    component: ConnexionAdministration,
    canActivate: [dejaConnecteGuard],
    title: 'Espace administrateur · OliveConnect',
  },

  // Espaces par rôle (provisoires, remplacés au fil des fonctionnalités)
  { path: 'agriculteur', component: EspaceProvisoire, canActivate: [roleGuard('AGRICULTEUR')] },
  { path: 'collecte', component: EspaceProvisoire, canActivate: [roleGuard('RESPONSABLE_COLLECTE')] },
  { path: 'chef-equipe', component: EspaceProvisoire, canActivate: [roleGuard('CHEF_EQUIPE')] },
  { path: 'logistique', component: EspaceProvisoire, canActivate: [roleGuard('RESPONSABLE_LOGISTIQUE')] },
  { path: 'pressoir', component: EspaceProvisoire, canActivate: [roleGuard('RESPONSABLE_PRESSOIR')] },
  { path: 'administration', component: EspaceProvisoire, canActivate: [roleGuard('ADMINISTRATEUR')] },

  // Toute autre adresse ramène à l'accueil
  { path: '**', redirectTo: '' },
];
