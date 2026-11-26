import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { FiltresReferentiel, Variete } from '../administration.modeles';
import { AdministrationService } from '../administration.service';
import { FormulaireVariete } from './formulaire-variete';

/** Colonnes triables de la liste des variétés (noms identiques au tri côté serveur). */
const COLONNES = [
  { tri: 'CODE', libelle: 'Code' },
  { tri: 'NOM', libelle: 'Nom' },
  { tri: 'RENDEMENT', libelle: 'Rendement moyen' },
];

/** Liste des variétés : recherche, archives, tri par colonne, ajout, modification et archivage. */
@Component({
  selector: 'app-liste-varietes',
  imports: [DecimalPipe, PanneauLateral, FormulaireVariete],
  templateUrl: './liste-varietes.html',
})
export class ListeVarietes {
  private readonly administration = inject(AdministrationService);

  readonly colonnes = COLONNES;
  readonly filtres = signal<FiltresReferentiel>({ texte: '', avecArchives: false, tri: 'NOM', sens: 'ASC' });
  private readonly rechargement = signal(0);
  readonly messageErreur = signal<string | null>(null);
  readonly messageSucces = signal<string | null>(null);

  readonly varietes = toSignal(
    toObservable(computed(() => ({ filtres: this.filtres(), rechargement: this.rechargement() }))).pipe(
      debounceTime(200),
      switchMap(({ filtres }) =>
        this.administration.varietes(filtres).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of([] as Variete[]);
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
    const sensParDefaut = tri === 'RENDEMENT' ? 'DESC' : 'ASC';
    this.changerFiltres({ tri, sens: actuels.tri === tri ? (actuels.sens === 'ASC' ? 'DESC' : 'ASC') : sensParDefaut });
  }

  indicateurTri(tri: string): string {
    return this.filtres().tri === tri ? (this.filtres().sens === 'ASC' ? '↑' : '↓') : '';
  }

  // --- Panneau : null = fermé, 'nouvelle' = ajout, sinon la variété modifiée ---

  readonly panneau = signal<Variete | 'nouvelle' | null>(null);
  readonly varieteModifiee = computed(() => {
    const panneau = this.panneau();
    return panneau === 'nouvelle' ? null : panneau;
  });

  apresEnregistrement(variete: Variete): void {
    this.panneau.set(null);
    this.messageSucces.set(`Variété « ${variete.nom} » enregistrée (${variete.code}).`);
    this.rechargement.update((valeur) => valeur + 1);
  }

  changerArchivage(variete: Variete): void {
    this.administration.changerArchivageVariete(variete.id, !variete.archivee).subscribe({
      next: (modifiee) => {
        this.panneau.set(null);
        this.messageSucces.set(`Variété « ${modifiee.nom} » ${modifiee.archivee ? 'archivée' : 'restaurée'}.`);
        this.rechargement.update((valeur) => valeur + 1);
      },
      error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
    });
  }
}
