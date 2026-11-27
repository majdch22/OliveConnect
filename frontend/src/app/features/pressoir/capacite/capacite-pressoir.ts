import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, of, switchMap } from 'rxjs';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { CapaciteDuJour, jour } from '../pressoir.modeles';
import { PressoirService } from '../pressoir.service';

/**
 * Page /pressoir/capacite (UC51, maquette PR1 « Publier la capacité ») : capacité des 7 derniers jours et des 14 prochains,
 * et publication de la capacité d'un jour (maintenance, panne, journée renforcée). La planification des tournées
 * utilise aussitôt la capacité publiée.
 */
@Component({
  selector: 'app-capacite-pressoir',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './capacite-pressoir.html',
})
export class CapacitePressoir {
  private readonly service = inject(PressoirService);
  readonly aujourdhui = jour();

  private readonly rechargement = signal(0);
  readonly messageErreur = signal<string | null>(null);
  readonly jours = toSignal(
    toObservable(this.rechargement).pipe(
      switchMap(() =>
        this.service.capacites(jour(-7), jour(14)).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
    ),
    { initialValue: null as CapaciteDuJour[] | null },
  );

  readonly date = signal(jour());
  readonly maximum = signal<number | null>(null);
  readonly motif = signal('');
  readonly envoiEnCours = signal(false);
  readonly erreurs = signal<Record<string, string>>({});
  readonly messageSucces = signal<string | null>(null);

  /** Reprend dans le formulaire un jour de la liste (aujourd'hui ou à venir). */
  choisir(capacite: CapaciteDuJour): void {
    if (capacite.date < this.aujourdhui) return;
    this.date.set(capacite.date);
    this.maximum.set(capacite.maximumKg);
    this.motif.set(capacite.motif ?? '');
    this.messageSucces.set(null);
  }

  publier(): void {
    const maximum = this.maximum();
    if (maximum === null || maximum < 0 || !this.date()) return;
    this.envoiEnCours.set(true);
    this.erreurs.set({});
    this.messageErreur.set(null);
    this.service.publierCapacite(this.date(), maximum, this.motif().trim() || null).subscribe({
      next: (capacite) => {
        this.envoiEnCours.set(false);
        this.messageSucces.set(
          `Capacité du ${capacite.date.split('-').reverse().join('/')} publiée : ${capacite.maximumKg.toLocaleString('fr-FR')} kg. La planification en tient compte.`,
        );
        this.rechargement.update((fois) => fois + 1);
      },
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreurs.set(erreursParChamp(erreur));
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }

  pourcentage(valeur: number, total: number): number {
    return total <= 0 ? 0 : Math.max(0, Math.min(100, Math.round((valeur / total) * 100)));
  }
}
