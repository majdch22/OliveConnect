import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { NotificationsService } from '../../../core/notifications/notifications.service';
import { DepuisPipe } from '../../../shared/pipes/depuis.pipe';
import { VisiteAgriculteur } from '../visite/visite-agriculteur';
import { COULEURS_STATUT_DEMANDE, Demande } from '../agriculteur.modeles';
import { AgriculteurService } from '../agriculteur.service';

/** États où une demande « vit » encore (pas terminée, annulée ni rejetée). */
const EN_COURS = ['SOUMISE', 'VALIDEE', 'PLANIFIEE', 'EN_COURS'];

/**
 * Page /agriculteur : visite guidée proposée à la première connexion, actions principales, prochaine collecte,
 * vergers, notifications récentes et derniers signalements.
 */
@Component({
  selector: 'app-accueil-agriculteur',
  imports: [RouterLink, DatePipe, DecimalPipe, DepuisPipe],
  templateUrl: './accueil-agriculteur.html',
})
export class AccueilAgriculteur {
  private readonly service = inject(AgriculteurService);
  readonly session = inject(AuthentificationService).session;
  readonly couleurs = COULEURS_STATUT_DEMANDE;
  readonly visite = inject(VisiteAgriculteur);

  readonly vergers = toSignal(this.service.vergers(), { initialValue: [] });
  readonly demandes = toSignal(this.service.demandes(), { initialValue: [] });
  readonly signalements = toSignal(this.service.signalements(), { initialValue: [] });

  readonly vergersActifs = computed(() => this.vergers().filter((verger) => !verger.archive));
  readonly demandesEnCours = computed(() => this.demandes().filter((demande) => EN_COURS.includes(demande.statut)));

  /** La demande en cours dont la date est la plus proche. */
  readonly prochaineCollecte = computed<Demande | null>(() =>
    [...this.demandesEnCours()].sort((a, b) => a.dateSouhaitee.localeCompare(b.dateSouhaitee))[0] ?? null,
  );

  readonly derniersSignalements = computed(() => this.signalements().slice(0, 3));

  private readonly notifications = inject(NotificationsService);
  /** Les trois dernières ; rechargées quand une notification arrive en direct. */
  readonly notificationsRecentes = toSignal(
    toObservable(this.notifications.derniereRecue).pipe(switchMap(() => this.notifications.liste(null, false, 0))),
    { initialValue: null },
  );
  readonly notificationsRecentesAffichees = computed(() => this.notificationsRecentes()?.elements.slice(0, 3) ?? []);
}
