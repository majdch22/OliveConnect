import { Component, inject } from '@angular/core';

import { AuthentificationService } from '../../core/authentification/authentification.service';

/**
 * Espace provisoire affiché après connexion, en attendant les écrans de chaque rôle.
 * Il prouve que la connexion, le rôle et la protection des pages fonctionnent.
 */
@Component({
  selector: 'app-espace-provisoire',
  template: `
    @if (authentification.session(); as session) {
      <div class="min-h-screen bg-olive-50">
        <header class="bg-olive-700 text-white">
          <div class="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
            <span class="text-lg font-bold">OliveConnect</span>
            <button
              type="button"
              (click)="authentification.deconnecter()"
              class="rounded-lg bg-white px-4 py-2 text-sm font-semibold text-olive-900 hover:bg-olive-100"
            >
              Se déconnecter
            </button>
          </div>
        </header>
        <main class="mx-auto max-w-5xl px-4 py-10">
          <h1 class="text-2xl font-bold text-olive-900">Bonjour {{ session.prenom }}</h1>
          <p class="mt-2 text-gray-700">
            Espace : <strong>{{ session.libelleRole }}</strong>
          </p>
          <p class="mt-6 rounded-lg bg-white p-4 text-sm text-gray-600 shadow-sm">
            Cet espace sera construit dans les prochaines fonctionnalités.
          </p>
        </main>
      </div>
    }
  `,
})
export class EspaceProvisoire {
  readonly authentification = inject(AuthentificationService);
}
