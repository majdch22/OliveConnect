import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { COULEURS_STATUT_DEMANDE, Demande, ETAPES_DEMANDE } from '../agriculteur.modeles';
import { AgriculteurService } from '../agriculteur.service';
import { FormulaireDemande } from './formulaire-demande';

/** États « vivants » : affichés dans l'onglet « En cours ». */
const EN_COURS = ['SOUMISE', 'VALIDEE', 'PLANIFIEE', 'EN_COURS'];

/**
 * Page /agriculteur/demandes : les demandes de récolte de l'agriculteur, avec leur avancement.
 * Une demande se modifie ou s'annule tant que la collecte n'a pas démarré.
 */
@Component({
  selector: 'app-demandes-agriculteur',
  imports: [DatePipe, DecimalPipe, RouterLink, ReactiveFormsModule, PanneauLateral, FormulaireDemande],
  templateUrl: './demandes-agriculteur.html',
})
export class DemandesAgriculteur {
  private readonly service = inject(AgriculteurService);
  readonly etapes = ETAPES_DEMANDE;
  readonly couleurs = COULEURS_STATUT_DEMANDE;

  /** Numéro de la demande qui vient d'être envoyée depuis « Déclarer une récolte ». */
  readonly demandeEnvoyee = inject(ActivatedRoute).snapshot.queryParamMap.get('envoyee');

  private readonly rechargement = signal(0);
  readonly demandes = toSignal(toObservable(this.rechargement).pipe(switchMap(() => this.service.demandes())), {
    initialValue: [] as Demande[],
  });

  readonly onglet = signal<'en-cours' | 'historique'>('en-cours');
  readonly demandesAffichees = computed(() =>
    this.demandes().filter((demande) => EN_COURS.includes(demande.statut) === (this.onglet() === 'en-cours')),
  );
  readonly nombreEnCours = computed(() => this.demandes().filter((demande) => EN_COURS.includes(demande.statut)).length);

  /** Rang d'une demande dans la frise (0 à 4), ou -1 si elle est rejetée ou annulée. */
  rangEtape(demande: Demande): number {
    return this.etapes.findIndex((etape) => etape.statut === demande.statut);
  }

  // --- Panneau d'une demande ---

  readonly selection = signal<Demande | null>(null);
  readonly mode = signal<'consultation' | 'modification' | 'annulation'>('consultation');
  readonly motif = new FormControl('', { nonNullable: true, validators: Validators.maxLength(300) });
  readonly messageSucces = signal<string | null>(null);
  readonly messageErreur = signal<string | null>(null);

  ouvrir(demande: Demande): void {
    this.selection.set(demande);
    this.mode.set('consultation');
    this.motif.reset();
    this.messageErreur.set(null);
  }

  apresModification(demande: Demande): void {
    this.selection.set(null);
    this.messageSucces.set(`Demande ${demande.numero} modifiée.`);
    this.rechargement.update((valeur) => valeur + 1);
  }

  confirmerAnnulation(demande: Demande): void {
    this.messageErreur.set(null);
    this.service.annulerDemande(demande.id, this.motif.value.trim() || null).subscribe({
      next: (annulee) => {
        this.selection.set(null);
        this.messageSucces.set(`Demande ${annulee.numero} annulée.`);
        this.rechargement.update((valeur) => valeur + 1);
      },
      error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
    });
  }
}
