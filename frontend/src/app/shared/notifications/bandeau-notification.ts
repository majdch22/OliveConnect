import { Component, effect, inject, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';

import { NotificationsService } from '../../core/notifications/notifications.service';

/** Durée d'affichage du bandeau d'une notification reçue en temps réel. */
const DUREE_AFFICHAGE_MILLISECONDES = 8000;

/**
 * Bandeau qui apparaît quand une notification arrive en temps réel (en haut à droite sur ordinateur, en haut sur téléphone).
 * Le toucher ouvre la page concernée ; il disparaît seul au bout de quelques secondes.
 */
@Component({
  selector: 'app-bandeau-notification',
  template: `
    @if (notifications.derniereRecue(); as notification) {
      <div role="status" aria-live="polite" class="fixed inset-x-4 top-4 z-50 sm:left-auto sm:right-6 sm:w-96">
        <div class="flex items-start gap-3 rounded-2xl border bg-white p-4 shadow-lg"
          [class]="notification.prioritaire ? 'border-amber-400' : 'border-gray-200'">
          <button type="button" class="min-w-0 flex-1 text-left" (click)="ouvrir(notification.lien)">
            <p class="text-xs font-semibold uppercase tracking-wide text-gray-500">
              {{ notification.prioritaire ? 'Prioritaire · ' : '' }}{{ notification.libelleCategorie }}
            </p>
            <p class="mt-0.5 font-semibold text-gray-900">{{ notification.titre }}</p>
            <p class="mt-0.5 line-clamp-2 text-sm text-gray-600">{{ notification.message }}</p>
          </button>
          <button type="button" class="rounded-lg px-2 py-1 text-gray-500 hover:bg-gray-100" aria-label="Fermer" (click)="notifications.oublierDerniere()">✕</button>
        </div>
      </div>
    }
  `,
})
export class BandeauNotification implements OnDestroy {
  readonly notifications = inject(NotificationsService);
  private readonly router = inject(Router);
  private minuterie: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // Chaque nouvelle notification relance le compte à rebours de disparition
    effect(() => {
      if (this.notifications.derniereRecue()) {
        clearTimeout(this.minuterie);
        this.minuterie = setTimeout(() => this.notifications.oublierDerniere(), DUREE_AFFICHAGE_MILLISECONDES);
      }
    });
  }

  ouvrir(lien: string | null): void {
    this.notifications.oublierDerniere();
    if (lien) {
      this.router.navigateByUrl(lien);
    }
  }

  ngOnDestroy(): void {
    clearTimeout(this.minuterie);
  }
}
