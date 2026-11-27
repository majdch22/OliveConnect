import { Component, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, of } from 'rxjs';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { Logo } from '../../../shared/logo/logo';
import { BandeauNotification } from '../../../shared/notifications/bandeau-notification';
import { ClocheNotifications } from '../../../shared/notifications/cloche-notifications';
import { VisiteGuidee } from '../../../shared/visite-guidee/visite-guidee';
import { PressoirService } from '../pressoir.service';
import { ETAPES_VISITE_PRESSOIR, VisitePressoir } from '../visite/visite-pressoir';

/**
 * Cadre de l'espace du responsable du pressoir (maquettes M7 et PR1).
 * La réception se fait souvent depuis un téléphone au pont-bascule : barre d'onglets en bas sur téléphone,
 * menu en haut sur ordinateur. Le nom du pressoir du compte est rappelé en haut.
 */
@Component({
  selector: 'app-mise-en-page-pressoir',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo, ClocheNotifications, BandeauNotification, VisiteGuidee],
  templateUrl: './mise-en-page-pressoir.html',
})
export class MiseEnPagePressoir {
  readonly authentification = inject(AuthentificationService);
  readonly visite = inject(VisitePressoir);
  readonly etapesVisite = ETAPES_VISITE_PRESSOIR;
  private readonly router = inject(Router);
  private readonly parametres = toSignal(inject(ActivatedRoute).queryParamMap);
  readonly monPressoir = toSignal(inject(PressoirService).monPressoir().pipe(catchError(() => of(null))), { initialValue: null });

  readonly menu = [
    { libelle: 'Réception', adresse: '/pressoir', exact: true, visite: 'reception', icone: 'M3 17h18M5 17V9l7-5 7 5v8M9 17v-5h6v5' },
    { libelle: 'Historique', adresse: '/pressoir/historique', exact: false, visite: 'historique', icone: 'M12 8v4l3 2M21 12a9 9 0 1 1-3-6.7M21 4v4h-4' },
    { libelle: 'Capacité', adresse: '/pressoir/capacite', exact: false, visite: 'capacite', icone: 'M4 20V10M10 20V4M16 20v-7M22 20H2' },
    { libelle: 'Tableau de bord', adresse: '/pressoir/tableau-de-bord', exact: false, visite: 'tableau', icone: 'M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z' },
    { libelle: 'Aide', adresse: '/pressoir/aide', exact: false, visite: 'aide', icone: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6M12 17h.01' },
  ];

  constructor() {
    this.visite.charger();
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
