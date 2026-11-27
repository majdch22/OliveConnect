import { computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

/** Un guide rapide : quelques étapes simples, et la page à laquelle il se rapporte. */
export interface GuideAide {
  titre: string;
  page: string;
  etapes: string[];
  lien: string;
}

export interface QuestionAide {
  question: string;
  reponse: string;
}

/**
 * Page d'aide d'un espace (maquette P5) : aide pour l'écran d'où l'on vient, guides rapides, questions fréquentes,
 * recherche dans toute l'aide et relance de la visite guidée.
 * Chaque espace en dérive son composant avec ses guides et ses questions, et le même gabarit {@link GABARIT_AIDE}.
 */
export abstract class PageAide {
  private readonly router = inject(Router);
  /** Page d'où l'on vient (bouton « ? » du menu), pour proposer son aide en premier. */
  private readonly depuis = inject(ActivatedRoute).snapshot.queryParamMap.get('depuis') ?? '';

  protected abstract readonly tousLesGuides: GuideAide[];
  protected abstract readonly toutesLesQuestions: QuestionAide[];
  /** Accueil de l'espace, où la visite guidée se relance. */
  protected abstract readonly accueil: string;
  abstract readonly titreContact: string;
  abstract readonly texteContact: string;

  readonly recherche = signal('');
  readonly ouvert = signal<string | null>(null);

  readonly guideDeLaPage = computed(() => this.tousLesGuides.find((guide) => guide.page && this.depuis.startsWith(guide.page)) ?? null);
  readonly guides = computed(() => this.tousLesGuides.filter((guide) => contient([guide.titre, ...guide.etapes], this.recherche())));
  readonly questions = computed(() => this.toutesLesQuestions.filter((entree) => contient([entree.question, entree.reponse], this.recherche())));

  basculer(titre: string): void {
    this.ouvert.update((actuel) => (actuel === titre ? null : titre));
  }

  relancerVisite(): void {
    this.router.navigate([this.accueil], { queryParams: { visite: 1 } });
  }
}

/** Recherche sans tenir compte des majuscules ni des accents (« equipe » trouve « équipe »). */
function contient(textes: string[], recherche: string): boolean {
  const simplifier = (texte: string) => texte.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  const cherche = simplifier(recherche.trim());
  return !cherche || textes.some((texte) => simplifier(texte).includes(cherche));
}

/** Gabarit commun des pages d'aide. */
export const GABARIT_AIDE = `
    <h1 class="text-2xl font-bold text-olive-900 sm:text-3xl">Aide</h1>

    <input type="search" class="champ-saisie mt-5 w-full" placeholder="🔍 Rechercher dans l’aide…" aria-label="Rechercher dans l’aide"
      [value]="recherche()" (input)="recherche.set($any($event.target).value)" />

    @if (guideDeLaPage(); as guide) {
      @if (!recherche()) {
        <div class="mt-5 rounded-2xl border border-olive-200 bg-olive-50 p-5">
          <p class="text-sm font-semibold text-olive-900">Aide pour l’écran où vous étiez</p>
          <button type="button" class="mt-1 text-left font-bold text-olive-900 hover:underline" (click)="ouvert.set(guide.titre)">{{ guide.titre }} →</button>
        </div>
      }
    }

    <div class="mt-6 grid gap-6 lg:grid-cols-2">
      <section>
        <h2 class="font-semibold text-gray-900">Guides rapides</h2>
        <ul class="mt-3 divide-y divide-gray-100 overflow-hidden rounded-2xl border border-gray-200 bg-white">
          @for (guide of guides(); track guide.titre) {
            <li>
              <button type="button" class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left font-medium text-gray-900 hover:bg-olive-50"
                [attr.aria-expanded]="ouvert() === guide.titre" (click)="basculer(guide.titre)">
                {{ guide.titre }} <span aria-hidden="true">{{ ouvert() === guide.titre ? '▾' : '›' }}</span>
              </button>
              @if (ouvert() === guide.titre) {
                <div class="px-5 pb-5">
                  <ol class="list-decimal space-y-1.5 pl-5 text-sm text-gray-700">
                    @for (etape of guide.etapes; track $index) { <li>{{ etape }}</li> }
                  </ol>
                  <a [routerLink]="guide.lien" class="mt-3 inline-block text-sm font-semibold text-olive-700 hover:underline">Ouvrir la page →</a>
                </div>
              }
            </li>
          } @empty {
            <li class="px-5 py-6 text-sm text-gray-500">Aucun guide ne correspond.</li>
          }
        </ul>
      </section>

      <section>
        <h2 class="font-semibold text-gray-900">Questions fréquentes</h2>
        <ul class="mt-3 divide-y divide-gray-100 overflow-hidden rounded-2xl border border-gray-200 bg-white">
          @for (entree of questions(); track entree.question) {
            <li>
              <button type="button" class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left font-medium text-gray-900 hover:bg-olive-50"
                [attr.aria-expanded]="ouvert() === entree.question" (click)="basculer(entree.question)">
                {{ entree.question }} <span aria-hidden="true">{{ ouvert() === entree.question ? '▾' : '▸' }}</span>
              </button>
              @if (ouvert() === entree.question) {
                <p class="px-5 pb-5 text-sm text-gray-700">{{ entree.reponse }}</p>
              }
            </li>
          } @empty {
            <li class="px-5 py-6 text-sm text-gray-500">Aucune question ne correspond.</li>
          }
        </ul>
      </section>
    </div>

    <section class="mt-6 grid gap-4 sm:grid-cols-2">
      <button type="button" class="bouton-secondaire" (click)="relancerVisite()">▶ Relancer la visite guidée</button>
      <div class="rounded-2xl border border-gray-200 bg-white p-5 text-sm text-gray-700">
        <p class="font-semibold text-gray-900">📞 {{ titreContact }}</p>
        <p class="mt-1">{{ texteContact }}</p>
      </div>
    </section>
`;
