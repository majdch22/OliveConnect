import { Component, computed, input, OnInit, output, signal } from '@angular/core';

/** Une étape : l'élément à montrer (attribut data-visite="…") et l'explication. */
export interface EtapeVisite {
  cible: string;
  titre: string;
  texte: string;
}

/** Position et taille de l'élément mis en avant, en pixels dans la fenêtre. */
interface Cadre {
  haut: number;
  gauche: number;
  largeur: number;
  hauteur: number;
}

const MARGE = 6;

/**
 * Visite guidée (maquette P6) : assombrit la page, entoure l'élément de l'étape et l'explique dans une bulle.
 * « Suivant », « Précédent », « Passer la visite » ; touche Échap pour quitter, flèches pour avancer ou reculer.
 * Si un élément n'est pas affiché (menu différent sur téléphone), la bulle s'affiche seule au centre.
 */
@Component({
  selector: 'app-visite-guidee',
  host: { '(window:keydown)': 'clavier($event)', '(window:resize)': 'redimensionnement()' },
  template: `
    <div class="fixed inset-0 z-40" role="dialog" aria-modal="true" [attr.aria-label]="'Visite guidée, étape ' + (rang() + 1) + ' sur ' + etapes().length">
      @if (cadre(); as zone) {
        <!-- Le « trou » lumineux autour de l'élément : une ombre immense assombrit tout le reste -->
        <div class="pointer-events-none absolute rounded-xl ring-4 ring-amber-400 transition-all duration-150 ease-out"
          [style.top.px]="zone.haut - marge" [style.left.px]="zone.gauche - marge"
          [style.width.px]="zone.largeur + 2 * marge" [style.height.px]="zone.hauteur + 2 * marge"
          style="box-shadow: 0 0 0 9999px rgb(0 0 0 / 0.55)"></div>
      } @else {
        <div class="absolute inset-0 bg-black/55"></div>
      }

      <div class="absolute inset-x-4 mx-auto max-w-sm rounded-2xl bg-white p-5 shadow-xl sm:inset-x-auto" [style]="positionBulle()">
        <p class="text-xs font-semibold uppercase tracking-wide text-olive-700">Étape {{ rang() + 1 }} / {{ etapes().length }}</p>
        <h2 class="mt-1 text-lg font-bold text-gray-900">{{ etape().titre }}</h2>
        <p class="mt-1 text-sm text-gray-700">{{ etape().texte }}</p>
        <div class="mt-4 flex items-center justify-between gap-2">
          <button type="button" class="text-sm font-semibold text-gray-500 hover:underline" (click)="termine.emit()">Passer la visite</button>
          <div class="flex gap-2">
            @if (rang() > 0) {
              <button type="button" class="bouton-secondaire px-4 py-2 text-sm" (click)="aller(rang() - 1)">Précédent</button>
            }
            <button type="button" class="bouton-principal px-4 py-2 text-sm" (click)="suivant()">
              {{ rang() === etapes().length - 1 ? 'Terminer' : 'Suivant' }}
            </button>
          </div>
        </div>
        <div class="mt-4 flex justify-center gap-1.5" aria-hidden="true">
          @for (point of etapes(); track $index) {
            <span class="h-2 w-2 rounded-full" [class]="$index === rang() ? 'bg-olive-700' : 'bg-gray-300'"></span>
          }
        </div>
      </div>
    </div>
  `,
})
export class VisiteGuidee implements OnInit {
  readonly etapes = input.required<EtapeVisite[]>();
  /** Émis en fin de visite, qu'elle soit terminée ou passée. */
  readonly termine = output<void>();

  readonly marge = MARGE;
  readonly rang = signal(0);
  readonly cadre = signal<Cadre | null>(null);
  readonly etape = computed(() => this.etapes()[this.rang()]);

  /** La bulle se place sous l'élément s'il est en haut de l'écran, au-dessus sinon ; au centre s'il n'y a pas d'élément. */
  readonly positionBulle = computed(() => {
    const zone = this.cadre();
    if (!zone) {
      return 'top: 50%; transform: translateY(-50%)';
    }
    const enHaut = zone.haut + zone.hauteur / 2 < window.innerHeight / 2;
    const gauche = Math.min(Math.max(16, zone.gauche), Math.max(16, window.innerWidth - 400));
    const horizontal = window.innerWidth >= 640 ? `left: ${gauche}px;` : '';
    return enHaut
      ? `${horizontal} top: ${zone.haut + zone.hauteur + MARGE + 12}px`
      : `${horizontal} bottom: ${window.innerHeight - zone.haut + MARGE + 12}px`;
  });

  ngOnInit(): void {
    this.aller(0);
  }

  suivant(): void {
    if (this.rang() === this.etapes().length - 1) {
      this.termine.emit();
    } else {
      this.aller(this.rang() + 1);
    }
  }

  aller(rang: number): void {
    this.rang.set(rang);
    const element = this.elementVisible(this.etapes()[rang].cible);
    // Défilement immédiat, et seulement si l'élément est hors de l'écran ; mesure dès l'image suivante
    if (element && !estALEcran(element)) {
      element.scrollIntoView({ block: 'center', behavior: 'instant' });
    }
    requestAnimationFrame(() => this.mesurer(element));
  }

  clavier(evenement: KeyboardEvent): void {
    if (evenement.key === 'Escape') {
      this.termine.emit();
    } else if (evenement.key === 'ArrowRight') {
      this.suivant();
    } else if (evenement.key === 'ArrowLeft' && this.rang() > 0) {
      this.aller(this.rang() - 1);
    }
  }

  redimensionnement(): void {
    this.mesurer(this.elementVisible(this.etape().cible));
  }

  /** Plusieurs éléments peuvent porter la même cible (menu ordinateur et menu téléphone) : on prend celui qui est affiché. */
  private elementVisible(cible: string): HTMLElement | null {
    const elements = Array.from(document.querySelectorAll<HTMLElement>(`[data-visite="${cible}"]`));
    return elements.find((element) => element.getClientRects().length > 0) ?? null;
  }

  private mesurer(element: HTMLElement | null): void {
    if (!element) {
      this.cadre.set(null);
      return;
    }
    const zone = element.getBoundingClientRect();
    this.cadre.set({ haut: zone.top, gauche: zone.left, largeur: zone.width, hauteur: zone.height });
  }
}

/** Vrai si l'élément est entièrement visible dans la fenêtre (pas besoin de faire défiler la page). */
function estALEcran(element: HTMLElement): boolean {
  const zone = element.getBoundingClientRect();
  return zone.top >= 0 && zone.bottom <= window.innerHeight;
}
