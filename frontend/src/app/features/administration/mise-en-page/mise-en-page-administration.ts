import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { Logo } from '../../../shared/logo/logo';
import { BandeauNotification } from '../../../shared/notifications/bandeau-notification';
import { ClocheNotifications } from '../../../shared/notifications/cloche-notifications';
import { AdministrationService } from '../administration.service';

/**
 * Cadre commun à toutes les pages de l'administration.
 * Ordinateur : menu fixe à gauche. Téléphone : menu en haut, défilant horizontalement.
 * La cloche ouvre les notifications (nouvelles inscriptions…) ; un bandeau montre celles qui arrivent en direct.
 */
@Component({
  selector: 'app-mise-en-page-administration',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo, ClocheNotifications, BandeauNotification],
  templateUrl: './mise-en-page-administration.html',
})
export class MiseEnPageAdministration implements OnInit {
  readonly authentification = inject(AuthentificationService);
  readonly administration = inject(AdministrationService);

  /** Entrées du menu. « exact » : l'accueil n'est actif que sur sa propre adresse. */
  readonly menu = [
    { libelle: 'Accueil', adresse: '/administration', exact: true, badge: false },
    { libelle: 'Inscriptions', adresse: '/administration/inscriptions', exact: false, badge: true },
    { libelle: 'Utilisateurs', adresse: '/administration/utilisateurs', exact: false, badge: false },
    { libelle: 'Ressources', adresse: '/administration/ressources', exact: false, badge: false },
    { libelle: 'Carte', adresse: '/administration/carte', exact: false, badge: false },
    { libelle: 'Référentiels', adresse: '/administration/referentiels', exact: false, badge: false },
    { libelle: 'Paramètres', adresse: '/administration/parametres', exact: false, badge: false },
    { libelle: 'Supervision', adresse: '/administration/supervision', exact: false, badge: false },
  ];

  ngOnInit(): void {
    this.administration.rafraichirStatistiques();
  }
}
