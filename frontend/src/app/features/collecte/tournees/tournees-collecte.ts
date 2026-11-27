import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, combineLatest, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { NotificationsService } from '../../../core/notifications/notifications.service';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { aujourdhui, COULEURS_STATUT_TOURNEE, ICONES_RESSOURCE, StatutTournee, Tournee, TypeRessource } from '../collecte.modeles';
import { CollecteService } from '../collecte.service';

/**
 * Page /collecte/tournees (maquette RC2) : tournées d'un jour, mises à jour en direct.
 * Navigation jour par jour, filtre par état, fiche d'une tournée (vergers dans l'ordre de passage, ressources,
 * historique) et annulation d'une tournée pas encore démarrée, avec un motif envoyé aux agriculteurs et au chef.
 */
@Component({
  selector: 'app-tournees-collecte',
  imports: [DatePipe, DecimalPipe, RouterLink, ReactiveFormsModule, PanneauLateral],
  templateUrl: './tournees-collecte.html',
})
export class TourneesCollecte {
  private readonly service = inject(CollecteService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationsService);
  private readonly parametres = inject(ActivatedRoute).snapshot.queryParamMap;

  readonly couleurs = COULEURS_STATUT_TOURNEE;
  readonly icones = ICONES_RESSOURCE;
  readonly typesRessource: { type: TypeRessource; libelle: string }[] = [
    { type: 'TRACTEUR', libelle: 'Tracteurs' },
    { type: 'BENNE', libelle: 'Bennes' },
    { type: 'OUVRIER', libelle: 'Ouvriers' },
    { type: 'MATERIEL', libelle: 'Matériel' },
  ];
  readonly aujourdhui = aujourdhui();
  readonly tourneeCreee = this.parametres.get('creee');

  readonly date = signal(this.parametres.get('date') ?? aujourdhui());
  readonly filtreStatut = signal<StatutTournee | ''>('');
  private readonly rechargement = signal(0);
  readonly messageErreur = signal<string | null>(null);

  readonly tournees = toSignal(
    combineLatest([toObservable(this.date), toObservable(this.rechargement), toObservable(this.notifications.derniereRecue)]).pipe(
      switchMap(([date]) =>
        this.service.tournees(date).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of([] as Tournee[] | null);
          }),
        ),
      ),
    ),
    { initialValue: null as Tournee[] | null },
  );

  readonly tourneesAffichees = computed(() =>
    (this.tournees() ?? []).filter((tournee) => !this.filtreStatut() || tournee.statut === this.filtreStatut()),
  );

  constructor() {
    // Ouvre la tournée indiquée dans l'adresse (après sa création) ; garde la fiche ouverte à jour
    const aOuvrir = this.parametres.get('tournee');
    effect(() => {
      const tournees = this.tournees();
      if (!tournees) return;
      const ouverte = this.selection();
      if (ouverte) {
        this.selection.set(tournees.find((tournee) => tournee.id === ouverte.id) ?? ouverte);
      } else if (aOuvrir && !this.ouvertureFaite) {
        this.ouvertureFaite = true;
        this.selection.set(tournees.find((tournee) => tournee.id === aOuvrir) ?? null);
      }
    });
  }
  private ouvertureFaite = false;

  changerJour(decalage: number): void {
    const [annee, mois, jour] = this.date().split('-').map(Number);
    const nouvelle = new Date(annee, mois - 1, jour + decalage);
    this.allerAu(`${nouvelle.getFullYear()}-${String(nouvelle.getMonth() + 1).padStart(2, '0')}-${String(nouvelle.getDate()).padStart(2, '0')}`);
  }

  allerAu(date: string): void {
    if (!date) return;
    this.date.set(date);
    this.router.navigate([], { queryParams: { date }, replaceUrl: true });
  }

  nombreDe(tournee: Tournee, type: TypeRessource): number {
    return tournee.affectations.filter((affectation) => affectation.type === type).length;
  }

  codesDe(tournee: Tournee, type: TypeRessource): string {
    return tournee.affectations.filter((affectation) => affectation.type === type).map((affectation) => affectation.code).join(', ');
  }

  // --- Fiche et annulation ---

  readonly selection = signal<Tournee | null>(null);
  readonly annulationOuverte = signal(false);
  readonly motif = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(300)] });
  readonly erreurFiche = signal<string | null>(null);
  readonly envoiEnCours = signal(false);
  readonly messageSucces = signal<string | null>(null);

  ouvrir(tournee: Tournee): void {
    this.selection.set(tournee);
    this.annulationOuverte.set(false);
    this.motif.reset();
    this.erreurFiche.set(null);
  }

  annuler(tournee: Tournee): void {
    this.motif.markAsTouched();
    if (this.motif.invalid || !this.motif.value.trim()) return;
    this.envoiEnCours.set(true);
    this.service.annulerTournee(tournee.id, this.motif.value.trim()).subscribe({
      next: (annulee) => {
        this.envoiEnCours.set(false);
        this.selection.set(null);
        this.messageSucces.set(`Tournée ${annulee.numero} annulée. Ses demandes sont de nouveau à planifier.`);
        this.rechargement.update((fois) => fois + 1);
      },
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreurFiche.set(texteErreur(erreur));
        this.rechargement.update((fois) => fois + 1);
      },
    });
  }
}
