import { DatePipe, DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, merge, of, Subject, switchMap, tap, timer } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { GraphiqueTemporel, PointTemporel } from '../../../shared/graphiques/graphique-temporel';
import { Echantillon } from '../administration.modeles';
import { AdministrationService } from '../administration.service';
import { MessagesEnvoyes } from './messages-envoyes';

/** Intervalle de rafraîchissement automatique de la page. */
const RAFRAICHISSEMENT_MS = 10_000;

/**
 * Page /administration/supervision : état de santé du serveur et de ses composants,
 * graphiques de la dernière heure, derniers avertissements du journal et sauvegardes de la base.
 * La page se met à jour toute seule toutes les 10 secondes.
 */
@Component({
  selector: 'app-supervision-administration',
  imports: [DatePipe, DecimalPipe, NgTemplateOutlet, GraphiqueTemporel, MessagesEnvoyes],
  templateUrl: './supervision-administration.html',
})
export class SupervisionAdministration {
  private readonly administration = inject(AdministrationService);

  private readonly actualisationDemandee = new Subject<void>();
  readonly messageErreur = signal<string | null>(null);

  /** État du serveur : chargé tout de suite, puis toutes les 10 secondes, ou sur demande. */
  readonly etat = toSignal(
    merge(timer(0, RAFRAICHISSEMENT_MS), this.actualisationDemandee).pipe(
      switchMap(() =>
        this.administration.etatServeur().pipe(
          tap(() => this.messageErreur.set(null)),
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
    ),
  );

  actualiser(): void {
    this.actualisationDemandee.next();
  }

  /** Résumé global affiché en haut : tout va bien, ou nombre de composants à surveiller. */
  readonly resume = computed(() => {
    const composants = this.etat()?.composants ?? [];
    const enPanne = composants.filter((composant) => composant.etat === 'HORS_SERVICE').length;
    const aSurveiller = composants.filter((composant) => composant.etat === 'ATTENTION').length;
    if (enPanne > 0) {
      return { etat: 'HORS_SERVICE' as const, texte: `${enPanne} composant(s) hors service` };
    }
    if (aSurveiller > 0) {
      return { etat: 'ATTENTION' as const, texte: `${aSurveiller} composant(s) à surveiller` };
    }
    return { etat: 'OPERATIONNEL' as const, texte: 'Tous les composants sont opérationnels' };
  });

  // --- Séries des graphiques (une mesure par graphique, jamais deux échelles sur le même) ---

  readonly processeur = computed(() => this.serie((mesure) => mesure.processeur));
  readonly memoire = computed(() => this.serie((mesure) => mesure.memoireUtiliseeMo));
  readonly requetes = computed(() => this.serie((mesure) => mesure.requetesParMinute));
  readonly tempsReponse = computed(() => this.serie((mesure) => mesure.tempsReponseMoyenMs));

  private serie(lecture: (mesure: Echantillon) => number | null): PointTemporel[] {
    return (this.etat()?.historique ?? []).map((mesure) => ({ moment: mesure.moment, valeur: lecture(mesure) }));
  }

  /** « 2 h 15 min » à partir d'un nombre de secondes. */
  dureeLisible(secondes: number): string {
    const jours = Math.floor(secondes / 86_400);
    const heures = Math.floor((secondes % 86_400) / 3_600);
    const minutes = Math.floor((secondes % 3_600) / 60);
    if (jours > 0) {
      return `${jours} j ${heures} h`;
    }
    return heures > 0 ? `${heures} h ${minutes} min` : `${minutes} min`;
  }

  /** Taille d'un fichier en Ko ou Mo. */
  tailleLisible(octets: number): string {
    return octets >= 1_048_576 ? `${(octets / 1_048_576).toFixed(1)} Mo` : `${Math.max(1, Math.round(octets / 1024))} Ko`;
  }

  // --- Sauvegarde à la demande ---

  readonly sauvegardeEnCours = signal(false);
  readonly messageSauvegarde = signal<{ reussite: boolean; texte: string } | null>(null);

  sauvegarder(): void {
    this.sauvegardeEnCours.set(true);
    this.messageSauvegarde.set(null);
    this.administration.sauvegarderMaintenant().subscribe({
      next: (sauvegarde) => {
        this.sauvegardeEnCours.set(false);
        this.messageSauvegarde.set({ reussite: true, texte: `Sauvegarde créée : ${sauvegarde.nom}` });
        this.actualiser();
      },
      error: (erreur: HttpErrorResponse) => {
        this.sauvegardeEnCours.set(false);
        this.messageSauvegarde.set({ reussite: false, texte: texteErreur(erreur) });
      },
    });
  }
}
