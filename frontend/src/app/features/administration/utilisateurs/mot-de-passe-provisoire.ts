import { Component, input, signal } from '@angular/core';

/**
 * Affiche une seule fois le mot de passe provisoire d'un compte, avec un bouton pour le copier.
 * Il n'est enregistré que chiffré : une fois cet encadré fermé, personne ne peut le relire.
 */
@Component({
  selector: 'app-mot-de-passe-provisoire',
  template: `
    <div class="rounded-xl border border-amber-300 bg-amber-50 p-5">
      <p class="font-semibold text-amber-900">Mot de passe provisoire de {{ destinataire() }}</p>
      <div class="mt-3 flex items-center gap-3">
        <code class="flex-1 rounded-lg bg-white px-4 py-3 text-lg font-bold tracking-wider text-gray-900">{{ motDePasse() }}</code>
        <button type="button" (click)="copier()" class="rounded-lg border border-amber-400 px-4 py-3 text-sm font-semibold text-amber-900 hover:bg-amber-100">
          {{ copie() ? 'Copié ✓' : 'Copier' }}
        </button>
      </div>
      <p class="mt-3 text-sm text-amber-900">
        Transmettez-le à la personne. Il ne sera plus affiché : en cas d'oubli, réinitialisez le mot de passe.
      </p>
    </div>
  `,
})
export class MotDePasseProvisoire {
  readonly motDePasse = input.required<string>();
  readonly destinataire = input.required<string>();
  readonly copie = signal(false);

  copier(): void {
    navigator.clipboard
      ?.writeText(this.motDePasse())
      .then(() => this.copie.set(true))
      .catch(() => this.copie.set(false));
  }
}
