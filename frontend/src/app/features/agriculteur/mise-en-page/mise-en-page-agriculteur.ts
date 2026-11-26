import { Component, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { Logo } from '../../../shared/logo/logo';
import { BandeauNotification } from '../../../shared/notifications/bandeau-notification';
import { ClocheNotifications } from '../../../shared/notifications/cloche-notifications';
import { VisiteGuidee } from '../../../shared/visite-guidee/visite-guidee';
import { ETAPES_VISITE_AGRICULTEUR, VisiteAgriculteur } from '../visite/visite-agriculteur';

/**
 * Cadre de l'espace agriculteur.
 * Ordinateur : menu en haut. Téléphone : barre d'onglets en bas de l'écran, facile à atteindre avec le pouce au verger.
 * En haut à droite : cloche des notifications, aide de l'écran (« ? »), profil et déconnexion.
 * Le cadre affiche aussi le bandeau des notifications en direct et la visite guidée.
 */
@Component({
  selector: 'app-mise-en-page-agriculteur',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo, ClocheNotifications, BandeauNotification, VisiteGuidee],
  templateUrl: './mise-en-page-agriculteur.html',
})
export class MiseEnPageAgriculteur {
  readonly authentification = inject(AuthentificationService);
  readonly visite = inject(VisiteAgriculteur);
  readonly etapesVisite = ETAPES_VISITE_AGRICULTEUR;
  private readonly router = inject(Router);
  private readonly parametres = toSignal(inject(ActivatedRoute).queryParamMap);

  /** Entrées du menu ; l'icône est un tracé SVG simple. « exact » : l'accueil n'est actif que sur sa propre adresse. */
  readonly menu = [
    { libelle: 'Accueil', adresse: '/agriculteur', exact: true, visite: 'accueil', icone: 'M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z' },
    { libelle: 'Vergers', adresse: '/agriculteur/vergers', exact: false, visite: 'vergers', icone: 'M12 3c4 3 6 6 6 9a6 6 0 0 1-12 0c0-3 2-6 6-9zM12 21v-6' },
    { libelle: 'Demandes', adresse: '/agriculteur/demandes', exact: false, visite: 'demandes', icone: 'M6 3h9l4 4v14H6zM9 12h7M9 16h7M14 3v5h5' },
    { libelle: 'Signalements', adresse: '/agriculteur/signalements', exact: false, visite: 'signalements', icone: 'M12 3 2 20h20zM12 10v4M12 17h.01' },
    { libelle: 'Aide', adresse: '/agriculteur/aide', exact: false, visite: 'aide', icone: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6M12 17h.01' },
  ];

  constructor() {
    this.visite.charger();
    // « Revoir la visite guidée » (profil, aide) arrive ici avec ?visite=1 : on la lance et on nettoie l'adresse
    effect(() => {
      if (this.parametres()?.get('visite') === '1') {
        this.visite.commencer();
        this.router.navigate([], { queryParams: { visite: null }, queryParamsHandling: 'merge', replaceUrl: true });
      }
    });
  }

  /** Adresse de l'aide, avec la page actuelle pour proposer l'aide de cet écran en premier. */
  aideDeLEcran(): { depuis: string } {
    return { depuis: this.router.url.split('?')[0] };
  }
}
