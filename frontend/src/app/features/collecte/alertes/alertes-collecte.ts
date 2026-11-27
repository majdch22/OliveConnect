import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, combineLatest, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { NotificationsService } from '../../../core/notifications/notifications.service';
import { DepuisPipe } from '../../../shared/pipes/depuis.pipe';
import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';
import { Pagination } from '../../../shared/ui/pagination';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { TYPES_SIGNALEMENT } from '../../agriculteur/agriculteur.modeles';
import { AlerteCollecte, aujourdhui, COULEURS_STATUT_ALERTE, CompteursAlertes, Page, StatutAlerte, TypeAlerte } from '../collecte.modeles';
import { CollecteService } from '../collecte.service';

const ONGLETS: { statut: StatutAlerte | null; libelle: string }[] = [
  { statut: 'OUVERTE', libelle: 'Ouvertes' },
  { statut: 'PRISE_EN_CHARGE', libelle: 'Prises en charge' },
  { statut: 'RESOLUE', libelle: 'Résolues' },
  { statut: null, libelle: 'Toutes' },
];

/**
 * Page /collecte/alertes (maquette RC3) : signalements des agriculteurs, les plus récents d'abord, mis à jour en direct.
 * Fiche : description, photo, distance au verger, maturité ; actions : prendre en charge, marquer résolu
 * (avec la réponse envoyée à l'agriculteur), créer une récolte urgente pour ce verger.
 */
@Component({
  selector: 'app-alertes-collecte',
  imports: [DatePipe, RouterLink, ReactiveFormsModule, DepuisPipe, Pagination, PanneauLateral, TelephonePipe],
  templateUrl: './alertes-collecte.html',
})
export class AlertesCollecte implements OnDestroy {
  private readonly service = inject(CollecteService);
  private readonly notifications = inject(NotificationsService);
  private readonly parametres = inject(ActivatedRoute).snapshot.queryParamMap;

  readonly onglets = ONGLETS;
  readonly types = TYPES_SIGNALEMENT;
  readonly couleurs = COULEURS_STATUT_ALERTE;
  readonly dateMinimum = aujourdhui();

  readonly statut = signal<StatutAlerte | null>('OUVERTE');
  readonly type = signal<TypeAlerte | null>((this.parametres.get('type') as TypeAlerte | null) ?? null);
  readonly page = signal(0);
  private readonly rechargement = signal(0);
  readonly messageErreur = signal<string | null>(null);

  private readonly declencheurs = combineLatest([
    toObservable(this.statut), toObservable(this.type), toObservable(this.page), toObservable(this.rechargement),
    toObservable(this.notifications.derniereRecue),
  ]);

  readonly resultat = toSignal(
    this.declencheurs.pipe(
      switchMap(([statut, type, page]) =>
        this.service.alertes(statut, type, page).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
    ),
    { initialValue: null as Page<AlerteCollecte> | null },
  );
  readonly compteurs = toSignal(
    this.declencheurs.pipe(switchMap(() => this.service.compteursAlertes().pipe(catchError(() => of(null))))),
    { initialValue: null as CompteursAlertes | null },
  );

  readonly selection = signal<AlerteCollecte | null>(null);

  icone(type: TypeAlerte): string {
    return this.types.find((candidat) => candidat.code === type)?.icone ?? '❔';
  }

  changerOnglet(statut: StatutAlerte | null): void {
    this.statut.set(statut);
    this.page.set(0);
  }

  changerType(valeur: string): void {
    this.type.set((valeur || null) as TypeAlerte | null);
    this.page.set(0);
  }

  // --- Fiche ---

  readonly commentaire = new FormControl('', { nonNullable: true, validators: Validators.maxLength(300) });
  readonly formulaireUrgence = signal(false);
  readonly dateUrgence = new FormControl(aujourdhui(), { nonNullable: true, validators: Validators.required });
  readonly quantiteUrgence = new FormControl(2000, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(100000)] });
  readonly erreurFiche = signal<string | null>(null);
  readonly envoiEnCours = signal(false);
  readonly photo = signal<string | null>(null);

  ouvrir(alerte: AlerteCollecte): void {
    this.selection.set(alerte);
    this.commentaire.reset();
    this.formulaireUrgence.set(false);
    this.erreurFiche.set(null);
    this.liberePhoto();
    if (alerte.avecPhoto) {
      this.service.photo(alerte.id).subscribe({ next: (fichier) => this.photo.set(URL.createObjectURL(fichier)), error: () => undefined });
    }
  }

  fermer(): void {
    this.selection.set(null);
    this.liberePhoto();
  }

  prendreEnCharge(alerte: AlerteCollecte): void {
    this.executer(this.service.prendreEnCharge(alerte.id, this.commentaire.value.trim() || null));
  }

  resoudre(alerte: AlerteCollecte): void {
    if (!this.commentaire.value.trim()) {
      this.erreurFiche.set("Écrivez ce qui a été fait : l'agriculteur lira cette réponse.");
      return;
    }
    this.executer(this.service.resoudre(alerte.id, this.commentaire.value.trim()));
  }

  creerDemandeUrgente(alerte: AlerteCollecte): void {
    if (this.dateUrgence.invalid || this.quantiteUrgence.invalid) return;
    this.executer(this.service.creerDemandeUrgente(alerte.id, this.dateUrgence.value, this.quantiteUrgence.value, this.commentaire.value.trim() || null));
  }

  private executer(appel: ReturnType<CollecteService['resoudre']>): void {
    this.envoiEnCours.set(true);
    this.erreurFiche.set(null);
    appel.subscribe({
      next: (alerte) => {
        this.envoiEnCours.set(false);
        this.selection.set(alerte);
        this.commentaire.reset();
        this.formulaireUrgence.set(false);
        this.rechargement.update((fois) => fois + 1);
        this.service.rafraichirAlertesOuvertes();
      },
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreurFiche.set(texteErreur(erreur));
        this.rechargement.update((fois) => fois + 1);
      },
    });
  }

  private liberePhoto(): void {
    const adresse = this.photo();
    if (adresse) URL.revokeObjectURL(adresse);
    this.photo.set(null);
  }

  ngOnDestroy(): void {
    this.liberePhoto();
  }
}
