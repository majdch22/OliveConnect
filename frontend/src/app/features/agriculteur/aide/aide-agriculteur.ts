import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

/** Un guide rapide : quelques étapes simples, et la page à laquelle il se rapporte. */
interface Guide {
  titre: string;
  page: string;
  etapes: string[];
  lien: string;
}

interface Question {
  question: string;
  reponse: string;
}

const GUIDES: Guide[] = [
  {
    titre: 'Déclarer une récolte en 1 minute',
    page: '/agriculteur/demandes/nouvelle',
    lien: '/agriculteur/demandes/nouvelle',
    etapes: [
      'Touchez « Déclarer une récolte » sur l’accueil.',
      'Choisissez le verger, puis « Planifiée » (à partir de demain) ou « Urgente » (dès aujourd’hui, traitée en priorité).',
      'Choisissez la date et la quantité : touchez la suggestion pour reprendre l’estimation de votre verger.',
      'Envoyez : la coopérative est prévenue tout de suite.',
    ],
  },
  {
    titre: 'Signaler la maturité ou un problème avec le GPS',
    page: '/agriculteur/signalements',
    lien: '/agriculteur/signalements',
    etapes: [
      'Au verger, touchez « Signaler maturité ou problème ».',
      'Choisissez le type (maturité, pluie, ravageur…) et le verger.',
      'Touchez « Ma position » : le téléphone indique où vous êtes. Sans GPS, touchez la carte.',
      'Décrivez ce que vous voyez et ajoutez une photo si vous le souhaitez.',
    ],
  },
  {
    titre: 'Modifier ou annuler une collecte',
    page: '/agriculteur/demandes',
    lien: '/agriculteur/demandes',
    etapes: [
      'Ouvrez « Demandes » et touchez la demande.',
      '« Modifier » change la date ou la quantité ; « Annuler la demande » la retire (le motif est facultatif).',
      'C’est possible tant que la collecte n’a pas démarré.',
    ],
  },
  {
    titre: 'Ajouter et placer un verger',
    page: '/agriculteur/vergers',
    lien: '/agriculteur/vergers',
    etapes: [
      'Ouvrez « Vergers » puis « Ajouter un verger ».',
      'Touchez la carte au milieu du verger, ou « Ma position » si vous y êtes.',
      'Indiquez la variété, la superficie et le nombre d’arbres.',
    ],
  },
  {
    titre: 'Comprendre les statuts',
    page: '',
    lien: '/agriculteur/demandes',
    etapes: [
      'Soumise : la coopérative a reçu votre demande.',
      'Validée : elle est acceptée, la tournée reste à organiser.',
      'Planifiée : une date, une équipe et un pressoir sont choisis.',
      'En cours : l’équipe récolte ; la demande ne peut plus changer.',
      'Terminée : les olives sont pesées au pressoir.',
    ],
  },
];

const QUESTIONS: Question[] = [
  {
    question: 'Pourquoi je ne peux plus annuler ?',
    reponse: 'Une collecte démarrée ne peut plus être modifiée. Contactez le responsable de collecte de la coopérative.',
  },
  {
    question: 'Le GPS ne fonctionne pas',
    reponse: 'Autorisez la localisation pour OliveConnect dans les réglages du téléphone, ou touchez simplement la carte pour placer le point.',
  },
  {
    question: 'Mon signalement est marqué « loin du verger »',
    reponse: 'La position envoyée est éloignée du verger choisi. Il est bien reçu : la coopérative le vérifiera.',
  },
  {
    question: 'J’ai oublié mon mot de passe',
    reponse: 'Sur la page de connexion, touchez « Mot de passe oublié ? » : un code vous est envoyé par SMS.',
  },
  {
    question: 'Je ne reçois plus de SMS',
    reponse: 'Vérifiez dans « Mon profil » que « SMS pour les collectes » est activé et que votre numéro est le bon.',
  },
];

/**
 * Page /agriculteur/aide (maquette P5) : aide pour l'écran d'où l'on vient, guides rapides, questions fréquentes,
 * recherche dans toute l'aide et relance de la visite guidée.
 */
@Component({
  selector: 'app-aide-agriculteur',
  imports: [RouterLink],
  template: `
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
        <p class="font-semibold text-gray-900">📞 Contacter la coopérative</p>
        <p class="mt-1">Pour une collecte déjà démarrée ou une information à corriger, adressez-vous au bureau de la coopérative.</p>
      </div>
    </section>
  `,
})
export class AideAgriculteur {
  private readonly router = inject(Router);
  /** Page d'où l'on vient (bouton « ? » du menu), pour proposer son aide en premier. */
  private readonly depuis = inject(ActivatedRoute).snapshot.queryParamMap.get('depuis') ?? '';

  readonly recherche = signal('');
  readonly ouvert = signal<string | null>(null);

  readonly guideDeLaPage = computed(() => GUIDES.find((guide) => guide.page && this.depuis.startsWith(guide.page)) ?? null);
  readonly guides = computed(() => GUIDES.filter((guide) => contient([guide.titre, ...guide.etapes], this.recherche())));
  readonly questions = computed(() => QUESTIONS.filter((entree) => contient([entree.question, entree.reponse], this.recherche())));

  basculer(titre: string): void {
    this.ouvert.update((actuel) => (actuel === titre ? null : titre));
  }

  relancerVisite(): void {
    this.router.navigate(['/agriculteur'], { queryParams: { visite: 1 } });
  }
}

/** Recherche sans tenir compte des majuscules ni des accents (« equipe » trouve « équipe »). */
function contient(textes: string[], recherche: string): boolean {
  const simplifier = (texte: string) => texte.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  const cherche = simplifier(recherche.trim());
  return !cherche || textes.some((texte) => simplifier(texte).includes(cherche));
}
