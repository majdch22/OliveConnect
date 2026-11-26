import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { Logo } from '../../../shared/logo/logo';

/**
 * Cadre de l'espace agriculteur.
 * Ordinateur : menu en haut. Téléphone : barre d'onglets en bas de l'écran, facile à atteindre avec le pouce au verger.
 */
@Component({
  selector: 'app-mise-en-page-agriculteur',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo],
  templateUrl: './mise-en-page-agriculteur.html',
})
export class MiseEnPageAgriculteur {
  readonly authentification = inject(AuthentificationService);

  /** Entrées du menu ; l'icône est un tracé SVG simple. « exact » : l'accueil n'est actif que sur sa propre adresse. */
  readonly menu = [
    { libelle: 'Accueil', adresse: '/agriculteur', exact: true, icone: 'M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z' },
    { libelle: 'Vergers', adresse: '/agriculteur/vergers', exact: false, icone: 'M12 3c4 3 6 6 6 9a6 6 0 0 1-12 0c0-3 2-6 6-9zM12 21v-6' },
    { libelle: 'Demandes', adresse: '/agriculteur/demandes', exact: false, icone: 'M6 3h9l4 4v14H6zM9 12h7M9 16h7M14 3v5h5' },
    { libelle: 'Signalements', adresse: '/agriculteur/signalements', exact: false, icone: 'M12 3 2 20h20zM12 10v4M12 17h.01' },
    { libelle: 'Mon compte', adresse: '/agriculteur/compte', exact: false, icone: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21c1-4 4-6 8-6s7 2 8 6' },
  ];
}
