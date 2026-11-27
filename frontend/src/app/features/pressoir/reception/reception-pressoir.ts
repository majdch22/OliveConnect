import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, combineLatest, map, of, switchMap } from 'rxjs';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { NotificationsService } from '../../../core/notifications/notifications.service';
import { categoriePour, classesCategorie, jour, MonPressoir, Reception, TourneeAttendue, tonnes } from '../pressoir.modeles';
import { PressoirService } from '../pressoir.service';
import { VisitePressoir } from '../visite/visite-pressoir';

/** Tolérance de pesée par défaut (paramètres) : affichée pendant la saisie, le serveur fait foi. */
const TOLERANCE_PESEE = 110;

/**
 * Page /pressoir (maquettes M7 et PR1) : capacité du jour, réception d'une benne, livraisons attendues et réceptions du jour.
 * Pendant la saisie, le taux de remplissage et la catégorie de qualité s'affichent tout de suite ; le serveur refuse une
 * pesée au-delà de la tolérance (110 % de la capacité de la benne). La dernière benne d'une tournée la clôture.
 * La page se relit à chaque notification reçue en direct (tournée qui arrive…).
 */
@Component({
  selector: 'app-reception-pressoir',
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './reception-pressoir.html',
})
export class ReceptionPressoir {
  private readonly service = inject(PressoirService);
  private readonly notifications = inject(NotificationsService);
  readonly visite = inject(VisitePressoir);
  readonly classesCategorie = classesCategorie;
  readonly tonnes = tonnes;
  readonly tolerance = TOLERANCE_PESEE;

  private readonly rechargement = signal(0);
  readonly messageErreur = signal<string | null>(null);
  private readonly declencheur = combineLatest([toObservable(this.rechargement), toObservable(this.notifications.derniereRecue)]);

  readonly monPressoir = toSignal(
    this.declencheur.pipe(switchMap(() => this.service.monPressoir().pipe(catchError((erreur: HttpErrorResponse) => {
      this.messageErreur.set(texteErreur(erreur));
      return of(null);
    })))),
    { initialValue: null as MonPressoir | null },
  );
  readonly livraisons = toSignal(
    this.declencheur.pipe(switchMap(() => this.service.livraisons(jour()).pipe(catchError(() => of([] as TourneeAttendue[] | null))))),
    { initialValue: null as TourneeAttendue[] | null },
  );
  readonly receptionsDuJour = toSignal(
    this.declencheur.pipe(switchMap(() => this.service.receptions(jour(), jour(), 0, 50).pipe(
      map((page) => page.elements), catchError(() => of([] as Reception[] | null))))),
    { initialValue: null as Reception[] | null },
  );

  readonly recevables = computed(() => (this.livraisons() ?? []).filter((tournee) => tournee.recevable && tournee.bennes.some((benne) => !benne.recue)));

  // --- Formulaire de réception ---

  readonly tourneeId = signal('');
  readonly benneId = signal('');
  readonly poids = signal<number | null>(null);
  readonly acidite = signal<number | null>(null);
  readonly commentaire = signal('');
  readonly envoiEnCours = signal(false);
  readonly erreurs = signal<Record<string, string>>({});
  readonly erreurReception = signal<string | null>(null);
  readonly messageSucces = signal<string | null>(null);

  readonly tournee = computed(() => this.recevables().find((tournee) => tournee.id === this.tourneeId()) ?? null);
  readonly benne = computed(() => this.tournee()?.bennes.find((benne) => benne.id === this.benneId()) ?? null);
  readonly remplissage = computed(() => {
    const benne = this.benne();
    const poids = this.poids();
    return benne && poids && benne.capaciteKg ? Math.round((poids * 100) / benne.capaciteKg) : null;
  });
  readonly categorie = computed(() => (this.acidite() === null || this.acidite()! < 0 ? null : categoriePour(this.acidite()!)));
  readonly complet = computed(() => !!this.tournee() && !!this.benne() && !!this.poids() && this.poids()! > 0 && this.acidite() !== null);

  choisirTournee(id: string): void {
    this.tourneeId.set(id);
    const premiere = this.tournee()?.bennes.find((benne) => !benne.recue);
    this.benneId.set(premiere?.id ?? '');
  }

  receptionner(): void {
    if (!this.complet()) return;
    this.envoiEnCours.set(true);
    this.erreurs.set({});
    this.erreurReception.set(null);
    this.service
      .receptionner({
        tourneeId: this.tourneeId(), benneId: this.benneId(), poidsNetKg: this.poids()!, aciditePourcent: this.acidite()!,
        commentaire: this.commentaire().trim() || null,
      })
      .subscribe({
        next: (reception) => {
          this.envoiEnCours.set(false);
          this.messageSucces.set(`Benne ${reception.codeBenne} reçue : ${reception.poidsNetKg.toLocaleString('fr-FR')} kg, ${reception.libelleCategorie.toLowerCase()}.`
            + (reception.tourneeCloturee ? ` Toutes les bennes de ${reception.numeroTournee} sont reçues : la tournée est clôturée et les agriculteurs sont prévenus.` : ''));
          this.poids.set(null);
          this.acidite.set(null);
          this.commentaire.set('');
          this.benneId.set('');
          if (reception.tourneeCloturee) this.tourneeId.set('');
          this.rechargement.update((fois) => fois + 1);
          // Benne suivante de la même tournée, s'il en reste une
          setTimeout(() => this.tourneeId() && this.choisirTournee(this.tourneeId()), 400);
        },
        error: (erreur: HttpErrorResponse) => {
          this.envoiEnCours.set(false);
          this.erreurs.set(erreursParChamp(erreur));
          this.erreurReception.set(texteErreur(erreur));
          this.rechargement.update((fois) => fois + 1);
        },
      });
  }

  bennesRecues(tournee: TourneeAttendue): number {
    return tournee.bennes.filter((benne) => benne.recue).length;
  }
}
