import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FormulaireConnexion } from '../../../shared/formulaire-connexion/formulaire-connexion';
import { Logo } from '../../../shared/logo/logo';

/**
 * Page /connexion : agriculteurs et personnel de la coopérative (pas l'administrateur).
 * Sur ordinateur : écran partagé (présentation à gauche, formulaire à droite).
 * Sur téléphone : la présentation devient un bandeau en haut.
 */
@Component({
  selector: 'app-connexion',
  imports: [FormulaireConnexion, RouterLink, Logo],
  templateUrl: './connexion.html',
})
export class Connexion {
  readonly avantages = [
    'Déclarez vos récoltes et suivez leur avancement',
    'Recevez les notifications de chaque étape',
    'Consultez la pesée et la qualité au pressoir',
  ];
}
