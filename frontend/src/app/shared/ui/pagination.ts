import { Component, computed, input, output } from '@angular/core';

/**
 * Barre de pagination : « 21–40 sur 57 », choix du nombre de lignes par page, page précédente / suivante.
 * La page est numérotée à partir de 0, comme côté serveur.
 */
@Component({
  selector: 'app-pagination',
  template: `
    <div class="flex flex-col items-center justify-between gap-3 border-t border-gray-200 px-4 py-3 text-sm text-gray-600 sm:flex-row">
      <p>
        @if (totalElements() === 0) {
          Aucun résultat
        } @else {
          <span class="font-semibold text-gray-900">{{ premier() }}–{{ dernier() }}</span>
          sur <span class="font-semibold text-gray-900">{{ totalElements() }}</span>
        }
      </p>

      <div class="flex items-center gap-4">
        <label class="flex items-center gap-2">
          Lignes par page
          <select
            class="rounded-md border border-gray-300 bg-white px-2 py-1 text-sm"
            [value]="taille()"
            (change)="changerTaille.emit(+$any($event.target).value)"
          >
            @for (choix of taillesPossibles; track choix) {
              <option [value]="choix">{{ choix }}</option>
            }
          </select>
        </label>

        <div class="flex items-center gap-1">
          <button type="button" class="rounded-md border border-gray-300 px-3 py-1 hover:bg-gray-50 disabled:opacity-40" [disabled]="page() === 0" (click)="changerPage.emit(page() - 1)">
            Précédente
          </button>
          <span class="px-2">Page {{ page() + 1 }} / {{ Math.max(totalPages(), 1) }}</span>
          <button type="button" class="rounded-md border border-gray-300 px-3 py-1 hover:bg-gray-50 disabled:opacity-40" [disabled]="page() + 1 >= totalPages()" (click)="changerPage.emit(page() + 1)">
            Suivante
          </button>
        </div>
      </div>
    </div>
  `,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly taille = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly totalPages = input.required<number>();

  readonly changerPage = output<number>();
  readonly changerTaille = output<number>();

  readonly taillesPossibles = [10, 20, 50];
  readonly Math = Math;

  /** Rang du premier et du dernier élément affichés (à partir de 1). */
  readonly premier = computed(() => this.page() * this.taille() + 1);
  readonly dernier = computed(() => Math.min((this.page() + 1) * this.taille(), this.totalElements()));
}
