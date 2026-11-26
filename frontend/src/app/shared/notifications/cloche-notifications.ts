import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NotificationsService } from '../../core/notifications/notifications.service';

/** Cloche du haut de l'écran, avec le nombre de notifications non lues ; ouvre la page des notifications. */
@Component({
  selector: 'app-cloche-notifications',
  imports: [RouterLink],
  template: `
    <a
      [routerLink]="adresse()"
      class="relative inline-flex h-10 w-10 items-center justify-center rounded-full hover:bg-black/5"
      [class.text-white]="clair()"
      [attr.aria-label]="'Notifications' + (notifications.nombreNonLues() ? ', ' + notifications.nombreNonLues() + ' non lues' : '')"
    >
      <svg viewBox="0 0 24 24" class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
        <path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.9 1.9 0 0 0 3.4 0" />
      </svg>
      @if (notifications.nombreNonLues() > 0) {
        <span class="absolute -right-0.5 -top-0.5 min-w-5 rounded-full bg-red-600 px-1.5 text-center text-xs font-bold leading-5 text-white">
          {{ notifications.nombreNonLues() > 99 ? '99+' : notifications.nombreNonLues() }}
        </span>
      }
    </a>
  `,
})
export class ClocheNotifications {
  readonly notifications = inject(NotificationsService);

  /** Page des notifications de l'espace (par exemple /agriculteur/notifications). */
  readonly adresse = input.required<string>();
  /** Vrai sur un fond foncé (menu de l'administration). */
  readonly clair = input(false);
}
