import { Component, input, output } from '@angular/core';

/**
 * Panneau qui glisse depuis la droite pour afficher une fiche sans quitter la liste.
 * Plein écran sur téléphone. Se ferme avec la croix, la touche Échap ou un clic sur le fond.
 */
@Component({
  selector: 'app-panneau-lateral',
  host: { '(document:keydown.escape)': 'fermer.emit()' },
  template: `
    <div class="fixed inset-0 z-40 flex justify-end">
      <div class="absolute inset-0 bg-gray-900/40" (click)="fermer.emit()" aria-hidden="true"></div>
      <section role="dialog" aria-modal="true" [attr.aria-label]="titre()" class="relative flex h-full w-full flex-col bg-white shadow-2xl sm:max-w-xl">
        <header class="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-5">
          <div>
            <h2 class="text-xl font-bold text-olive-900">{{ titre() }}</h2>
            @if (sousTitre()) {
              <p class="mt-1 text-sm text-gray-500">{{ sousTitre() }}</p>
            }
          </div>
          <button type="button" (click)="fermer.emit()" class="rounded-lg p-2 text-gray-500 hover:bg-gray-100" aria-label="Fermer">
            <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 6l12 12M18 6L6 18" /></svg>
          </button>
        </header>
        <div class="flex-1 overflow-y-auto px-6 py-6">
          <ng-content />
        </div>
      </section>
    </div>
  `,
})
export class PanneauLateral {
  readonly titre = input.required<string>();
  readonly sousTitre = input<string | null>(null);
  readonly fermer = output<void>();
}
