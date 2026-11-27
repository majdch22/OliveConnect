import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { Pagination } from '../../../shared/ui/pagination';
import { classesCategorie, jour, Page, Reception } from '../pressoir.modeles';
import { PressoirService } from '../pressoir.service';

/**
 * Page /pressoir/historique (maquette PR1) : réceptions d'une période (aujourd'hui, 7 jours, 30 jours ou dates choisies),
 * les plus récentes d'abord, avec la répartition du poids entre vergers ; export de la période en fichier CSV.
 */
@Component({
  selector: 'app-historique-pressoir',
  imports: [DatePipe, DecimalPipe, Pagination],
  templateUrl: './historique-pressoir.html',
})
export class HistoriquePressoir {
  private readonly service = inject(PressoirService);
  readonly classesCategorie = classesCategorie;
  readonly raccourcis = [
    { libelle: "Aujourd'hui", jours: 1 },
    { libelle: '7 jours', jours: 7 },
    { libelle: '30 jours', jours: 30 },
  ];

  readonly du = signal(jour(-6));
  readonly au = signal(jour());
  readonly page = signal(0);
  readonly taille = signal(20);
  readonly messageErreur = signal<string | null>(null);
  readonly ouverte = signal<string | null>(null);

  private readonly filtres = computed(() => ({ du: this.du(), au: this.au(), page: this.page(), taille: this.taille() }));
  readonly resultat = toSignal(
    toObservable(this.filtres).pipe(
      switchMap((filtres) =>
        this.service.receptions(filtres.du, filtres.au, filtres.page, filtres.taille).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
    ),
    { initialValue: null as Page<Reception> | null },
  );

  periode(jours: number): void {
    this.du.set(jour(-(jours - 1)));
    this.au.set(jour());
    this.page.set(0);
  }

  estPeriode(jours: number): boolean {
    return this.du() === jour(-(jours - 1)) && this.au() === jour();
  }

  changerDates(du: string, au: string): void {
    if (!du || !au) return;
    this.du.set(du <= au ? du : au);
    this.au.set(du <= au ? au : du);
    this.page.set(0);
  }

  changerTaille(taille: number): void {
    this.taille.set(taille);
    this.page.set(0);
  }

  basculer(reception: Reception): void {
    this.ouverte.update((actuelle) => (actuelle === reception.id ? null : reception.id));
  }

  /** Le fichier est téléchargé avec le jeton, puis enregistré par le navigateur. */
  exporter(): void {
    this.service.exporter(this.du(), this.au()).subscribe({
      next: (fichier) => {
        const adresse = URL.createObjectURL(fichier);
        const lien = document.createElement('a');
        lien.href = adresse;
        lien.download = `receptions-${this.du()}-${this.au()}.csv`;
        lien.click();
        URL.revokeObjectURL(adresse);
      },
      error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
    });
  }
}
