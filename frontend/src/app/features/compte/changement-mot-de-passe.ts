import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthentificationService } from '../../core/authentification/authentification.service';
import { ESPACE_PAR_ROLE } from '../../core/authentification/modeles';
import { FormulaireMotDePasse } from '../../shared/mot-de-passe/formulaire-mot-de-passe';
import { Logo } from '../../shared/logo/logo';

/**
 * Page /mot-de-passe : affichée d'office après une connexion avec un mot de passe provisoire
 * (création du compte ou réinitialisation par l'administrateur). Une fois changé, la personne arrive dans son espace.
 */
@Component({
  selector: 'app-changement-mot-de-passe',
  imports: [FormulaireMotDePasse, Logo],
  template: `
    <div class="flex min-h-screen flex-col bg-olive-50">
      <header class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
        <app-logo />
        <button type="button" class="bouton-secondaire px-4 py-2 text-sm" (click)="authentification.deconnecter()">Se déconnecter</button>
      </header>
      <main class="flex flex-1 items-center justify-center px-4 py-10">
        <div class="w-full max-w-xl rounded-2xl bg-white p-8 shadow-sm">
          <h1 class="text-2xl font-bold text-olive-900">Choisissez votre mot de passe</h1>
          <p class="mt-2 text-gray-600">
            Bonjour {{ authentification.session()?.prenom }}. Le mot de passe que vous avez reçu est provisoire :
            choisissez le vôtre pour accéder à votre espace.
          </p>
          <div class="mt-6">
            <app-formulaire-mot-de-passe (change)="allerDansMonEspace()" />
          </div>
        </div>
      </main>
    </div>
  `,
})
export class ChangementMotDePasse {
  readonly authentification = inject(AuthentificationService);
  private readonly router = inject(Router);

  allerDansMonEspace(): void {
    const session = this.authentification.session();
    if (session) {
      this.router.navigateByUrl(ESPACE_PAR_ROLE[session.role]);
    }
  }
}
