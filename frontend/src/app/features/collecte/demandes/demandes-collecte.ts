import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';
import { Pagination } from '../../../shared/ui/pagination';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { COULEURS_STATUT_DEMANDE } from '../../agriculteur/agriculteur.modeles';
import { CompteursDemandes, DemandeCollecte, FicheDemande, MOTIFS_REJET, Page, StatutDemande, TypeRecolte } from '../collecte.modeles';
import { CollecteService } from '../collecte.service';

/** Onglets de la file : chaque état, puis toutes les demandes. */
const ONGLETS: { statut: StatutDemande | null; libelle: string }[] = [
  { statut: 'SOUMISE', libelle: 'À examiner' },
  { statut: 'VALIDEE', libelle: 'Validées' },
  { statut: 'PLANIFIEE', libelle: 'Planifiées' },
  { statut: 'EN_COURS', libelle: 'En cours' },
  { statut: 'TERMINEE', libelle: 'Terminées' },
  { statut: 'REJETEE', libelle: 'Rejetées' },
  { statut: 'ANNULEE', libelle: 'Annulées' },
  { statut: null, libelle: 'Toutes' },
];

/**
 * Page /collecte/demandes (maquette RC1) : file des demandes de récolte, urgentes puis plus mûres d'abord.
 * La fiche d'une demande s'ouvre dans un panneau : verger, agriculteur, maturité, pressoir le plus proche, historique,
 * et les décisions : valider, valider et planifier directement, rejeter avec un motif (motifs proposés en un clic).
 */
@Component({
  selector: 'app-demandes-collecte',
  imports: [DatePipe, DecimalPipe, RouterLink, ReactiveFormsModule, Pagination, PanneauLateral, TelephonePipe],
  templateUrl: './demandes-collecte.html',
})
export class DemandesCollecte {
  private readonly service = inject(CollecteService);
  private readonly router = inject(Router);
  private readonly parametres = inject(ActivatedRoute).snapshot.queryParamMap;

  readonly onglets = ONGLETS;
  readonly couleurs = COULEURS_STATUT_DEMANDE;
  readonly motifsRapides = MOTIFS_REJET;

  // --- Filtres (relus depuis l'adresse : les tuiles de l'accueil ouvrent le bon onglet) ---

  readonly statut = signal<StatutDemande | null>((this.parametres.get('statut') as StatutDemande | null) ?? 'SOUMISE');
  readonly type = signal<TypeRecolte | null>(null);
  readonly texte = signal('');
  readonly page = signal(0);
  readonly taille = signal(10);
  private readonly rechargement = signal(0);

  private readonly filtres = computed(() => ({
    statut: this.statut(), type: this.type(), texte: this.texte(), page: this.page(), taille: this.taille(), fois: this.rechargement(),
  }));

  readonly messageErreur = signal<string | null>(null);
  readonly resultat = toSignal(
    toObservable(this.filtres).pipe(
      debounceTime(200),
      switchMap((filtres) =>
        this.service.demandes(filtres).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
    ),
    { initialValue: null as Page<DemandeCollecte> | null },
  );
  readonly compteurs = toSignal(
    toObservable(this.rechargement).pipe(switchMap(() => this.service.compteursDemandes().pipe(catchError(() => of(null))))),
    { initialValue: null as CompteursDemandes | null },
  );

  changerOnglet(statut: StatutDemande | null): void {
    this.statut.set(statut);
    this.page.set(0);
  }

  changerType(valeur: string): void {
    this.type.set((valeur || null) as TypeRecolte | null);
    this.page.set(0);
  }

  chercher(valeur: string): void {
    this.texte.set(valeur);
    this.page.set(0);
  }

  changerTaille(taille: number): void {
    this.taille.set(taille);
    this.page.set(0);
  }

  nombre(statut: StatutDemande | null): number | null {
    const compteurs = this.compteurs();
    return statut && compteurs ? compteurs[statut] : null;
  }

  // --- Fiche d'une demande ---

  readonly fiche = signal<FicheDemande | null>(null);
  readonly mode = signal<'consultation' | 'rejet'>('consultation');
  readonly commentaire = new FormControl('', { nonNullable: true, validators: Validators.maxLength(300) });
  readonly motif = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(300)] });
  readonly envoiEnCours = signal(false);
  readonly erreurFiche = signal<string | null>(null);
  readonly messageSucces = signal<string | null>(null);

  ouvrir(demande: DemandeCollecte): void {
    this.mode.set('consultation');
    this.commentaire.reset();
    this.motif.reset();
    this.erreurFiche.set(null);
    this.service.ficheDemande(demande.id).subscribe({
      next: (fiche) => this.fiche.set(fiche),
      error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
    });
  }

  fermer(): void {
    this.fiche.set(null);
  }

  /** Valide la demande ; « et planifier » ouvre ensuite la planification avec cette demande déjà cochée. */
  valider(demande: DemandeCollecte, puisPlanifier: boolean): void {
    this.envoiEnCours.set(true);
    this.erreurFiche.set(null);
    this.service.validerDemande(demande.id, this.commentaire.value.trim() || null).subscribe({
      next: (validee) => {
        this.envoiEnCours.set(false);
        this.fiche.set(null);
        if (puisPlanifier) {
          this.router.navigate(['/collecte/planifier'], { queryParams: { demandes: validee.id, date: validee.dateSouhaitee } });
          return;
        }
        this.messageSucces.set(`Demande ${validee.numero} validée. L'agriculteur est prévenu.`);
        this.rechargement.update((fois) => fois + 1);
      },
      error: (erreur: HttpErrorResponse) => this.echec(erreur),
    });
  }

  choisirMotif(motif: string): void {
    this.motif.setValue(motif);
  }

  rejeter(demande: DemandeCollecte): void {
    this.motif.markAsTouched();
    if (this.motif.invalid || !this.motif.value.trim()) {
      return;
    }
    this.envoiEnCours.set(true);
    this.erreurFiche.set(null);
    this.service.rejeterDemande(demande.id, this.motif.value.trim()).subscribe({
      next: (rejetee) => {
        this.envoiEnCours.set(false);
        this.fiche.set(null);
        this.messageSucces.set(`Demande ${rejetee.numero} rejetée. L'agriculteur a reçu le motif.`);
        this.rechargement.update((fois) => fois + 1);
      },
      error: (erreur: HttpErrorResponse) => this.echec(erreur),
    });
  }

  private echec(erreur: HttpErrorResponse): void {
    this.envoiEnCours.set(false);
    this.erreurFiche.set(texteErreur(erreur));
    // La demande a pu être traitée par un autre responsable : on relit la liste
    this.rechargement.update((fois) => fois + 1);
  }
}
