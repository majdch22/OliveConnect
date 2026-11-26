import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { combineLatest, map, scan, switchMap } from 'rxjs';

import { CategorieNotification, Notification } from '../../../core/notifications/notification.modeles';
import { NotificationsService } from '../../../core/notifications/notifications.service';
import { DepuisPipe } from '../../../shared/pipes/depuis.pipe';

type Onglet = 'toutes' | 'non-lues' | CategorieNotification;

/** Icône de chaque famille de notification. */
const ICONES: Record<CategorieNotification, string> = { COLLECTE: '🚜', ALERTE: '⚠', COMPTE: '👤' };

/**
 * Page « Notifications » (tous les rôles) : onglets Toutes / Non lues / Collectes / Alertes, « Tout marquer lu ».
 * Toucher une notification la marque lue et ouvre la page concernée.
 */
@Component({
  selector: 'app-page-notifications',
  imports: [DepuisPipe],
  template: `
    <header class="flex flex-wrap items-end justify-between gap-4">
      <h1 class="text-2xl font-bold text-olive-900 sm:text-3xl">Notifications</h1>
      <button type="button" class="bouton-secondaire px-4 py-2 text-sm" [disabled]="service.nombreNonLues() === 0" (click)="toutLire()">
        Tout marquer lu
      </button>
    </header>

    <nav class="mt-6 flex gap-2 overflow-x-auto border-b border-gray-200" aria-label="Familles de notifications">
      @for (entree of onglets; track entree.code) {
        <button type="button" (click)="choisir(entree.code)" class="-mb-px shrink-0 border-b-2 px-4 py-3 text-sm font-semibold"
          [class]="onglet() === entree.code ? 'border-olive-700 text-olive-900' : 'border-transparent text-gray-500'">
          {{ entree.libelle }}{{ entree.code === 'non-lues' ? ' (' + service.nombreNonLues() + ')' : '' }}
        </button>
      }
    </nav>

    <ul class="mt-4 divide-y divide-gray-100 overflow-hidden rounded-2xl border border-gray-200 bg-white">
      @for (notification of notifications(); track notification.id) {
        <li>
          <button type="button" (click)="ouvrir(notification)" class="flex w-full items-start gap-4 px-5 py-4 text-left hover:bg-olive-50"
            [class.bg-olive-50]="!notification.lue">
            <span class="mt-0.5 text-xl" aria-hidden="true">{{ icones[notification.categorie] }}</span>
            <span class="min-w-0 flex-1">
              <span class="flex flex-wrap items-center gap-2">
                <span class="font-semibold text-gray-900">{{ notification.titre }}</span>
                @if (notification.prioritaire) {
                  <span class="rounded bg-amber-100 px-1.5 py-0.5 text-xs font-semibold text-amber-800">Prioritaire</span>
                }
                @if (!notification.lue) { <span class="h-2 w-2 rounded-full bg-olive-700" aria-label="Non lue"></span> }
              </span>
              <span class="mt-0.5 block text-sm text-gray-600">{{ notification.message }}</span>
            </span>
            <span class="shrink-0 text-xs text-gray-500">{{ notification.dateCreation | depuis }}</span>
          </button>
        </li>
      } @empty {
        <li class="px-5 py-10 text-center text-gray-500">Aucune notification ici.</li>
      }
    </ul>

    @if (resteAVoir()) {
      <div class="mt-4 text-center">
        <button type="button" class="bouton-secondaire px-4 py-2 text-sm" (click)="page.update((numero) => numero + 1)">Voir plus</button>
      </div>
    }
  `,
})
export class PageNotifications {
  readonly service = inject(NotificationsService);
  private readonly router = inject(Router);
  readonly icones = ICONES;

  readonly onglets: { code: Onglet; libelle: string }[] = [
    { code: 'toutes', libelle: 'Toutes' },
    { code: 'non-lues', libelle: 'Non lues' },
    { code: 'COLLECTE', libelle: 'Collectes' },
    { code: 'ALERTE', libelle: 'Alertes' },
    { code: 'COMPTE', libelle: 'Compte' },
  ];

  readonly onglet = signal<Onglet>('toutes');
  readonly page = signal(0);
  /** Change à chaque action qui modifie la liste (lecture, tout marquer lu) pour la recharger. */
  private readonly rechargement = signal(0);

  /** Les pages déjà chargées, mises bout à bout ; revient à la première page quand l'onglet change. */
  private readonly chargement = toSignal(
    combineLatest([toObservable(this.onglet), toObservable(this.page), toObservable(this.rechargement)]).pipe(
      switchMap(([onglet, page]) => {
        const categorie = onglet === 'toutes' || onglet === 'non-lues' ? null : onglet;
        return this.service.liste(categorie, onglet === 'non-lues', page).pipe(map((resultat) => ({ resultat, page })));
      }),
      scan(
        (cumul, { resultat, page }) => ({
          elements: page === 0 ? resultat.elements : [...cumul.elements, ...resultat.elements],
          totalPages: resultat.totalPages,
          page,
        }),
        { elements: [] as Notification[], totalPages: 0, page: 0 },
      ),
    ),
  );

  readonly notifications = computed(() => this.chargement()?.elements ?? []);
  readonly resteAVoir = computed(() => {
    const chargement = this.chargement();
    return !!chargement && chargement.page + 1 < chargement.totalPages;
  });

  choisir(onglet: Onglet): void {
    this.page.set(0);
    this.onglet.set(onglet);
  }

  ouvrir(notification: Notification): void {
    const suite = () => (notification.lien ? this.router.navigateByUrl(notification.lien) : this.recharger());
    if (notification.lue) {
      suite();
      return;
    }
    this.service.marquerLue(notification).subscribe({ next: suite, error: suite });
  }

  toutLire(): void {
    this.service.toutMarquerLu().subscribe(() => this.recharger());
  }

  private recharger(): void {
    this.page.set(0);
    this.rechargement.update((valeur) => valeur + 1);
  }
}
