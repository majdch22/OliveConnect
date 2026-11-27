import { Component, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { NotificationsService } from '../../../core/notifications/notifications.service';
import { Logo } from '../../../shared/logo/logo';
import { BandeauNotification } from '../../../shared/notifications/bandeau-notification';
import { ClocheNotifications } from '../../../shared/notifications/cloche-notifications';
import { VisiteGuidee } from '../../../shared/visite-guidee/visite-guidee';
import { CollecteService } from '../collecte.service';
import { ETAPES_VISITE_COLLECTE, VisiteCollecte } from '../visite/visite-collecte';

/**
 * Cadre de l'espace du responsable de collecte (maquettes D1, RC1 à RC4 : version bureau).
 * Menu en haut : Accueil, Demandes, Planifier, Tournées, Alertes (pastille des signalements ouverts), Carte, Aide.
 * Sur téléphone, le menu défile horizontalement sous la barre du haut.
 */
@Component({
  selector: 'app-mise-en-page-collecte',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo, ClocheNotifications, BandeauNotification, VisiteGuidee],
  templateUrl: './mise-en-page-collecte.html',
})
export class MiseEnPageCollecte {
  readonly authentification = inject(AuthentificationService);
  readonly collecte = inject(CollecteService);
  readonly visite = inject(VisiteCollecte);
  readonly etapesVisite = ETAPES_VISITE_COLLECTE;
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationsService);
  private readonly parametres = toSignal(inject(ActivatedRoute).queryParamMap);

  readonly menu = [
    { libelle: 'Accueil', adresse: '/collecte', exact: true, visite: 'accueil' },
    { libelle: 'Demandes', adresse: '/collecte/demandes', exact: false, visite: 'demandes' },
    { libelle: 'Planifier', adresse: '/collecte/planifier', exact: false, visite: 'planifier' },
    { libelle: 'Tournées', adresse: '/collecte/tournees', exact: false, visite: 'tournees' },
    { libelle: 'Alertes', adresse: '/collecte/alertes', exact: false, visite: 'alertes', pastille: true },
    { libelle: 'Carte', adresse: '/collecte/carte', exact: false, visite: 'carte' },
    { libelle: 'Aide', adresse: '/collecte/aide', exact: false, visite: 'aide' },
  ];

  constructor() {
    this.visite.charger();
    this.collecte.rafraichirAlertesOuvertes();
    // Un nouveau signalement arrive en direct : la pastille du menu se met à jour
    effect(() => {
      if (this.notifications.derniereRecue()?.categorie === 'ALERTE') {
        this.collecte.rafraichirAlertesOuvertes();
      }
    });
    // « Revoir la visite guidée » (profil, aide) arrive ici avec ?visite=1
    effect(() => {
      if (this.parametres()?.get('visite') === '1') {
        this.visite.commencer();
        this.router.navigate([], { queryParams: { visite: null }, queryParamsHandling: 'merge', replaceUrl: true });
      }
    });
  }

  aideDeLEcran(): { depuis: string } {
    return { depuis: this.router.url.split('?')[0] };
  }
}
