import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, of, switchMap, tap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { BadgeStatut } from '../../../shared/ui/badge-statut';
import { Pagination } from '../../../shared/ui/pagination';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';
import { CompteAdministration, EtatInscription, FiltresInscriptions, Sens, TriComptes } from '../administration.modeles';
import { AdministrationService } from '../administration.service';

/** Ordres de tri proposés dans la liste déroulante « Trier par ». */
const ORDRES: { code: string; libelle: string; tri: TriComptes; sens: Sens }[] = [
  { code: 'recentes', libelle: 'Plus récentes', tri: 'DATE_CREATION', sens: 'DESC' },
  { code: 'anciennes', libelle: 'Plus anciennes', tri: 'DATE_CREATION', sens: 'ASC' },
  { code: 'nom-croissant', libelle: 'Nom (A → Z)', tri: 'NOM', sens: 'ASC' },
  { code: 'nom-decroissant', libelle: 'Nom (Z → A)', tri: 'NOM', sens: 'DESC' },
];

const FILTRES_INITIAUX: FiltresInscriptions = {
  etat: 'EN_ATTENTE',
  texte: '',
  delegation: '',
  tri: 'DATE_CREATION',
  sens: 'DESC',
  page: 0,
  taille: 10,
};

/**
 * Page /administration/inscriptions : l'administrateur accepte ou refuse les inscriptions des agriculteurs.
 * Onglets par état, recherche, filtre par délégation, tri et pagination (exécutés par le serveur).
 */
@Component({
  selector: 'app-inscriptions-administration',
  imports: [DatePipe, ReactiveFormsModule, BadgeStatut, Pagination, PanneauLateral, TelephonePipe],
  templateUrl: './inscriptions-administration.html',
})
export class InscriptionsAdministration {
  readonly administration = inject(AdministrationService);

  readonly onglets: { etat: EtatInscription; libelle: string }[] = [
    { etat: 'EN_ATTENTE', libelle: 'En attente' },
    { etat: 'ACCEPTEE', libelle: 'Acceptées' },
    { etat: 'REFUSEE', libelle: 'Refusées' },
  ];
  readonly ordres = ORDRES;
  readonly motifsRapides = [
    'Informations incomplètes.',
    'Délégation hors de la zone de collecte de la coopérative.',
    "Carte d'identité invalide ou illisible.",
    'Inscription en double.',
  ];
  readonly delegations = toSignal(this.administration.delegations(), { initialValue: [] });

  // --- Liste ---

  readonly filtres = signal<FiltresInscriptions>(FILTRES_INITIAUX);
  private readonly rechargement = signal(0);
  readonly chargement = signal(true);

  /** Recharge la liste à chaque changement de filtre (avec un court délai pendant la frappe). */
  readonly resultat = toSignal(
    toObservable(computed(() => ({ filtres: this.filtres(), rechargement: this.rechargement() }))).pipe(
      debounceTime(250),
      tap(() => this.chargement.set(true)),
      switchMap(({ filtres }) =>
        this.administration.listerInscriptions(filtres).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
      tap(() => this.chargement.set(false)),
    ),
  );

  readonly ordreChoisi = computed(
    () => ORDRES.find((ordre) => ordre.tri === this.filtres().tri && ordre.sens === this.filtres().sens)?.code ?? 'recentes',
  );
  readonly filtresActifs = computed(() => this.filtres().texte !== '' || this.filtres().delegation !== '');

  /** Change un ou plusieurs filtres ; on revient à la première page sauf si la page elle-même change. */
  changerFiltres(changement: Partial<FiltresInscriptions>): void {
    this.filtres.update((actuels) => ({ ...actuels, page: 0, ...changement }));
  }

  changerOrdre(code: string): void {
    const ordre = ORDRES.find((possible) => possible.code === code) ?? ORDRES[0];
    this.changerFiltres({ tri: ordre.tri, sens: ordre.sens });
  }

  effacerFiltres(): void {
    this.changerFiltres({ texte: '', delegation: '' });
  }

  // --- Fiche et actions ---

  readonly selection = signal<CompteAdministration | null>(null);
  readonly refusEnCours = signal(false);
  readonly actionEnCours = signal(false);
  readonly messageSucces = signal<string | null>(null);
  readonly messageErreur = signal<string | null>(null);
  readonly motif = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(300)] });

  ouvrir(inscription: CompteAdministration): void {
    this.selection.set(inscription);
    this.refusEnCours.set(false);
    this.motif.reset();
    this.messageErreur.set(null);
  }

  fermer(): void {
    this.selection.set(null);
  }

  accepter(inscription: CompteAdministration): void {
    this.executer(this.administration.accepterInscription(inscription.id), (compte) =>
      `Inscription de ${compte.prenom} ${compte.nom} acceptée. Numéro d'adhérent : ${compte.numeroAdherent}.`,
    );
  }

  confirmerRefus(inscription: CompteAdministration): void {
    if (this.motif.invalid || this.motif.value.trim() === '') {
      this.motif.markAsTouched();
      return;
    }
    this.executer(this.administration.refuserInscription(inscription.id, this.motif.value.trim()), (compte) =>
      `Inscription de ${compte.prenom} ${compte.nom} refusée.`,
    );
  }

  /** Lance une action, puis ferme la fiche, affiche le message de réussite et recharge la liste. */
  private executer(
    action: ReturnType<AdministrationService['accepterInscription']>,
    messageReussite: (compte: CompteAdministration) => string,
  ): void {
    this.actionEnCours.set(true);
    this.messageErreur.set(null);
    action.subscribe({
      next: (compte) => {
        this.actionEnCours.set(false);
        this.messageSucces.set(messageReussite(compte));
        this.selection.set(null);
        this.rechargement.update((valeur) => valeur + 1);
      },
      error: (erreur: HttpErrorResponse) => {
        this.actionEnCours.set(false);
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }
}
