import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { texteErreur } from '../../../core/erreurs/message-erreur';
import { BadgeStatut } from '../../../shared/ui/badge-statut';
import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';
import { CompteAdministration } from '../administration.modeles';
import { AdministrationService } from '../administration.service';
import { FormulaireCompte } from './formulaire-compte';
import { MotDePasseProvisoire } from './mot-de-passe-provisoire';

/** Action sensible qui demande une confirmation avant d'être lancée. */
type ActionAConfirmer = 'desactiver' | 'reinitialiser';

/**
 * Contenu du panneau d'un compte : informations, modification, désactivation / réactivation
 * et réinitialisation du mot de passe. Prévient la liste à chaque changement pour qu'elle se recharge.
 */
@Component({
  selector: 'app-fiche-compte',
  imports: [DatePipe, BadgeStatut, FormulaireCompte, MotDePasseProvisoire, TelephonePipe],
  templateUrl: './fiche-compte.html',
})
export class FicheCompte {
  private readonly administration = inject(AdministrationService);
  private readonly session = inject(AuthentificationService).session;

  readonly compte = input.required<CompteAdministration>();
  readonly change = output<CompteAdministration>();

  readonly enModification = signal(false);
  readonly actionAConfirmer = signal<ActionAConfirmer | null>(null);
  readonly actionEnCours = signal(false);
  readonly motDePasseProvisoire = signal<string | null>(null);
  readonly messageSucces = signal<string | null>(null);
  readonly messageErreur = signal<string | null>(null);

  /** Nom du pressoir d'un responsable du pressoir (archivés compris, pour l'historique). */
  private readonly pressoirs = toSignal(
    this.administration.pressoirs({ texte: '', avecArchives: true, tri: 'NOM', sens: 'ASC' }),
    { initialValue: [] },
  );
  readonly nomPressoir = computed(() => {
    const pressoir = this.pressoirs().find((candidat) => candidat.id === this.compte().pressoirId);
    return pressoir ? `${pressoir.nom} (${pressoir.code})` : 'Aucun pressoir rattaché';
  });

  /** L'administrateur ne peut ni se désactiver ni changer son propre rôle. */
  readonly estMonCompte = computed(() => this.compte().id === this.session()?.id);

  apresModification(compte: CompteAdministration): void {
    this.enModification.set(false);
    this.messageSucces.set('Modifications enregistrées.');
    this.change.emit(compte);
  }

  desactiver(): void {
    this.executer(this.administration.desactiver(this.compte().id), 'Compte désactivé : son accès est coupé immédiatement.');
  }

  reactiver(): void {
    this.executer(this.administration.reactiver(this.compte().id), 'Compte réactivé.');
  }

  reinitialiserMotDePasse(): void {
    this.preparerAction();
    this.administration.reinitialiserMotDePasse(this.compte().id).subscribe({
      next: (resultat) => {
        this.actionEnCours.set(false);
        this.motDePasseProvisoire.set(resultat.motDePasseProvisoire);
        this.change.emit(resultat.compte);
      },
      error: (erreur: HttpErrorResponse) => this.echec(erreur),
    });
  }

  private executer(action: Observable<CompteAdministration>, reussite: string): void {
    this.preparerAction();
    action.subscribe({
      next: (compte) => {
        this.actionEnCours.set(false);
        this.messageSucces.set(reussite);
        this.change.emit(compte);
      },
      error: (erreur: HttpErrorResponse) => this.echec(erreur),
    });
  }

  private preparerAction(): void {
    this.actionAConfirmer.set(null);
    this.actionEnCours.set(true);
    this.messageSucces.set(null);
    this.messageErreur.set(null);
  }

  private echec(erreur: HttpErrorResponse): void {
    this.actionEnCours.set(false);
    this.messageErreur.set(texteErreur(erreur));
  }
}
