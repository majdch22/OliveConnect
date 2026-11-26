import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { FiltresReferentiel, OptionListe, Pressoir } from '../administration.modeles';
import { AdministrationService } from '../administration.service';
import { FormulairePressoir } from './formulaire-pressoir';

/** Colonnes triables de la liste des pressoirs (noms identiques au tri côté serveur). */
const COLONNES = [
  { tri: 'CODE', libelle: 'Code' },
  { tri: 'NOM', libelle: 'Nom' },
  { tri: 'DELEGATION', libelle: 'Délégation' },
  { tri: 'CAPACITE', libelle: 'Capacité par jour' },
];

/** Liste des pressoirs : recherche, archives, tri par colonne, ajout, modification et archivage. */
@Component({
  selector: 'app-liste-pressoirs',
  imports: [DecimalPipe, TelephonePipe, PanneauLateral, FormulairePressoir],
  templateUrl: './liste-pressoirs.html',
})
export class ListePressoirs {
  private readonly administration = inject(AdministrationService);

  readonly delegations = input.required<OptionListe[]>();

  readonly colonnes = COLONNES;
  readonly filtres = signal<FiltresReferentiel>({ texte: '', avecArchives: false, tri: 'NOM', sens: 'ASC' });
  private readonly rechargement = signal(0);
  readonly messageErreur = signal<string | null>(null);
  readonly messageSucces = signal<string | null>(null);

  readonly pressoirs = toSignal(
    toObservable(computed(() => ({ filtres: this.filtres(), rechargement: this.rechargement() }))).pipe(
      debounceTime(200),
      switchMap(({ filtres }) =>
        this.administration.pressoirs(filtres).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of([] as Pressoir[]);
          }),
        ),
      ),
    ),
  );

  changerFiltres(changement: Partial<FiltresReferentiel>): void {
    this.filtres.update((actuels) => ({ ...actuels, ...changement }));
  }

  trierPar(tri: string): void {
    const actuels = this.filtres();
    const sensParDefaut = tri === 'CAPACITE' ? 'DESC' : 'ASC';
    this.changerFiltres({ tri, sens: actuels.tri === tri ? (actuels.sens === 'ASC' ? 'DESC' : 'ASC') : sensParDefaut });
  }

  indicateurTri(tri: string): string {
    return this.filtres().tri === tri ? (this.filtres().sens === 'ASC' ? '↑' : '↓') : '';
  }

  // --- Panneau : null = fermé, 'nouveau' = ajout, sinon le pressoir modifié ---

  readonly panneau = signal<Pressoir | 'nouveau' | null>(null);
  readonly pressoirModifie = computed(() => {
    const panneau = this.panneau();
    return panneau === 'nouveau' ? null : panneau;
  });

  apresEnregistrement(pressoir: Pressoir): void {
    this.panneau.set(null);
    this.messageSucces.set(`Pressoir « ${pressoir.nom} » enregistré (${pressoir.code}).`);
    this.rechargement.update((valeur) => valeur + 1);
  }

  changerArchivage(pressoir: Pressoir): void {
    this.messageErreur.set(null);
    this.administration.changerArchivagePressoir(pressoir.id, !pressoir.archive).subscribe({
      next: (modifie) => {
        this.panneau.set(null);
        this.messageSucces.set(`Pressoir « ${modifie.nom} » ${modifie.archive ? 'archivé' : 'restauré'}.`);
        this.rechargement.update((valeur) => valeur + 1);
      },
      error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
    });
  }
}
