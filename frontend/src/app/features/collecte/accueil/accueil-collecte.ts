import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { texteErreur } from '../../../core/erreurs/message-erreur';
import { NotificationsService } from '../../../core/notifications/notifications.service';
import { TableauDeBordCollecte } from '../collecte.modeles';
import { CollecteService } from '../collecte.service';
import { VisiteCollecte } from '../visite/visite-collecte';

/**
 * Page /collecte : tableau de bord du responsable de collecte.
 * Chiffres clés (chacun mène à sa liste), charge des 7 prochains jours, capacité des pressoirs aujourd'hui
 * et signalements non résolus par type. Relu à chaque notification reçue en direct.
 */
@Component({
  selector: 'app-accueil-collecte',
  imports: [RouterLink, DatePipe, DecimalPipe],
  templateUrl: './accueil-collecte.html',
})
export class AccueilCollecte {
  private readonly service = inject(CollecteService);
  private readonly notifications = inject(NotificationsService);
  readonly session = inject(AuthentificationService).session;
  readonly visite = inject(VisiteCollecte);
  readonly messageErreur = signal<string | null>(null);

  readonly tableau = toSignal(
    toObservable(this.notifications.derniereRecue).pipe(
      switchMap(() =>
        this.service.tableauDeBord().pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
    ),
    { initialValue: null as TableauDeBordCollecte | null },
  );

  readonly tourneesDuJour = computed(() =>
    Object.entries(this.tableau()?.tourneesDuJourParStatut ?? {})
      .filter(([statut]) => statut !== 'ANNULEE')
      .reduce((total, [, nombre]) => total + (nombre ?? 0), 0),
  );

  /** La plus grande quantité d'un jour : longueur pleine des barres de charge. */
  readonly chargeMaximum = computed(() =>
    Math.max(1, ...(this.tableau()?.charge ?? []).map((jour) => jour.planifieeKg + jour.enAttenteKg)),
  );

  pourcentage(valeur: number, total: number): number {
    return total <= 0 ? 0 : Math.min(100, Math.round((valeur / total) * 100));
  }

  /** « 1 h 25 », « 12 min » ou « moins d'une minute ». */
  delai(minutes: number | null): string {
    if (minutes === null) return '—';
    if (minutes < 1) return "moins d'une minute";
    if (minutes < 60) return `${minutes} min`;
    const heures = Math.floor(minutes / 60);
    return heures >= 48 ? `${Math.round(heures / 24)} jours` : `${heures} h ${String(minutes % 60).padStart(2, '0')}`;
  }
}
