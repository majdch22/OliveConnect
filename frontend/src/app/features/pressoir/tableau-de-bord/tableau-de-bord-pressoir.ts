import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { classesCategorie, TableauDeBordPressoir as Donnees, tonnes } from '../pressoir.modeles';
import { PressoirService } from '../pressoir.service';

/**
 * Page /pressoir/tableau-de-bord (UC52) : volume reçu, bennes, remplissage moyen, acidité moyenne pondérée,
 * volume par jour, répartition par catégorie de qualité et qualité par variété, sur 7 ou 30 jours.
 */
@Component({
  selector: 'app-tableau-de-bord-pressoir',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './tableau-de-bord-pressoir.html',
})
export class TableauDeBordPressoir {
  private readonly service = inject(PressoirService);
  readonly classesCategorie = classesCategorie;
  readonly tonnes = tonnes;
  readonly periodes = [7, 30];

  readonly jours = signal(7);
  readonly messageErreur = signal<string | null>(null);
  readonly donnees = toSignal(
    toObservable(this.jours).pipe(
      switchMap((jours) =>
        this.service.tableauDeBord(jours).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
    ),
    { initialValue: null as Donnees | null },
  );

  readonly maximumParJour = computed(() => Math.max(1, ...(this.donnees()?.parJour ?? []).map((ligne) => ligne.poidsKg)));
  readonly totalCategories = computed(() => (this.donnees()?.parCategorie ?? []).reduce((total, ligne) => total + ligne.poidsKg, 0));

  pourcentage(valeur: number, total: number): number {
    return total <= 0 ? 0 : Math.min(100, Math.round((valeur / total) * 100));
  }
}
