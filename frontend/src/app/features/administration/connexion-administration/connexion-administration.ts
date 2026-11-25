import { Component } from '@angular/core';

import { FormulaireConnexion } from '../../../shared/formulaire-connexion/formulaire-connexion';
import { Logo } from '../../../shared/logo/logo';

/**
 * Page /administration/connexion : réservée à l'administrateur.
 * Adresse séparée, non liée depuis l'accueil ; le serveur refuse ici tout compte qui n'est pas administrateur.
 * Fond sombre pour la distinguer clairement de la connexion générale.
 */
@Component({
  selector: 'app-connexion-administration',
  imports: [FormulaireConnexion, Logo],
  template: `
    <div class="flex min-h-screen flex-col bg-olive-900 bg-[radial-gradient(ellipse_at_top,_#5b7a1e_0%,_#2f3e12_60%)]">
      <header class="px-6 py-6 lg:px-12">
        <app-logo [clair]="true" />
      </header>

      <main class="flex flex-1 items-center justify-center px-6 pb-16">
        <div class="w-full max-w-md rounded-2xl bg-white p-8 shadow-2xl lg:p-10">
          <div class="flex items-center gap-3">
            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-olive-100" aria-hidden="true">
              <svg viewBox="0 0 24 24" class="h-6 w-6 text-olive-900" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="5" y="11" width="14" height="10" rx="2" />
                <path d="M8 11V7a4 4 0 0 1 8 0v4" />
              </svg>
            </span>
            <div>
              <h1 class="text-2xl font-bold text-olive-900">Espace administrateur</h1>
              <p class="text-sm text-gray-600">Accès réservé à l'administration de la plateforme</p>
            </div>
          </div>
          <div class="mt-8">
            <app-formulaire-connexion espace="ADMINISTRATION" />
          </div>
        </div>
      </main>
    </div>
  `,
})
export class ConnexionAdministration {}
